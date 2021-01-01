package com.republicate.modality.impl;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.republicate.modality.Attribute;
import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.ModelRepository;
import com.republicate.modality.WrappingInstance;
import com.republicate.modality.config.ConfigDigester;
import com.republicate.modality.config.ConfigHelper;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.config.Constants;
import com.republicate.modality.filter.IdentifiersFilters;
import com.republicate.modality.filter.ValueFilters;
import com.republicate.modality.sql.BasicDataSource;
import com.republicate.modality.sql.ConnectionPool;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.Credentials;
import com.republicate.modality.sql.DriverInfos;
import com.republicate.modality.sql.PooledDataSource;
import com.republicate.modality.sql.StatementPool;
import com.republicate.modality.util.ConversionHandler;
import com.republicate.modality.util.ConversionHandlerImpl;
import com.republicate.modality.util.Cryptograph;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.ClassUtils;
import org.apache.velocity.tools.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;

// TODO - Velocity-aware model should be a subclass
// import org.apache.velocity.app.VelocityEngine;

public abstract class BaseModel extends AttributeHolder implements Constants
{
    public BaseModel()
    {
        this("model");
    }

    public BaseModel(String modelId)
    {
        super(modelId);
        // CB TODO - add suffixes to the default id when initializing several models without id
        setModelId(Optional.ofNullable(modelId).orElse(DEFAULT_MODEL_ID));
    }

    private Logger logger = LoggerFactory.getLogger("modality");

    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @Since Modality 1.1
     */
    public Model setLoggerName(String loggerName)
    {
        this.logger = LoggerFactory.getLogger(loggerName);
        return getModel();
    }

    /*
     * Configuration
     */

    public Model configure(Map params)
    {
        ensureConfigured(params.get("servletContext"));
        configure(new ConfigHelper(params));
        return getModel();
    }

    public boolean isConfigured()
    {
        return configured;
    }

    private void ensureConfigured()
    {
        ensureConfigured(null);
    }

    private void ensureConfigured(Object servletContext)
    {
        if (!configured && !configuring)
        {
            synchronized (this)
            {
                configuring = true;
                ConfigHelper config = new ConfigHelper();
                this.servletContext = servletContext;
                loadDefaultConfig(config);
                loadGlobalConfig(config, servletContext);
                loadModelConfig(config, servletContext);
                configure(config);
                configuring = false;
                configured = true;
            }
        }
    }

    private void loadDefaultConfig(ConfigHelper config)
    {
        URL url = ClassUtils.getResource(MODALITY_DEFAULTS_PATH, ConfigHelper.class);
        config.setProperties(url);
    }

    private void loadGlobalConfig(ConfigHelper config, Object servletContext)
    {
        URL url = config.findURL(MODALITY_PROPERTIES, servletContext, false);
        if (url != null)
        {
            config.setProperties(url);
        }
    }

    private void loadModelConfig(ConfigHelper config, Object servletContext)
    {
        String modelConfig = config.getString(MODEL_CONFIGURATION);
        if (modelConfig == null)
        {
            String modelId = getModelId();
            if (modelId != null)
            {
                modelConfig = modelId + ".properties";
            }
        }
        if (modelConfig != null)
        {
            URL url = config.findURL(modelConfig, servletContext, false);
            if (url != null)
            {
                config.setProperties(url);
            }
        }
    }

