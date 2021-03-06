package org.greenplum.pxf.plugins.hbase;

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


import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.plugins.hbase.utilities.HBaseTupleDescription;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HBaseAccessor.class, HBaseConfiguration.class, ConnectionFactory.class})
public class HBaseAccessorTest {
    static final String tableName = "fishy_HBase_table";

    private RequestContext context;
    HBaseTupleDescription tupleDescription;
    Table table;
    Scan scanDetails;
    Configuration hbaseConfiguration;
    Connection hbaseConnection;
    HBaseAccessor accessor;

    /*
	 * After each test is done, close the accessor
	 * if it was created
	 */
    @After
    public void tearDown() throws Exception {
        if (accessor == null) {
            return;
        }

        closeAccessor();
        accessor = null;
    }

	/*
	 * Test construction of HBaseAccessor.
	 * Actually no need for this as it is tested in all other tests
	 * constructing HBaseAccessor but it serves as a simple example
	 * of mocking
	 *
	 * HBaseAccessor is created and then HBaseTupleDescriptioncreation
	 * is verified
	 */
    @Test
    public void construction() throws Exception {
        prepareConstruction();
        HBaseAccessor accessor = new HBaseAccessor();
        accessor.initialize(context);
        PowerMockito.verifyNew(HBaseTupleDescription.class).withArguments(context);
    }

	/*
	 * Test Open returns false when table has no regions
	 *
	 * Done by returning an empty Map from getRegionLocations
	 * Verify Scan object doesn't contain any columns / filters
	 * Verify scan did not start
	 */
    @Test
    @Ignore
    @SuppressWarnings("unchecked")
    public void tableHasNoMetadata() throws Exception {
        prepareConstruction();
        prepareTableOpen();
        prepareEmptyScanner();

        accessor = new HBaseAccessor();
        accessor.initialize(context);

        try {
            accessor.openForRead();
            fail("should throw no metadata exception");
        } catch (Exception e) {
            assertEquals("Missing fragment metadata information", e.getMessage());
        }

        verifyScannerDidNothing();
    }

    /*
     * Helper for test setup.
     * Creates a mock for HBaseTupleDescription and RequestContext
     */
    private void prepareConstruction() throws Exception {
        context = new RequestContext();
        context.setConfig("default");
        context.setUser("test-user");
        tupleDescription = mock(HBaseTupleDescription.class);
        PowerMockito.whenNew(HBaseTupleDescription.class).withArguments(context).thenReturn(tupleDescription);
    }

    /*
     * Helper for test setup.
     * Adds a table name and prepares for table creation
     */
    private void prepareTableOpen() throws Exception {
        // Set table name
        context.setDataSource(tableName);

        // Make sure we mock static functions in HBaseConfiguration
        PowerMockito.mockStatic(HBaseConfiguration.class);

        hbaseConfiguration = mock(Configuration.class);
        when(HBaseConfiguration.create()).thenReturn(hbaseConfiguration);

        // Make sure we mock static functions in ConnectionFactory
        PowerMockito.mockStatic(ConnectionFactory.class);
        hbaseConnection = mock(Connection.class);
        when(ConnectionFactory.createConnection(hbaseConfiguration)).thenReturn(hbaseConnection);
        table = mock(Table.class);
        when(hbaseConnection.getTable(TableName.valueOf(tableName))).thenReturn(table);
    }

    /*
     * Helper for test setup.
     * Sets zero columns (not realistic) and no filter
     */
    private void prepareEmptyScanner() throws Exception {
        scanDetails = mock(Scan.class);
        PowerMockito.whenNew(Scan.class).withNoArguments().thenReturn(scanDetails);

        when(tupleDescription.columns()).thenReturn(0);
    }

    /*
     * Verify Scan object was used but didn't do much
     */
    private void verifyScannerDidNothing() throws Exception {
        // setMaxVersions was called with 1
        verify(scanDetails).setMaxVersions(1);
        // addColumn was not called
        verify(scanDetails, never()).addColumn(any(byte[].class), any(byte[].class));
        // addFilter was not called
        verify(scanDetails, never()).setFilter(any(org.apache.hadoop.hbase.filter.Filter.class));
        // Nothing else was missed
        verifyNoMoreInteractions(scanDetails);
        // Scanner was not used
        verify(table, never()).getScanner(scanDetails);
    }

    /*
     * Close the accessor and make sure table was closed
     */
    private void closeAccessor() throws Exception {
        accessor.closeForRead();
        verify(table).close();
    }
}
