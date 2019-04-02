package org.greenplum.pxf.plugins.hive.utilities;

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


import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Reader;
import org.apache.hadoop.hive.ql.io.sarg.SearchArgument;
import org.apache.hadoop.hive.ql.io.sarg.SearchArgumentFactory;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.*;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.security.UserGroupInformation;
import org.greenplum.pxf.api.BasicFilter;
import org.greenplum.pxf.api.LogicalFilter;
import org.greenplum.pxf.api.UnsupportedTypeException;
import org.greenplum.pxf.api.io.DataType;
import org.greenplum.pxf.api.model.Metadata;
import org.greenplum.pxf.api.model.Metadata.Field;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.api.utilities.ColumnDescriptor;
import org.greenplum.pxf.api.utilities.EnumGpdbType;
import org.greenplum.pxf.api.utilities.Utilities;
import org.greenplum.pxf.plugins.hive.*;
import org.greenplum.pxf.plugins.hive.HiveInputFormatFragmenter.PXF_HIVE_INPUT_FORMATS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import static org.greenplum.pxf.api.model.ConfigurationFactory.PXF_CONFIG_RESOURCE_PATH_PROPERTY;


/**
 * Class containing helper functions connecting
 * and interacting with Hive.
 */
public class HiveUtilities {

    private static final Logger LOG = LoggerFactory.getLogger(HiveUtilities.class);
    private static final String WILDCARD = "*";

    /**
     * Default Hive DB (schema) name.
     */
    private static final String HIVE_DEFAULT_DBNAME = "default";

    static final String STR_RC_FILE_INPUT_FORMAT = "org.apache.hadoop.hive.ql.io.RCFileInputFormat";
    static final String STR_TEXT_FILE_INPUT_FORMAT = "org.apache.hadoop.mapred.TextInputFormat";
    static final String STR_ORC_FILE_INPUT_FORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat";
    private static final int DEFAULT_DELIMITER_CODE = 44;

    /**
     * Initializes HiveConf configuration object from request configuration. Since hive-site.xml
     * is not available on classpath due to multi-server support, it is added explicitly based
     * on location for a given PXF configuration server
     * @param configuration request configuration
     * @return instance of HiveConf object
     */
    public static HiveConf getHiveConf(Configuration configuration) {
        // prepare hiveConf object and explicitly add this request's hive-site.xml file to it
        HiveConf hiveConf = new HiveConf(configuration, HiveConf.class);

        String hiveSiteUrl = configuration.get(String.format("%s.%s", PXF_CONFIG_RESOURCE_PATH_PROPERTY, "hive-site.xml"));
        if (hiveSiteUrl != null) {
            try {
                hiveConf.addResource(new URL(hiveSiteUrl));
            } catch (MalformedURLException e) {
                throw new RuntimeException(
                        String.format("Failed to add %s to hive configuration", hiveSiteUrl), e);
            }
        }
        return hiveConf;
    }
    /**
     * Initializes the HiveMetaStoreClient
     * Uses classpath configuration files to locate the MetaStore
     *
     * @return initialized client
     */
    public static HiveMetaStoreClient initHiveClient(Configuration configuration) {
        HiveConf hiveConf = HiveUtilities.getHiveConf(configuration);
        try {
            if (UserGroupInformation.isSecurityEnabled()) {
                LOG.debug("initialize HiveMetaStoreClient as login user '{}'", UserGroupInformation.getLoginUser().getUserName());
                // wrap in doAs for Kerberos to propagate kerberos tokens from login Subject
                return UserGroupInformation.getLoginUser().
                        doAs((PrivilegedExceptionAction<HiveMetaStoreClient>) () ->
                                new HiveMetaStoreClient(hiveConf));
            } else {
                return new HiveMetaStoreClient(hiveConf);
            }
        } catch (MetaException | InterruptedException | IOException e) {
            throw new RuntimeException("Failed connecting to Hive MetaStore service: " + e.getMessage(), e);
        }
    }