    private Model configure(ConfigHelper config)
    {
        try
        {
            config.setPrefix("model.");
            Optional.ofNullable(config.getString(MODEL_LOGGER_NAME)).ifPresent(this::setLoggerName);
            setWriteAccess(config.getEnum(MODEL_WRITE_ACCESS, getWriteAccess()));
            setReverseMode(config.getEnum(MODEL_REVERSE_MODE, getReverseMode()));
            // TODO - Velocity-aware model should be a subclass
            // Optional.ofNullable((VelocityEngine)config.get(MODEL_VELOCITY_ENGINE)).ifPresent(this::setVelocityEngine);
            Optional.ofNullable(config.getString(MODEL_SCHEMA)).ifPresent(this::setSchema);
            Optional.ofNullable(config.getString(MODEL_IDENTIFIERS_INFLECTOR))
                .ifPresent(getIdentifiersFilters()::setInflector);
            Optional.ofNullable(config.get(MODEL_IDENTIFIERS_MAPPING))
                .ifPresent(getIdentifiersFilters()::addMappings);
            Optional.ofNullable(config.getSubProperties(MODEL_IDENTIFIERS_MAPPING))
                .ifPresent(getIdentifiersFilters()::addMappings);
            Optional.ofNullable(config.getSubProperties(MODEL_FILTERS_READ))
                .ifPresent(getFilters()::setReadMapping);
            Optional.ofNullable(config.getSubProperties(MODEL_FILTERS_WRITE))
                .ifPresent(getFilters()::setWriteMapping);
            Object dataSource = config.get(MODEL_DATASOURCE);
            if (dataSource != null)
            {
                try
                {
                    if (dataSource instanceof String)
                    {
                        setDataSource((String)dataSource);
                    }
                    else if (dataSource instanceof DataSource)
                    {
                        setDataSource((DataSource)dataSource);
                    }
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("could not set model datasource", e);
                }
            }

            Optional.ofNullable(config.getString(MODEL_FILTERS_CRYPTOGRAPH)).ifPresent(getFilters()::setCryptographClass);

            Optional.ofNullable(config.get(MODEL_INSTANCES_FACTORY)).ifPresent(getInstances()::setFactory);
            Optional.ofNullable(config.getSubProperties(MODEL_INSTANCES_CLASSES)).ifPresent(getInstances()::setClasses);

            Optional.ofNullable(config.getString(MODEL_DATABASE)).ifPresent(this::setDatabaseURL);
            Optional.ofNullable(config.getString(MODEL_CREDENTIALS_USER)).ifPresent(getCredentials()::setUser);
            Optional.ofNullable(config.getString(MODEL_CREDENTIALS_PASSWORD)).ifPresent(getCredentials()::setPassword);

            String path = config.getString(MODEL_DEFINITION);
            boolean useDefault = false;
            if (path == null)
            {
                URL definition = getDefinition();
                if (definition == null)
                {
                    useDefault = true;
                    path = getModelId() + ".xml";
                }
            }
            if (path != null)
            {
                try
                {
                    setDefinition(config.findURL(path));
                }
                catch (ConfigurationException ce)
                {
                    if (!useDefault)
                    {
                        throw ce;
                    }
                }
            }
            return getModel();
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("configuration problem", e);
        }
        finally
        {
            config.resetPrefix();
        }
    }

    public NavigableMap<String, Attribute> getConfiguration()
    {
        return new TreeMap(); // TODO
    }


    /*
     * Initialization
     */
    public Model initialize()
    {
        ensureConfigured();
        return initialize(getDefinition());
    }

    public Model initialize(URL url)
    {
        try
        {
            if (url == null)
            {
                initialize((Reader)null);
            }
            else
            {
                setDefinition(url);
                Reader reader = new InputStreamReader(url.openStream());
                InputSource source = new InputSource(reader);
                source.setSystemId(url.toExternalForm());
                initialize(source);
            }
        }
        catch (IOException ioe)
        {
            throw new ConfigurationException("could not initialize model", ioe);
        }
        return getModel();
    }

    public Model initialize(String path)
    {
        return initialize(new ConfigHelper().findURL(path));
    }

    public Model initialize(Reader reader) throws ConfigurationException
    {
        return initialize(reader == null ? null : new InputSource(reader));

    }

    public Model initialize(InputSource source) throws ConfigurationException
    {
        try
        {
            ensureConfigured();
            readDefinition(source);
            connect();
            getIdentifiersFilters().initialize();
            getFilters().initialize();
            reverseEngineer();
            getInstances().initialize();
            initializeAttributes(); // root attributes initialization
            registerModel();
            initialized = true;
        }
        catch (ConfigurationException ce)
        {
            throw initializationError = ce;
        }
        catch (Exception e)
        {
            throw initializationError = new ConfigurationException("could not initialize model " + getModelId(), e);
        }
        return getModel();
    }

    protected void initializeEntities()
    {
        for (Entity entity : getEntities().values())
        {
            entity.initialize();
        }
    }

    protected void readDefinition(InputSource source) throws Exception
    {
        if (source == null)
        {
            return;
        }
        DocumentBuilderFactory builderFactory = XmlUtils.createDocumentBuilderFactory();
        builderFactory.setXIncludeAware(true);
        Element doc = builderFactory.newDocumentBuilder().parse(source).getDocumentElement();
        String rootTag = doc.getTagName();

        if (!"model".equals(rootTag))
        {
            throw new ConfigurationException("expecting a <model> root tag");
        }
        new ConfigDigester(doc, this).process();
    }

