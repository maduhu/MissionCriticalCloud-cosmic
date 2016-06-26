package com.cloud.usage;

import static org.junit.Assert.assertEquals;

import com.cloud.utils.PropertiesUtil;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mysql.MySqlConnection;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class UsageSanityCheckerIT {

    protected static final String EXPECTED_ERRORS_1 = "Error: Found 2 usage records with raw_usage > 10\n" +
            "Error: Found 1 Vm usage records which are created after Vm is destroyed\n" +
            "Error: Found 2 duplicate allocated Vm entries in vm usage helper table\n" +
            "Error: Found 1 running Vm entries without corresponding allocated entries in vm usage helper table\n" +
            "Error: Found 1 volume usage records which are created after volume is removed\n" +
            "Error: Found 1 template/ISO usage records which are created after it is removed\n" +
            "Error: Found 1 snapshot usage records which are created after it is removed\n";
    protected static final String EXPECTED_ERRORS_2 = "Error: Found 3 usage records with raw_usage > 10\n" +
            "Error: Found 1 Vm usage records which are created after Vm is destroyed\n" +
            "Error: Found 8 duplicate running Vm entries in vm usage helper table\n" +
            "Error: Found 4 duplicate allocated Vm entries in vm usage helper table\n" +
            "Error: Found 4 running Vm entries without corresponding allocated entries in vm usage helper table\n" +
            "Error: Found 2 volume usage records which are created after volume is removed\n" +
            "Error: Found 6 duplicate records in volume usage helper table\n" +
            "Error: Found 2 template/ISO usage records which are created after it is removed\n" +
            "Error: Found 1 snapshot usage records which are created after it is removed\n";
    protected static final String EXPECTED_ERRORS_3 = "";
    protected Connection cloudConn;
    protected Connection usageConn;
    protected MySqlConnection dbuUsageConn;
    protected MySqlConnection dbuCloudConn;
    protected Properties properties = new Properties();
    protected IDataSet cloudDataSet;
    protected IDataSet usageDataSet;
    protected String cloudDbuFileName;
    protected String usageDbuFileName;
    protected String expectedErrors;

    public UsageSanityCheckerIT(final String cloudDbuFileName, final String usageDbuFileName,
                                final String expectedErrors) {
        this.cloudDbuFileName = cloudDbuFileName;
        this.usageDbuFileName = usageDbuFileName;
        this.expectedErrors = expectedErrors;
    }

    @Parameters
    public static Collection<Object[]> data() {
        final Object[][] data = new Object[][]{
                {"cloud1.xml", "cloud_usage1.xml", EXPECTED_ERRORS_1},
                {"cloud2.xml", "cloud_usage2.xml", EXPECTED_ERRORS_2},
                {"cloud3.xml", "cloud_usage3.xml", EXPECTED_ERRORS_3}
        };
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        PropertiesUtil.loadFromFile(properties, PropertiesUtil.findConfigFile("db.properties"));

        Class.forName("com.mysql.jdbc.Driver");
        cloudConn = createConnection("cloud");
        usageConn = createConnection("usage");

        dbuCloudConn = new MySqlConnection(cloudConn, properties.getProperty("db.cloud.name"));
        dbuUsageConn = new MySqlConnection(usageConn, properties.getProperty("db.usage.name"));
        cloudDataSet = getCloudDataSet();
        usageDataSet = getUsageDataSet();
        DatabaseOperation.CLEAN_INSERT.execute(dbuCloudConn, cloudDataSet);
        DatabaseOperation.CLEAN_INSERT.execute(dbuUsageConn, usageDataSet);
    }

    protected Connection createConnection(final String dbSchema) throws SQLException {
        final String cloudDbUrl = "jdbc:mysql://" + properties.getProperty("db." + dbSchema + ".host") +
                ":" + properties.getProperty("db." + dbSchema + ".port") + "/" +
                properties.getProperty("db." + dbSchema + ".name");
        return DriverManager.getConnection(cloudDbUrl, properties.getProperty("db." + dbSchema + ".username"),
                properties.getProperty("db." + dbSchema + ".password"));
    }

    protected IDataSet getCloudDataSet() throws DataSetException, FileNotFoundException {
        return new FlatXmlDataSetBuilder().build(PropertiesUtil.openStreamFromURL(cloudDbuFileName));
    }

    protected IDataSet getUsageDataSet() throws DataSetException, FileNotFoundException {
        return new FlatXmlDataSetBuilder().build(PropertiesUtil.openStreamFromURL(usageDbuFileName));
    }

    @After
    public void tearDown() throws DataSetException, FileNotFoundException, DatabaseUnitException, SQLException {
        DatabaseOperation.DELETE_ALL.execute(dbuCloudConn, getCloudDataSet());
        DatabaseOperation.DELETE_ALL.execute(dbuUsageConn, getUsageDataSet());
    }

    @Test
    public void testRunSanityCheck() throws SQLException, ClassNotFoundException, FileNotFoundException, DatabaseUnitException {
        // Prepare
        final UsageSanityChecker checker = Mockito.spy(new UsageSanityChecker());
        Mockito.doReturn(cloudConn).when(checker).getConnection();
        Mockito.doNothing().when(checker).readLastCheckId();
        Mockito.doNothing().when(checker).updateNewMaxId();
        checker.lastId = 2;

        // Execute
        final String actualErrors = checker.runSanityCheck();

        // Assert
        assertEquals("Expected errors not found", expectedErrors, actualErrors);
    }
}
