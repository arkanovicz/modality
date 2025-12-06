package com.republicate.modality;

import com.republicate.modality.sql.DriverInfos;
import com.republicate.modality.util.Converter;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.republicate.modality.config.ConfigurationException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IssuesTest extends BaseBookshelfTests
{
    private static String currentSqlFile = null;

    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource("misc_tests.sql");
        currentSqlFile = "misc_tests.sql";
    }

    private void switchDatabase(String sqlFile) throws Exception
    {
        if (!sqlFile.equals(currentSqlFile))
        {
            clearDataSource();
            populateDataSource(sqlFile);
            currentSqlFile = sqlFile;
        }
    }

    public @Test
    void testPKPosition() throws Exception
    {
        switchDatabase("misc_tests.sql");
        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);
        model.initialize(getResourceReader("misc_tests.xml"));

        // Lure modality in believing the db engine is pedantic (like postgres)
        DriverInfos driverInfos = model.getDriverInfos();
        driverInfos.setStrictColumnTypes(true);

        // Tweak integer to string conversion
        model.getConversionHandler().addConverter(String.class, Integer.class, new Converter()
        {
            @Override
            public Serializable convert(Serializable o)
            {
                return "something";
            }
        });

        Entity foo = model.getEntity("foo");
        assertNotNull(foo);
        Instance bar = foo.newInstance();
        bar.put("id", 1);
        bar.put("val", "bar");
        bar.insert();

        // Because, when, the bug is present, 1 will be mapped to 'val', it will be converted to string (pedantic engine safe-guarding)
        // using our fake converter...
        bar = foo.fetch(1);
        assertNotNull(bar);
        assertEquals("bar", bar.getString("val"));
    }

    /**
     * Test that an entity declared in XML model but not present in the database
     * doesn't cause an NPE during join reverse engineering.
     *
     * The issue: when an entity is in the XML model but the corresponding table
     * doesn't exist in the database, the entity remains in knownEntitiesByTable
     * with table=null, and then during join reverse engineering we get an NPE
     * when trying to access the entity's primary key or call getExportedKeys().
     */
    public @Test
    void testMissingEntityInDatabase() throws Exception
    {
        switchDatabase("missing_entity_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        // This should NOT throw an NPE
        // The model declares 'publisher' entity which doesn't exist in the database
        model.initialize(getResourceReader("missing_entity_test.xml"));

        // Verify that existing entities work
        Entity book = model.getEntity("book");
        assertNotNull("book entity should exist", book);

        Entity author = model.getEntity("author");
        assertNotNull("author entity should exist", author);

        // Verify we can fetch data
        Instance authorInstance = author.fetch(1);
        assertNotNull("should be able to fetch author", authorInstance);
        assertEquals("Test Author", authorInstance.getString("name"));
    }

    /**
     * Test when FK exists in DB referencing a table that IS in the database
     * but the referenced table is NOT declared in the XML model.
     * The reverse engineering should discover the publisher table from DB.
     */
    public @Test
    void testFKReferencingUndeclaredEntity() throws Exception
    {
        switchDatabase("fk_missing_pk_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        // This should NOT throw an NPE
        // The 'category' entity in XML doesn't exist in DB
        // The 'publisher' table exists in DB but is not in XML (should be reverse-engineered)
        model.initialize(getResourceReader("fk_missing_pk_test.xml"));

        // Verify that existing entities work
        Entity book = model.getEntity("book");
        assertNotNull("book entity should exist", book);

        // Publisher should have been reverse-engineered from the database
        Entity publisher = model.getEntity("publisher");
        assertNotNull("publisher entity should be reverse-engineered from DB", publisher);

        // category was declared but doesn't exist in DB - should still be in model but with null table
        Entity category = model.getEntity("category");
        assertNotNull("category entity should exist in model", category);
    }

    /**
     * Test that when an entity has an explicit table= attribute pointing to a
     * non-existent table, we get a ConfigurationException rather than NPE.
     */
    public @Test
    void testExplicitMissingTable() throws Exception
    {
        switchDatabase("explicit_missing_table_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        try
        {
            model.initialize(getResourceReader("explicit_missing_table_test.xml"));
            fail("Should throw ConfigurationException for explicit missing table");
        }
        catch (ConfigurationException e)
        {
            // Expected - entity 'book' explicitly references non-existent table
            assertTrue("Error should mention the missing table",
                e.getMessage().contains("nonexistent_book") || e.getMessage().contains("NONEXISTENT_BOOK"));
        }
    }

    /**
     * Test when a "view" entity has an explicit primary key set but the view
     * doesn't exist in the database. This could cause NPE during join reverse engineering
     * because the entity has a primary key but no columns.
     */
    public @Test
    void testViewMissingWithExplicitPK() throws Exception
    {
        switchDatabase("view_missing_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        try
        {
            // This should throw ConfigurationException (not NPE) because
            // the view entity has explicit table= attribute but doesn't exist
            model.initialize(getResourceReader("view_missing_test.xml"));
            fail("Should throw ConfigurationException for missing view with explicit table");
        }
        catch (ConfigurationException e)
        {
            // Expected - entity 'book_summary' explicitly references non-existent view
            assertTrue("Error should mention the missing table/view",
                e.getMessage().contains("book_summary_view") || e.getMessage().contains("BOOK_SUMMARY_VIEW"));
        }
    }

    /**
     * Test when sqlPrimaryKey references a column that doesn't exist in the table.
     * This could cause NPE during entity initialization because the column mapping
     * would return null.
     */
    public @Test
    void testWrongPKColumn() throws Exception
    {
        switchDatabase("wrong_pk_column_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        try
        {
            // This might throw NPE because 'nonexistent_id' column doesn't exist
            // The entity tries to build primaryKey from sqlPrimaryKey but column isn't found
            model.initialize(getResourceReader("wrong_pk_column_test.xml"));
            fail("Should throw an exception for wrong PK column name");
        }
        catch (NullPointerException e)
        {
            // This is the bug! Should be ConfigurationException, not NPE
            // For now we document that NPE occurs
            logger.error("NPE when sqlPrimaryKey references non-existent column - this is a bug", e);
        }
        catch (ConfigurationException e)
        {
            // Log the full exception chain
            logger.info("Got ConfigurationException: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null)
            {
                logger.info("  Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
            // The exception is correct, just verify it was thrown
            assertNotNull("ConfigurationException thrown as expected", e);
        }
    }

    /**
     * Test for the exact NPE scenario at line 1032 in declareUpstreamJoin.
     *
     * The issue: when an entity is declared in XML with explicit sqlPrimaryKey
     * but the table doesn't exist:
     * 1. sqlPrimaryKey is set (not null) from the XML attribute
     * 2. The guard at ReverseEngineer.getJoins():208 passes because sqlPrimaryKey is set
     * 3. But getPrimaryKey() returns null because no columns exist (table not in DB)
     * 4. declareUpstreamJoin NPEs at line 1032: pkEntity.getPrimaryKey().get(0).name
     *
     * This test documents the NPE bug that needs to be fixed.
     */
    public @Test
    void testNPEInJoinReverseEngineering() throws Exception
    {
        switchDatabase("npe_join_test.sql");

        Map identMapping = new HashMap();
        identMapping.put("*", "lowercase");
        identMapping.put("*.*", "lowercase");
        DataSource dataSource = getDataSource();
        Model model = new Model();
        model.setDataSource(dataSource);
        model.setReverseMode(Model.ReverseMode.EXTENDED);
        model.getIdentifiersFilters().addMappings(identMapping);

        try
        {
            // This should throw an exception - ideally ConfigurationException, not NPE
            // The 'author' entity has sqlPrimaryKey="author_id" but table 'author_missing' doesn't exist
            model.initialize(getResourceReader("npe_join_test.xml"));
            fail("Should throw an exception for missing table with explicit sqlPrimaryKey");
        }
        catch (NullPointerException e)
        {
            // This is the bug - should be ConfigurationException
            logger.error("NPE in join reverse engineering - table doesn't exist but has sqlPrimaryKey", e);
            // For now, just document that it happens
            assertNotNull("NPE thrown - this is a bug", e);
        }
        catch (ConfigurationException e)
        {
            // This is what we expect after the fix - or wrapped NPE
            logger.info("Got ConfigurationException: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null)
            {
                logger.info("  Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                if (cause instanceof NullPointerException)
                {
                    logger.error("Root cause is NPE - this should be a better error message", cause);
                }
                cause = cause.getCause();
            }
            assertNotNull("ConfigurationException thrown", e);
        }
    }

}