    protected final void connect() throws Exception
    {
        if (dataSource == null)
        {
            if (databaseURL == null)
            {
                throw new ConfigurationException("cannot connect: no data source");
            }
            else
            {
                setDataSource(new BasicDataSource(databaseURL));
            }
        }
        // override driver properties deduced from database metadata
        // with properties provided by the user
        Connection connection = getCredentials().getConnection(dataSource);
        Properties props = ReverseEngineer.getStockDriverProperties(connection.getMetaData().getURL());
        DriverInfos stockInfos = new DriverInfos();
        ConfigDigester.setProperties(this, props);
        getDriverInfos().setDefaults(stockInfos);
        getDriverInfos().log();

        connectionPool = new ConnectionPool(dataSource, credentials, driverInfos, schema, true, maxConnections);
        transactionConnectionPool = new ConnectionPool(dataSource, credentials, driverInfos, schema, false, maxConnections);
        statementPool = new StatementPool(getModelId(), connectionPool, getConnectionsCheckInterval());
    }

    protected final void registerModel()
    {
        ModelRepository.registerModel(servletContext, getModel());
    }

    protected void checkInitialized()
    {
        if (!initialized)
        {
            throw Optional.ofNullable(initializationError).orElse(new ConfigurationException("model hasn't been initialized"));
        }
    }

    /*
     * Getters and setters
     */

    public String getModelId()
    {
        return modelId;
    }

    private void setModelId(String modelId)
    {
        this.modelId = modelId;
    }

    public WriteAccess getWriteAccess()
    {
        return writeAccess;
    }

    public Model setWriteAccess(WriteAccess writeAccess)
    {
        ensureConfigured();
        this.writeAccess = writeAccess;
        return getModel();
    }

    public ReverseMode getReverseMode()
    {
        return reverseMode;
    }

    public Model setReverseMode(ReverseMode reverseMode)
    {
        ensureConfigured();
        this.reverseMode = reverseMode;
        return getModel();
    }

    public Model setReverseMode(String reverseMode)
    {
        ensureConfigured();
        try
        {
            this.reverseMode = ReverseMode.valueOf(ReverseMode.class, reverseMode.toUpperCase(Locale.ROOT));
        }
        catch (NullPointerException | IllegalArgumentException e)
        {
            String msg = "provided constant is not a valid reverse mode: "+ reverseMode;
            getLogger().error(msg, e);
            throw new ConfigurationException(msg, e);
        }
        return getModel();
    }

    // TODO - Velocity-aware model should be a subclass
    /*
    public VelocityEngine getVelocityEngine()
    {
        return velocityEngine;
    }

    public Model setVelocityEngine(VelocityEngine velocityEngine)
    {
        this.velocityEngine = velocityEngine;
        return get();
    }
     */

    public Model setDataSource(String dataSourceName) throws Exception
    {
        Object resource;
        Context ctx;
        try
        {
            ctx = InitialContext.doLookup("java:comp/env");
            resource = ctx.lookup(dataSourceName);
            if (resource == null)
            {
                throw new RuntimeException("Data source not found: " + dataSourceName);
            }
        }
        catch (NameNotFoundException nnfe)
        {
            throw new RuntimeException("Data source not found: " + dataSourceName, nnfe);
        }
        DataSource dataSource = null;
        if (resource instanceof DataSource)
        {
            dataSource = (DataSource)resource;
        }
        else if (resource instanceof ConnectionPoolDataSource)
        {
            dataSource = new PooledDataSource((ConnectionPoolDataSource)resource);
        }
        else
        {
            throw new RuntimeException("Data source resource type not handled: " + dataSource.getClass().getName());
        }
        return setDataSource(dataSource);
    }

    public Model setDataSource(DataSource dataSource) throws Exception
    {
        ensureConfigured();
        if (this.dataSource != null)
        {
            throw new ConfigurationException("data source cannot be changed (no dynamic reloading)");
        }
        this.dataSource = dataSource;
        return getModel();
    }

    public String getDatabaseURL()
    {
        return databaseURL;
    }

    public Model setDatabaseURL(String databaseURL)
    {
        ensureConfigured();
        this.databaseURL = databaseURL;
        return getModel();
    }

    public String getSchema()
    {
        return schema;
    }

