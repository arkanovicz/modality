package com.republicate.modality.tools.model;

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
import com.republicate.modality.RowAttribute;
import com.republicate.modality.RowsetAttribute;
import com.republicate.modality.ScalarAttribute;
import com.republicate.modality.config.Constants;
import com.republicate.modality.impl.ReverseEngineer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.config.ValidScope;
import org.apache.velocity.tools.generic.SafeConfig;
import org.apache.velocity.tools.generic.ValueParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>MetaModelTool</p>
 *
 * @author Claude Brisson
 * @version $Revision: $
 * @since VelocityTools 3.1
 */

@ValidScope(Scope.APPLICATION)
@DefaultKey("model")
public class MetaModelTool extends SafeConfig implements Constants, Serializable
{
    @Override
    protected void configure(ValueParser params)
    {
        String modelId = Optional.ofNullable(params.getString("key")).orElse(Model.DEFAULT_MODEL_ID);
        model = getModel(params.get("servletContext"), modelId);
        if (model == null)
        {
            throw new RuntimeException("meta model tool cannot find model");
        }
        try
        {
            metaModel = model.getMetaModel();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException("cannot configure meta model tool", sqle);
        }
    }

    protected Model getModel(Object context, String modelId)
    {
        Model model = ModelRepository.getModel(context, modelId);
        return model;
    }

    protected void error(String message, Object... arguments)
    {
        // default implementation only logs
        getLog().error(message, arguments);
    }

    public List<String> getTables()
    {
        try
        {
            return metaModel.getTables();
        }
        catch (SQLException sqle)
        {
            error("could not get tables list", sqle);
            return null;
        }
    }

    public List<ColumnDesc> getColumns(String entityName)
    {
        try
        {
            Entity entity = model.getEntity(entityName);
            if (entity == null)
            {
                error("unknown entity: {}", entityName);
                return null;
            }
            List<Entity.Column> cols = metaModel.getColumns(entity);
            return cols.stream().map(col -> new ColumnDesc(col)).collect(Collectors.toList());
        }
        catch (SQLException sqle)
        {
            error("could not get columns of entity {}", entityName, sqle);
            return null;
        }
    }

    public List<String> getPrimaryKey(String entityName)
    {
        try
        {
            Entity entity = model.getEntity(entityName);
            if (entity == null)
            {
                error("unknown entity: {}", entityName);
                return null;
            }
            String keyCols[] = metaModel.getPrimaryKey(entity);
            return Arrays.asList(keyCols);
        }
        catch (SQLException sqle)
        {
            error("could not get primary key of entity {}", entityName, sqle);
            return null;
        }
    }

    public List<Pair<String, List<String>>> getJoins(String pkEntityName)
    {
        try
        {
            Entity pkEntity = model.getEntity(pkEntityName);
            if (pkEntity == null)
            {
                error("unknown entity: {}", pkEntityName);
                return null;
            }
            return metaModel.getJoins(pkEntity);
        }
        catch (SQLException sqle)
        {
            error("could not get joins of entity {}", pkEntityName, sqle);
            return null;
        }
    }

    public List<String> describeEnum(String entityName, String columnName) throws SQLException
    {
        try
        {
            Entity entity = model.getEntity(entityName);
            if (entity == null)
            {
                error("unknown entity: {}", entityName);
                return null;
            }
            return metaModel.describeEnum(entity, columnName);
        }
        catch (SQLException sqle)
        {
            error("could not get primary key of entity {}", entityName, sqle);
            return null;
        }

    }

    private transient Model model = null;
    private transient ReverseEngineer metaModel = null;

    protected static class ColumnDesc
    {
        public ColumnDesc(Entity.Column col)
        {
            this.column = col;
        }

        public String getName()
        {
            return column.name;
        }

        public int getType()
        {
            return column.type;
        }

        public Integer getSize()
        {
            return column.size;
        }

        public boolean isGenerated()
        {
            return column.generated;
        }

        public int getIndex()
        {
            return column.getIndex();
        }

        public boolean isKeyColumn()
        {
            return column.isKeyColumn();
        }

        private Entity.Column column;
    }
}