    public static Table getHiveTable(HiveMetaStoreClient client, Metadata.Item itemName)
            throws Exception {
        Table tbl = client.getTable(itemName.getPath(), itemName.getName());
        String tblType = tbl.getTableType();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Item: " + itemName.getPath() + "." + itemName.getName() + ", type: " + tblType);
        }

        if (TableType.valueOf(tblType) == TableType.VIRTUAL_VIEW) {
            throw new UnsupportedOperationException("Hive views are not supported by GPDB");
        }

        return tbl;
    }

    /**
     * Checks if hive type is supported, and if so return its matching GPDB
     * type. Unsupported types will result in an exception. <br>
     * The supported mappings are:
     * <ul>
     * <li>{@code tinyint -> int2}</li>
     * <li>{@code smallint -> int2}</li>
     * <li>{@code int -> int4}</li>
     * <li>{@code bigint -> int8}</li>
     * <li>{@code boolean -> bool}</li>
     * <li>{@code float -> float4}</li>
     * <li>{@code double -> float8}</li>
     * <li>{@code string -> text}</li>
     * <li>{@code binary -> bytea}</li>
     * <li>{@code timestamp -> timestamp}</li>
     * <li>{@code date -> date}</li>
     * <li>{@code decimal(precision, scale) -> numeric(precision, scale)}</li>
     * <li>{@code varchar(size) -> varchar(size)}</li>
     * <li>{@code char(size) -> bpchar(size)}</li>
     * <li>{@code array<dataType> -> text}</li>
     * <li>{@code map<keyDataType, valueDataType> -> text}</li>
     * <li>{@code struct<field1:dataType,...,fieldN:dataType> -> text}</li>
     * <li>{@code uniontype<...> -> text}</li>
     * </ul>
     *
     * @param hiveColumn
     *            hive column schema
     * @return field with mapped GPDB type and modifiers
     * @throws UnsupportedTypeException
     *             if the column type is not supported
     * @see EnumHiveToGpdbType
     */
    public static Metadata.Field mapHiveType(FieldSchema hiveColumn) throws UnsupportedTypeException {
        String fieldName = hiveColumn.getName();
        String hiveType = hiveColumn.getType(); // Type name and modifiers if any
        String hiveTypeName; // Type name
        String[] modifiers = null; // Modifiers
        EnumHiveToGpdbType hiveToGpdbType = EnumHiveToGpdbType.getHiveToGpdbType(hiveType);
        EnumGpdbType gpdbType = hiveToGpdbType.getGpdbType();

        if (hiveToGpdbType.getSplitExpression() != null) {
            String[] tokens = hiveType.split(hiveToGpdbType.getSplitExpression());
            hiveTypeName = tokens[0];
            if (gpdbType.getModifiersNum() > 0) {
                modifiers = Arrays.copyOfRange(tokens, 1, tokens.length);
                if (modifiers.length != gpdbType.getModifiersNum()) {
                    throw new UnsupportedTypeException(
                            "GPDB does not support type " + hiveType
                                    + " (Field " + fieldName + "), "
                                    + "expected number of modifiers: "
                                    + gpdbType.getModifiersNum()
                                    + ", actual number of modifiers: "
                                    + modifiers.length);
                }
                if (!verifyIntegerModifiers(modifiers)) {
                    throw new UnsupportedTypeException("GPDB does not support type " + hiveType + " (Field " + fieldName + "), modifiers should be integers");
                }
            }
        } else
            hiveTypeName = hiveType;

        return new Metadata.Field(fieldName, gpdbType, hiveToGpdbType.isComplexType(), hiveTypeName, modifiers);
    }

    /**
     * Verifies modifiers are null or integers.
     * Modifier is a value assigned to a type,
     * e.g. size of a varchar - varchar(size).
     *
     * @param modifiers type modifiers to be verified
     * @return whether modifiers are null or integers
     */
    private static boolean verifyIntegerModifiers(String[] modifiers) {
        if (modifiers == null) {
            return true;
        }
        for (String modifier : modifiers) {
            if (StringUtils.isBlank(modifier) || !StringUtils.isNumeric(modifier)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts the db_name and table_name from the qualifiedName.
     * qualifiedName is the Hive table name that the user enters in the CREATE EXTERNAL TABLE statement
     * or when querying HCatalog table.
     * It can be either <code>table_name</code> or <code>db_name.table_name</code>.
     *
     * @param qualifiedName Hive table name
     * @return {@link Metadata.Item} object holding the full table name
     */
    public static Metadata.Item extractTableFromName(String qualifiedName) {
        List<Metadata.Item> items = extractTablesFromPattern(null, qualifiedName);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No tables found");
        }
        return items.get(0);
    }

    /**
     * Extracts the db_name(s) and table_name(s) corresponding to the given pattern.
     * pattern is the Hive table name or pattern that the user enters in the CREATE EXTERNAL TABLE statement
     * or when querying HCatalog table.
     * It can be either <code>table_name_pattern</code> or <code>db_name_pattern.table_name_pattern</code>.
     *
     * @param client  Hivemetastore client
     * @param pattern Hive table name or pattern
     * @return list of {@link Metadata.Item} objects holding the full table name
     */
    public static List<Metadata.Item> extractTablesFromPattern(HiveMetaStoreClient client, String pattern) {

        String dbPattern, tablePattern;
        String errorMsg = " is not a valid Hive table name. "
                + "Should be either <table_name> or <db_name.table_name>";

        if (StringUtils.isBlank(pattern)) {
            throw new IllegalArgumentException("empty string" + errorMsg);
        }

        String[] rawToks = pattern.split("[.]");
        ArrayList<String> toks = new ArrayList<String>();
        for (String tok : rawToks) {
            if (StringUtils.isBlank(tok)) {
                continue;
            }
            toks.add(tok.trim());
        }

        if (toks.size() == 1) {
            dbPattern = HIVE_DEFAULT_DBNAME;
            tablePattern = toks.get(0);
        } else if (toks.size() == 2) {
            dbPattern = toks.get(0);
            tablePattern = toks.get(1);
        } else {
            throw new IllegalArgumentException("\"" + pattern + "\"" + errorMsg);
        }

        return getTablesFromPattern(client, dbPattern, tablePattern);
    }

    private static List<Metadata.Item> getTablesFromPattern(HiveMetaStoreClient client, String dbPattern, String tablePattern) {

        List<String> databases = null;
        List<Metadata.Item> itemList = new ArrayList<Metadata.Item>();
        List<String> tables = new ArrayList<String>();

        if (client == null || (!dbPattern.contains(WILDCARD) && !tablePattern.contains(WILDCARD))) {
            /* This case occurs when the call is invoked as part of the fragmenter api or when metadata is requested for a specific table name */
            itemList.add(new Metadata.Item(dbPattern, tablePattern));
            return itemList;
        }

        try {
            databases = client.getDatabases(dbPattern);
            if (databases.isEmpty()) {
                LOG.warn("No database found for the given pattern: " + dbPattern);
                return null;
            }
            for (String dbName : databases) {
                for (String tableName : client.getTables(dbName, tablePattern)) {
                    itemList.add(new Metadata.Item(dbName, tableName));
                }
            }
            return itemList;

        } catch (MetaException cause) {
            throw new RuntimeException("Failed connecting to Hive MetaStore service: " + cause.getMessage(), cause);
        }
    }


    /**
     * Converts GPDB type to hive type.
     * @see EnumHiveToGpdbType For supported mappings
     * @param type      GPDB data type
     * @param modifiers Integer array of modifier info
     * @return Hive type
     * @throws UnsupportedTypeException if type is not supported
     */
    public static String toCompatibleHiveType(DataType type, Integer[] modifiers) {

        EnumHiveToGpdbType hiveToGpdbType = EnumHiveToGpdbType.getCompatibleHiveToGpdbType(type);
        return EnumHiveToGpdbType.getFullHiveTypeName(hiveToGpdbType, modifiers);
    }



    /**
     * Validates whether given GPDB and Hive data types are compatible.
     * If data type could have modifiers, GPDB data type is valid if it hasn't modifiers at all
     * or GPDB's modifiers are greater or equal to Hive's modifiers.
     * <p>
     * For example:
     * <p>
     * Hive type - varchar(20), GPDB type varchar - valid.
     * <p>
     * Hive type - varchar(20), GPDB type varchar(20) - valid.
     * <p>
     * Hive type - varchar(20), GPDB type varchar(25) - valid.
     * <p>
     * Hive type - varchar(20), GPDB type varchar(15) - invalid.
     *
     *
     * @param gpdbDataType   GPDB data type
     * @param gpdbTypeMods   GPDB type modifiers
     * @param hiveType       full Hive type, i.e. decimal(10,2)
     * @param gpdbColumnName Hive column name
     * @throws UnsupportedTypeException if types are incompatible
     */
    public static void validateTypeCompatible(DataType gpdbDataType, Integer[] gpdbTypeMods, String hiveType, String gpdbColumnName) {

        EnumHiveToGpdbType hiveToGpdbType = EnumHiveToGpdbType.getHiveToGpdbType(hiveType);
        EnumGpdbType expectedGpdbType = hiveToGpdbType.getGpdbType();

        if (!expectedGpdbType.getDataType().equals(gpdbDataType)) {
            throw new UnsupportedTypeException("Invalid definition for column " + gpdbColumnName
                    + ": expected GPDB type " + expectedGpdbType.getDataType() +
                    ", actual GPDB type " + gpdbDataType);
        }

        switch (gpdbDataType) {
            case NUMERIC:
            case VARCHAR:
            case BPCHAR:
                if (gpdbTypeMods != null && gpdbTypeMods.length > 0) {
                    Integer[] hiveTypeModifiers = EnumHiveToGpdbType
                            .extractModifiers(hiveType);
                    for (int i = 0; i < hiveTypeModifiers.length; i++) {
                        if (gpdbTypeMods[i] < hiveTypeModifiers[i])
                            throw new UnsupportedTypeException(
                                    "Invalid definition for column " + gpdbColumnName
                                            + ": modifiers are not compatible, "
                                            + Arrays.toString(hiveTypeModifiers) + ", "
                                            + Arrays.toString(gpdbTypeMods));
                    }
                }
                break;
        }
    }

    /* Turns a Properties class into a string */
    private static String serializeProperties(Properties props) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        props.store(outStream, ""/* comments */);
        return outStream.toString();
    }

    /*
     * Validates that partition format corresponds to PXF supported formats and
     * transforms the class name to an enumeration for writing it to the
     * accessors on other PXF instances.
     */
    private static String assertFileType(String className, HiveTablePartition partData)
            throws Exception {
        switch (className) {
            case STR_RC_FILE_INPUT_FORMAT:
                return PXF_HIVE_INPUT_FORMATS.RC_FILE_INPUT_FORMAT.name();
            case STR_TEXT_FILE_INPUT_FORMAT:
                return PXF_HIVE_INPUT_FORMATS.TEXT_FILE_INPUT_FORMAT.name();
            case STR_ORC_FILE_INPUT_FORMAT:
                return PXF_HIVE_INPUT_FORMATS.ORC_FILE_INPUT_FORMAT.name();
            default:
                throw new IllegalArgumentException(
                        "HiveInputFormatFragmenter does not yet support "
                                + className
                                + " for "
                                + partData
                                + ". Supported InputFormat are "
                                + Arrays.toString(PXF_HIVE_INPUT_FORMATS.values()));
        }
    }


    /* Turns the partition keys into a string */
    public static String serializePartitionKeys(HiveTablePartition partData) throws Exception {
        if (partData.partition == null) /*
         * this is a simple hive table - there
         * are no partitions
         */ {
            return HiveDataFragmenter.HIVE_NO_PART_TBL;
        }

        StringBuilder partitionKeys = new StringBuilder();
        String prefix = "";
        ListIterator<String> valsIter = partData.partition.getValues().listIterator();
        ListIterator<FieldSchema> keysIter = partData.partitionKeys.listIterator();
        while (valsIter.hasNext() && keysIter.hasNext()) {
            FieldSchema key = keysIter.next();
            String name = key.getName();
            String type = key.getType();
            String val = valsIter.next();
            String oneLevel = prefix + name + HiveDataFragmenter.HIVE_1_PART_DELIM + type
                    + HiveDataFragmenter.HIVE_1_PART_DELIM + val;
            partitionKeys.append(oneLevel);
            prefix = HiveDataFragmenter.HIVE_PARTITIONS_DELIM;
        }

        return partitionKeys.toString();
    }

    /**
     * The method which serializes fragment-related attributes, needed for reading and resolution to string
     *
     * @param fragmenterClassName fragmenter class name
     * @param partData            partition data
     * @param filterInFragmenter  whether filtering was done in fragmenter
     * @return serialized representation of fragment-related attributes
     * @throws Exception when error occurred during serialization
     */
    @SuppressWarnings("unchecked")
    public static byte[] makeUserData(String fragmenterClassName, HiveTablePartition partData, boolean filterInFragmenter) throws Exception {

        HiveUserData hiveUserData = null;

        if (fragmenterClassName == null) {
            throw new IllegalArgumentException("No fragmenter provided.");
        }

        Class fragmenterClass = Class.forName(fragmenterClassName);

        String inputFormatName = partData.storageDesc.getInputFormat();
        String serdeClassName = partData.storageDesc.getSerdeInfo().getSerializationLib();
        String propertiesString = serializeProperties(partData.properties);
        String partitionKeys = serializePartitionKeys(partData);
        String delimiter = getDelimiterCode(partData.storageDesc).toString();
        String colTypes = partData.properties.getProperty("columns.types");
        int skipHeader = Integer.parseInt(partData.properties.getProperty("skip.header.line.count", "0"));

        if (HiveInputFormatFragmenter.class.isAssignableFrom(fragmenterClass)) {
            assertFileType(inputFormatName, partData);
        }

        hiveUserData = new HiveUserData(inputFormatName, serdeClassName, propertiesString, partitionKeys, filterInFragmenter, delimiter, colTypes, skipHeader);

        return hiveUserData.toString().getBytes();
    }

    /**
     * The method parses raw user data into HiveUserData class
     *
     * @param context input data
     * @return instance of HiveUserData class
     * @throws IllegalArgumentException when incorrect number of tokens in Hive user data received
     */
    public static HiveUserData parseHiveUserData(RequestContext context) throws IllegalArgumentException {
        String userData = new String(context.getFragmentUserData());
        String[] toks = userData.split(HiveUserData.HIVE_UD_DELIM, HiveUserData.getNumOfTokens());

        if (toks.length != (HiveUserData.getNumOfTokens())) {
            throw new IllegalArgumentException("HiveInputFormatFragmenter expected "
                    + HiveUserData.getNumOfTokens() + " tokens, but got " + toks.length);
        }

        HiveUserData hiveUserData = new HiveUserData(toks[0], toks[1], toks[2], toks[3], Boolean.valueOf(toks[4]), toks[5], toks[6], Integer.parseInt(toks[7]));

        return hiveUserData;
    }

    private static String getSerdeParameter(StorageDescriptor sd, String parameterKey) {
        String parameterValue = null;
        if (sd != null && sd.getSerdeInfo() != null && sd.getSerdeInfo().getParameters() != null && sd.getSerdeInfo().getParameters().get(parameterKey) != null) {
            parameterValue = sd.getSerdeInfo().getParameters().get(parameterKey);
        }

        return parameterValue;
    }

    /**
     * The method which extracts field delimiter from storage descriptor.
     * When unable to extract delimiter from storage descriptor, default value is used
     *
     * @param sd StorageDescriptor of table/partition
     * @return ASCII code of delimiter
     */
    public static Integer getDelimiterCode(StorageDescriptor sd) {
        Integer delimiterCode = null;

        String delimiter = getSerdeParameter(sd, serdeConstants.FIELD_DELIM);
        if (delimiter != null) {
            delimiterCode = (int) delimiter.charAt(0);
            return delimiterCode;
        }

        delimiter = getSerdeParameter(sd, serdeConstants.SERIALIZATION_FORMAT);
        if (delimiter != null) {
            delimiterCode = Integer.parseInt(delimiter);
            return delimiterCode;
        }

        return DEFAULT_DELIMITER_CODE;
    }

    /**
     * The method determines whether metadata definition has any complex type
     * @see EnumHiveToGpdbType for complex type attribute definition
     *
     * @param metadata metadata of relation
     * @return true if metadata has at least one field of complex type
     */
    public static boolean hasComplexTypes(Metadata metadata) {
        boolean hasComplexTypes = false;
        List<Field> fields = metadata.getFields();
        for (Field field : fields) {
            if (field.isComplexType()) {
                hasComplexTypes = true;
                break;
            }
        }

        return hasComplexTypes;
    }

    /**
     * Populates the given metadata object with the given table's fields and partitions,
     * The partition fields are added at the end of the table schema.
     * Throws an exception if the table contains unsupported field types.
     * Supported HCatalog types: TINYINT,
     * SMALLINT, INT, BIGINT, BOOLEAN, FLOAT, DOUBLE, STRING, BINARY, TIMESTAMP,
     * DATE, DECIMAL, VARCHAR, CHAR.
     *
     * @param tbl      Hive table
     * @param metadata schema of given table
     */
    public static void getSchema(Table tbl, Metadata metadata) {

        int hiveColumnsSize = tbl.getSd().getColsSize();
        int hivePartitionsSize = tbl.getPartitionKeysSize();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Hive table: " + hiveColumnsSize + " fields, " + hivePartitionsSize + " partitions.");
        }

        // check hive fields
        try {
            List<FieldSchema> hiveColumns = tbl.getSd().getCols();
            for (FieldSchema hiveCol : hiveColumns) {
                metadata.addField(HiveUtilities.mapHiveType(hiveCol));
            }
            // check partition fields
            List<FieldSchema> hivePartitions = tbl.getPartitionKeys();
            for (FieldSchema hivePart : hivePartitions) {
                metadata.addField(HiveUtilities.mapHiveType(hivePart));
            }
        } catch (UnsupportedTypeException e) {
            String errorMsg = "Failed to retrieve metadata for table " + metadata.getItem() + ". " +
                    e.getMessage();
            throw new UnsupportedTypeException(errorMsg);
        }
    }

    /**
     * Creates an instance of a given serde type
     *
     * @param serdeClassName the name of the serde class
     * @return instance of a given serde
     * @throws Exception if an error occurs during the creation of SerDe instance
     */
    @SuppressWarnings("deprecation")
    public static SerDe createDeserializer(String serdeClassName) throws Exception {
        SerDe deserializer = (SerDe) Utilities.createAnyInstance(serdeClassName);
        return deserializer;
    }

    /**
     * Creates ORC file reader.
     * @param requestContext input data with given data source
     * @return ORC file reader
     */
    public static Reader getOrcReader(Configuration configuration, RequestContext requestContext) {
        try {
            Path path = new Path(requestContext.getDataSource());
            return OrcFile.createReader(path.getFileSystem(configuration), path);
        } catch (Exception e) {
            throw new RuntimeException("Exception while getting orc reader", e);
        }
    }

    /**
     * Uses {@link HiveFilterBuilder} to translate a filter string into a
     * Hive {@link SearchArgument} object. The result is added as a filter to
     * JobConf object
     */
    public static SearchArgument buildSearchArgument(RequestContext context) throws Exception {

        if (!context.hasFilter()) {
            return null;
        }

        /* Predicate pushdown configuration */
        String filterStr = context.getFilterString();

        HiveFilterBuilder eval = new HiveFilterBuilder();
        Object filter = eval.getFilterObject(filterStr);
        SearchArgument.Builder filterBuilder = SearchArgumentFactory.newBuilder();

        /*
         * If there is only a single filter it will be of type Basic Filter
         * need special case logic to make sure to still wrap the filter in a
         * startAnd() & end() block
         */
        if (filter instanceof LogicalFilter) {
            if (!buildExpression(context, filterBuilder, Arrays.asList(filter))) {
                return null;
            }
        }
        else {
            filterBuilder.startAnd();
            if(!buildArgument(context, filterBuilder, filter)) {
                return null;
            }
            filterBuilder.end();
        }
        return filterBuilder.build();
    }


    private static boolean buildExpression(RequestContext context, SearchArgument.Builder builder, List<Object> filterList) {
        for (Object f : filterList) {
            if (f instanceof LogicalFilter) {
                switch(((LogicalFilter) f).getOperator()) {
                    case HDOP_OR:
                        builder.startOr();
                        break;
                    case HDOP_AND:
                        builder.startAnd();
                        break;
                    case HDOP_NOT:
                        builder.startNot();
                        break;
                }
                if (buildExpression(context, builder, ((LogicalFilter) f).getFilterList())) {
                    builder.end();
                } else {
                    return false;
                }
            } else {
                if (!buildArgument(context, builder, f)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean buildArgument(RequestContext context, SearchArgument.Builder builder, Object filterObj) {
        /* The below functions will not be compatible and requires update  with Hive 2.0 APIs */
        BasicFilter filter = (BasicFilter) filterObj;
        int filterColumnIndex = filter.getColumn().index();
        // filter value might be null for unary operations
        Object filterValue = filter.getConstant() == null ? null : filter.getConstant().constant();
        ColumnDescriptor filterColumn = context.getColumn(filterColumnIndex);
        String filterColumnName = filterColumn.columnName();

        /* Need to convert java.sql.Date to Hive's DateWritable Format */
        if (filterValue instanceof Date)
            filterValue= new DateWritable((Date) filterValue);

        switch(filter.getOperation()) {
            case HDOP_LT:
                builder.lessThan(filterColumnName, filterValue);
                break;
            case HDOP_GT:
                builder.startNot().lessThanEquals(filterColumnName, filterValue).end();
                break;
            case HDOP_LE:
                builder.lessThanEquals(filterColumnName, filterValue);
                break;
            case HDOP_GE:
                builder.startNot().lessThanEquals(filterColumnName, filterValue).end();
                break;
            case HDOP_EQ:
                builder.equals(filterColumnName, filterValue);
                break;
            case HDOP_NE:
                builder.startNot().equals(filterColumnName, filterValue).end();
                break;
            case HDOP_IS_NULL:
                builder.isNull(filterColumnName);
                break;
            case HDOP_IS_NOT_NULL:
                builder.startNot().isNull(filterColumnName).end();
                break;
            case HDOP_IN:
                if (filterValue instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>)filterValue;
                    builder.in(filterColumnName, l.toArray());
                } else {
                    throw new IllegalArgumentException("filterValue should be instace of List for HDOP_IN operation");
                }
                break;
            default: {
                LOG.debug("Filter push-down is not supported for " + filter.getOperation() + "operation.");
                return false;
            }
        }
        return true;
    }
}