    public Model setSchema(String schema)
    {
        ensureConfigured();
        this.schema = schema;
        if (connectionPool != null)
        {
            connectionPool.setSchema(schema);
        }
        return getModel();
    }

    public long getConnectionsCheckInterval()
    {
        return connectionsCheckInterval;
    }

    public Model setConnectionsCheckInterval(long connectionsCheckInterval)
    {
        this.connectionsCheckInterval = connectionsCheckInterval;
        return getModel();
    }

    public URL getDefinition()
    {
        return definition;
    }

    public Model setDefinition(String path) throws MalformedURLException
    {
        if (path != null && path.contains("://"))
        {
            setDefinition(new URL(path));
        }
        return getModel();
    }

    public Model setDefinition(URL definition)
    {
        ensureConfigured();
        this.definition = definition;
        return getModel();
    }

    public Credentials getCredentials()
    {
        return credentials;
    }

    public IdentifiersFilters getIdentifiersFilters()
    {
        return identifiers;
    }

    public FiltersSet getFilters()
    {
        return filters;
    }

    public Entity getEntity(String name)
    {
        return entitiesMap.get(name);
    }

    public DriverInfos getDriverInfos()
    {
        return driverInfos;
    }

    protected ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    protected StatementPool getStatementPool()
    {
        return statementPool;
    }

    protected ConnectionWrapper getTransactionConnection() throws SQLException
    {
        return transactionConnectionPool.getConnection();
    }

    public NavigableMap<String, Entity> getEntities()
    {
        return Collections.unmodifiableNavigableMap(entitiesMap);
    }

    protected UserInstancesConfig getInstances()
    {
        return userInstancesConfig;
    }

    public ConversionHandler getConversionHandler()
    {
        return conversionHandler;
    }

    protected Object getServletContext()
    {
        return servletContext; // null in a standalone context
    }

    /*
     * Definition
     */

    public void addEntity(Entity entity)
    {
        entitiesMap.put(entity.getName(), entity);
    }

    private ReverseEngineer getMetaModel(ConnectionWrapper connection) throws SQLException
    {
        return new ReverseEngineer(connection.getMetaData(), driverInfos);
    }

    public ReverseEngineer getMetaModel() throws SQLException
    {
        ConnectionWrapper connection = connectionPool.getConnection();
        return getMetaModel(connection);
    }

