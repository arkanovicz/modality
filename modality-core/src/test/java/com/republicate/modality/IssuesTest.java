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

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IssuesTest extends BaseBookshelfTests
{
    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource("misc_tests.sql");
    }

    public @Test
    void testPKPosition() throws Exception
    {
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


}