    private void reverseEngineer() throws SQLException
    {
        if (connectionPool == null)
        {
            getLogger().warn("connection pool not available: not performing reverse enginering");
            return;
        }
        ConnectionWrapper connection = null;
        try
        {
            connection = connectionPool.getConnection();
            connection.enterBusyState();
            ReverseEngineer reverseEngineer = getMetaModel(connection);

            // we need to temporarily remember whether explicit table names where provided for explicit entities
            Set<String> entitiesWithExplicitTables = new HashSet<>();

            for (Entity entity : entitiesMap.values())
            {
                String table = entity.getTable();
                if (table == null) table = entity.getName();
                else entitiesWithExplicitTables.add(entity.getName());
                // adapt known entities table case if necessary
                table = driverInfos.getTableName(table);
                entity.setTable(table);
            }


            // build a temporary map of declared entities per tables
            Map<String, Entity> knownEntitiesByTable = null;
            if (getReverseMode().reverseColumns())
            {
                // build a temporary map of declared entities per tables
                knownEntitiesByTable = new TreeMap<>();
                for (Entity entity : entitiesMap.values())
                {
                    Entity prev = knownEntitiesByTable.put(entity.getTable(), entity);
                    if (prev != null)
                    {
                        throw new ConfigurationException("entity table name collision: entities " + entity.getName() + " and " + prev.getName() + " both reference table " + entity.getTable());
                    }
                }
            }

            // reverse enginering of tables if asked so
            if (getReverseMode().reverseTables())
            {
                List<String> sqlTables = reverseEngineer.getTables();
                for (String table : sqlTables)
                {
                    Entity entity = knownEntitiesByTable.get(table);
                    if (entity == null)
                    {
                        String entityName = getIdentifiersFilters().transformTableName(table);
                        entity = getEntity(entityName);
                        if (entity != null)
                        {
                            if (entity.getTable() != null)
                            {
                                if (entitiesWithExplicitTables.contains(entityName))
                                {
                                    throw new ConfigurationException("entity table name collision: entity " + entity.getName() + " maps both tables " + entity.getTable() + " and " + table);
                                }
                                else
                                {
                                    // it means the wild guess we made for the table name was wrong
                                    knownEntitiesByTable.remove(entity.getTable());
                                }
                            }
                            getLogger().warn("binding entity {} to table {}", entity.getName(), table);
                            entity.setTable(table);
                            knownEntitiesByTable.put(table, entity);
                        }
                        else
                        {
                            entity = new Entity(entityName, getModel());
                            entity.setTable(table);
                            addEntity(entity);
                            knownEntitiesByTable.put(table, entity);
                        }
                    }
                }
            }

            if (getReverseMode().reverseColumns())
            {
                // reverse enginering of columns and primary key
                for (Entity entity : knownEntitiesByTable.values())
                {
                    List<Entity.Column> columns = reverseEngineer.getColumns(entity);
                    if (columns.size() == 0)
                    {
                        // it means the sql table does not exist
                        if (entitiesWithExplicitTables.contains(entity.getName()))
                        {
                            throw new ConfigurationException("sql table '" + entity.getTable() + "' not found for entity " + entity.getName());
                        } else
                        {
                            entity.setTable(null);
                        }
                    } else
                    {
                        for (Entity.Column column : columns)
                        {
                            entity.addColumn(column);
                        }
                        // do not overwrite primary key if it has been set by config (for views, per instance)
                        if (entity.getSqlPrimaryKey() == null)
                        {
                            entity.setSqlPrimaryKey(reverseEngineer.getPrimaryKey(entity));
                        }
                    }
                }
            }

            // initialization point for entities
            initializeEntities();

            // reverse enginering of joins, if asked so
            if (getReverseMode().reverseJoins())
            {
                Map<Entity, List<Pair<Entity, List<String>>>> potentialJoinTables = new HashMap<>();
                for (Entity pkEntity : knownEntitiesByTable.values())
                {
                    List<Pair<String, List<String>>> joins = reverseEngineer.getJoins(pkEntity);
                    for (Pair<String, List<String>> join : joins)
                    {
                        String fkTable = join.getLeft();
                        List<String> fkColumns = join.getRight();
                        Entity fkEntity = knownEntitiesByTable.get(fkTable);
                        if (fkEntity != null)
                        {
                            // define upstream attribute from fk to pk
                            declareUpstreamJoin(pkEntity, fkEntity, fkColumns);

                            // define downstream attribute from pk to fk
                            declareJoinTowardsForeignKey(pkEntity, fkEntity, fkColumns);

                            List<Pair<Entity, List<String>>> fks = potentialJoinTables.get(fkEntity);
                            if (fks == null)
                            {
                                fks = new ArrayList<Pair<Entity, List<String>>>();
                                potentialJoinTables.put(fkEntity, fks);
                            }
                            fks.add(Pair.of(pkEntity, fkColumns));
                        }
                    }
                }
                // reverse enginering of join tables
                if (getReverseMode().reverseExtended())
                {
                    for (Map.Entry<Entity, List<Pair<Entity, List<String>>>> entry : potentialJoinTables.entrySet())
                    {
                        Entity fkEntity = entry.getKey();
                        List<Pair<Entity, List<String>>> pks = entry.getValue();
                        // TODO - joins detection should be configurable
                        // for now:
                        // - join table must reference two different tables with distinct columns
                        // - join table name must be a snake case concatenation of both pk tables
                        if (pks.size() == 2)
                        {
                            List<String> leftFkColumns = pks.get(0).getRight();
                            List<String> rightFkColumns = pks.get(1).getRight();
                            if (Collections.disjoint(leftFkColumns, rightFkColumns))
                            {
                                Entity leftPK = pks.get(0).getLeft();
                                Entity rightPK = pks.get(1).getLeft();
                                String name1 = leftPK.getName() + "_" + rightPK.getName();
                                String name2 = rightPK.getName() + "_" + leftPK.getName();
                                if (fkEntity.getName().equals(name1) || fkEntity.getName().equals(name2))
                                {
                                    declareExtendedJoin(leftPK, leftFkColumns, fkEntity, rightFkColumns, rightPK);
                                    declareExtendedJoin(rightPK, rightFkColumns, fkEntity, leftFkColumns, leftPK);
                                }
                            }
                        }
                    }
                }
            }
        }
        finally
        {
            if (connection != null)
            {
                connection.leaveBusyState();
            }
        }
    }

    private void declareUpstreamJoin(Entity pkEntity, Entity fkEntity, List<String> fkColumns) throws SQLException
    {
        String upstreamAttributeName;
        if (fkColumns.size() == 1)
        {
            // if fk column name equals pk column name, take pk table name
            // else take fk column name with '_id' stripped off
            if (fkColumns.get(0).equals(pkEntity.getPrimaryKey().get(0).name))
            {
                upstreamAttributeName = pkEntity.getName();
            }
            else
            {
                // take fk column name with _id suffix stripped out
                upstreamAttributeName = fkColumns.get(0).toLowerCase(Locale.ROOT);
                if (upstreamAttributeName.length() > 3 && upstreamAttributeName.endsWith("_id"))
                {
                    upstreamAttributeName = upstreamAttributeName.substring(0, upstreamAttributeName.length() - 3);
                }
            }
        }
        else
        {
            upstreamAttributeName = pkEntity.getName(); // hope it's a singular
        }

        Attribute previous = fkEntity.getAttribute(upstreamAttributeName);
        if (previous != null)
        {
            getLogger().debug("explicit declaration of attribute {}.{} supersedes implicit imported key from {}", fkEntity.getName(), upstreamAttributeName, pkEntity.getName());
        }
        else
        {
            fkEntity.declareUpstreamJoin(upstreamAttributeName, pkEntity, fkColumns);
        }
    }

    private void declareJoinTowardsForeignKey(Entity pkEntity, Entity fkEntity, List<String> fkColumns) throws SQLException
    {
        String downstreamAttributeName = getIdentifiersFilters().pluralize(fkEntity.getName());
        Attribute previous = pkEntity.getAttribute(downstreamAttributeName);
        if (previous != null)
        {
            getLogger().debug("explicit declaration of attribute {}.{} supersedes implicit exported key towards {}", pkEntity.getName(), downstreamAttributeName, fkEntity.getName());
        }
        else
        {
            pkEntity.declareDownstreamJoin(downstreamAttributeName, fkEntity, fkColumns);
        }
    }

    private void declareExtendedJoin(Entity leftEntity, List<String> leftFKCols, Entity joinEntity, List<String> rightFKCols, Entity rightEntity) throws SQLException
    {
        String joinAttributeName = getIdentifiersFilters().pluralize(rightEntity.getName());
        Attribute previous = leftEntity.getAttribute(joinAttributeName);
        if (previous != null)
        {
            getLogger().debug("explicit declaration of attribute {}.{} supersedes implicit extended join {}", leftEntity.getName(), joinAttributeName, rightEntity.getName());
        }
        else
        {
            leftEntity.declareExtendedJoin(joinAttributeName, leftFKCols, joinEntity, rightFKCols, rightEntity);
        }
    }

    /*
     * Operations
     */

    protected final String quoteIdentifier(String identifier)
    {
        return driverInfos.quoteIdentifier(identifier);
    }

    /*
     * Helpers
     */

    protected ConnectionWrapper getCurrentTransactionConnection()
    {
        return StatementPool.getCurrentTransactionConnection(getModelId());
    }

    /**
     * <p>gather filters getters and setters in a subclass to ease configuration</p>
     * <p>Can be configured after initialization.</p>
     */
    public class FiltersSet
    {
        public FiltersSet()
        {
            readFilters = new ValueFilters("filters.read");
            writeFilters = new ValueFilters("filters.write");
        }

        public ValueFilters getReadFilters()
        {
            return readFilters;
        }

        public Model setReadMapping(Map filters)
        {
            ensureConfigured();
            readFilters.addMappings(filters);
            return getModel();
        }

        public ValueFilters getWriteFilters()
        {
            return writeFilters;
        }

        public Model setWriteMapping(Map filters)
        {
            ensureConfigured();
            writeFilters.addMappings(filters);
            return getModel();
        }

        public final Model setCryptographClass(String cryptographClass)
        {
            ensureConfigured();
            this.cryptographClass = cryptographClass;
            return getModel();
        }

        protected final void initialize()
        {
            if (readFilters.needsCryptograph() || writeFilters.needsCryptograph())
                try
                {
                    Cryptograph cryptograph = initCryptograph();
                    readFilters.setCryptograph(cryptograph);
                    writeFilters.setCryptograph(cryptograph);
                }
                catch (RuntimeException re)
                {
                    throw re;
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("could not initialize cryptograph", e);
                }

        }

        private final Cryptograph initCryptograph() throws Exception
        {
            if (cryptographClass == null)
            {
                throw new ConfigurationException("no cryptograph classname found in filters.cryptograph");
            }
            Class clazz = ClassUtils.getClass(cryptographClass);
            Cryptograph cryptograph = (Cryptograph)clazz.newInstance();
            String secret = getSecret();
            if (secret == null)
            {
                throw new ConfigurationException("no cryptograph secret: either definition file or database url must be provided");
            }
            cryptograph.init(getSecret());
            return cryptograph;
        }

        private final String getSecret()
        {
            return Optional.ofNullable(
                getDefinition()).map(x -> String.valueOf(x)).
                orElse(Optional.ofNullable(getDatabaseURL()).filter(x -> x.length() >= 16).
                    orElse("sixteen chars..."));
        }

        private String cryptographClass = null;
    }

    protected class UserInstancesConfig
    {

        protected Class getFactory()
        {
            return factory;
        }

        public Model setFactory(Object factory)
        {
            ensureConfigured();
            if (factory == null || factory instanceof Class)
            {
                this.factory = (Class)factory;
            }
            else if (factory instanceof String)
            {
                try
                {
                    this.factory = ClassUtils.getClass((String)factory);
                }
                catch (ClassNotFoundException cnfe)
                {
                    throw new ConfigurationException("cannot get instances factory", cnfe);
                }
            }
            else
            {
                throw new ConfigurationException("expecting factory class or classname");
            }
            return getModel();
        }

        protected Map<String, Class> getClasses()
        {
            return classes;
        }

        public Model setClasses(Map<String, ?> classes)
        {
            ensureConfigured();
            this.classes = new TreeMap<String, Class>();
            try
            {
                for (Map.Entry<String, ?> entry : classes.entrySet())
                {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Class clazz = null;
                    if (value instanceof String)
                    {
                        clazz = ClassUtils.getClass((String)value);
                    }
                    else if (value instanceof Class)
                    {
                        clazz = (Class)value;
                    }
                    this.classes.put(key, clazz);
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                throw new ConfigurationException("could not build instances classes map", cnfe);
            }
            return getModel();
        }

        protected void initialize()
        {
            Set<String> classProvided = new HashSet<String>();
            if (classes != null)
            {
                for (Map.Entry<String, Class> entry : classes.entrySet())
                {
                    String key = entry.getKey();
                    final Entity entity = getEntity(key);
                    final Class clazz = entry.getValue();
                    if (entity == null)
                    {
                        throw new ConfigurationException("instance.classes." + key + ": no entity named " + key);
                    }
                    if (Instance.class.isAssignableFrom(clazz))
                    {
                        try
                        {
                            final Constructor ctor = clazz.getDeclaredConstructor(Entity.class);
                            // ctor.setAccessible(true);
                            entity.setInstanceBuilder(() ->
                            {
                                try
                                {
                                    return (Instance)ctor.newInstance(entity);
                                }
                                catch (IllegalAccessException | InstantiationException | InvocationTargetException e)
                                {
                                    throw new RuntimeException("could not create instance of class " + clazz.getName());
                                }
                            });

                        }
                        catch (NoSuchMethodException nsme)
                        {
                            throw new ConfigurationException("Class " + clazz.getName() + " must declare a public ctor taking an Entity as argument");
                        }
                    }
                    else
                    {
                        entity.setInstanceBuilder(() ->
                        {
                            try
                            {
                                Object obj = clazz.newInstance();
                                return new WrappingInstance(entity, obj);
                            }
                            catch (InstantiationException | IllegalAccessException e)
                            {
                                throw new RuntimeException("could not create instance of class " + clazz.getName());
                            }
                        }, PropertyUtils.getPropertyDescriptors(clazz));

                    }
                    classProvided.add(key);
                }
            }
            if (factory != null)
            {
                for (final Entity entity : entitiesMap.values())
                {
                    if (!classProvided.contains(entity.getName()))
                    {
                        Method method = null;
                        String capitalized = StringUtils.capitalize(entity.getName());
                        for (String prefix : factoryMethodPrefixes)
                        {
                            try
                            {
                                method = factory.getMethod(prefix + capitalized);
                            }
                            catch (NoSuchMethodException e)
                            {
                            }
                        }
                        if (method != null)
                        {
                            // let's try it
                            Object obj;
                            try
                            {
                                obj = method.invoke(null);
                            }
                            catch (IllegalAccessException | InvocationTargetException e)
                            {
                                throw new ConfigurationException("factory instance creation failed for entity " + entity.getName(), e);
                            }
                            if (obj == null)
                            {
                                throw new ConfigurationException("factory instance creation returned null for entity " + entity.getName());
                            }
                            Class clazz = obj.getClass();
                            final Method creationMethod = method;
                            if (Instance.class.isAssignableFrom(clazz))
                            {
                                entity.setInstanceBuilder(() ->
                                {
                                    try
                                    {
                                        return (Instance)creationMethod.invoke(null);
                                    }
                                    catch (IllegalAccessException | InvocationTargetException e)
                                    {
                                        throw new RuntimeException("could not create instance of class " + clazz.getName());
                                    }
                                });
                            }
                            else
                            {
                                entity.setInstanceBuilder(() ->
                                {
                                    try
                                    {
                                        return new WrappingInstance(entity, creationMethod.invoke(null));
                                    }
                                    catch (IllegalAccessException | InvocationTargetException e)
                                    {
                                        throw new RuntimeException("could not create instance of class " + clazz.getName());
                                    }
                                });

                            }
                        }
                    }
                }
            }
        }

        private Class factory = null;

        private Map<String, Class> classes = null;
    }

    private String[] factoryMethodPrefixes = { "create", "new", "get" };

    /*
     * Members
     */

    private boolean configuring = false;
    private boolean configured = false;
    private boolean initialized = false;
    private ConfigurationException initializationError = null;

    private Object servletContext = null;

    private String modelId = null;

    public enum WriteAccess { NONE, JAVA, VTL }

    private WriteAccess writeAccess = WriteAccess.JAVA;

    public enum ReverseMode
    {
        NONE, COLUMNS, TABLES, JOINS, FULL, EXTENDED;

        public boolean reverseColumns()
        {
            return ordinal() > 0;
        }

        public boolean reverseTables()
        {
            return ordinal() == 2 || ordinal() > 3;
        }

        public boolean reverseJoins()
        {
            return ordinal() > 2;
        }
        public boolean reverseExtended()
        {
            return ordinal() == 5;
        }
    }

    private ReverseMode reverseMode = ReverseMode.NONE;

    // TODO - Velocity-aware model should be a subclass
    // private VelocityEngine velocityEngine = null;

    private String schema = null;

    /**
     * driver properties
     */
    private DriverInfos driverInfos = new DriverInfos();

    /**
     * Entities map
     */
    private NavigableMap<String, Entity> entitiesMap = new TreeMap<>();

    /**
     * Definition file URL
     */
    private URL definition = null;

    /**
     * Data source
     */
    private transient DataSource dataSource = null;

    private String databaseURL = null;

    /**
     * Pool of connections.
     */
    private transient ConnectionPool connectionPool = null;

    /**
     * Max connections.
     */
    private int maxConnections = 50; // applies to connectionPool and transactionConnectionPool

    /**
     * Connections check interval, -1 for non
     * (defaults to 5 minutes)
     */
    private long connectionsCheckInterval = 300;

    /**
     * Pool of connections for transactions.
     */
    private transient ConnectionPool transactionConnectionPool = null;

    /**
     * Pool of prepared statements.
     */
    private transient StatementPool statementPool = null;

    private transient Credentials credentials = new Credentials();

    /**
     * Identifiers mapper
     */
    private IdentifiersFilters identifiers = new IdentifiersFilters();

    /**
     * Value filters
     */

    /**
     * Explicit values converters when reading from database
     */
    protected ValueFilters readFilters = null;

    /**
     * Explicit values converters when writing to database
     */
    protected ValueFilters writeFilters = null;

    private FiltersSet filters = new FiltersSet();

    private UserInstancesConfig userInstancesConfig = new UserInstancesConfig();

    /**
     * <p>Implicit values converters, used when:</p>
     * <ul>
     *     <li>populating a wrapped instance with database values</li>
     *     <li>writing values to database whose driver needs strict SQL types (<code>driver.strict_column_types = true</code>)</li>
     * </ul>
     * <p>TODO: we may wish to distinguish those two converters, and/or to make them configurable.</p>
     */
    private ConversionHandler conversionHandler = new ConversionHandlerImpl();

}
