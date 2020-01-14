# Changelog

## 5.10.1 (01/07/2020)

#### Enhancements:

- [#276](https://github.com/greenplum-db/pxf/pull/276) Refactor filter parser code (Non-user facing)

#### Bug Fixes:

- [#283](https://github.com/greenplum-db/pxf/pull/283) Fix parquet write decimal
- [#280](https://github.com/greenplum-db/pxf/pull/280) Introduced pxf.session.user property and JDBC conn pool qualifier

---

## 5.10.0 (12/02/2019)

#### Enhancements:

- [#248](https://github.com/greenplum-db/pxf/pull/268) Enable Avro write
- [#261](https://github.com/greenplum-db/pxf/pull/261) Add Support for impersonation per server
- [#257](https://github.com/greenplum-db/pxf/pull/257) Add Support for Kerberized Hive 3
- [#254](https://github.com/greenplum-db/pxf/pull/254) Merge pxf impersonation jdbc
- [#247](https://github.com/greenplum-db/pxf/pull/247) Add support for multiple kerberos Hadoop and Hive servers

#### Bug Fixes:

- [#268](https://github.com/greenplum-db/pxf/pull/268) Fallback on using LineRecordReader when reading encrypted files

---

## 5.9.2 (10/28/2019)

#### Bug Fixes:

- [#253](https://github.com/greenplum-db/pxf/pull/253) Improve performance of HdfsFileFragmenter

---

## 5.9.1 (10/15/2019)

#### Bug Fixes:

- [#251](https://github.com/greenplum-db/pxf/pull/251) Fix regression in *:text:multi profiles when using wildcards

---

## 5.9.0 (09/25/2019)

#### Enhancements:

- [#243](https://github.com/greenplum-db/pxf/pull/243) Upgrade jackson libraries to 2.9.10
- [#235](https://github.com/greenplum-db/pxf/pull/235) Certify support for Hive 3.1
- [#226](https://github.com/greenplum-db/pxf/pull/226) Add OR and NOT support for JDBC filter pushdown
- [#236](https://github.com/greenplum-db/pxf/pull/236) Upgrade tomcat to version 7.0.96
- [#230](https://github.com/greenplum-db/pxf/pull/230) Add support for Hive 2 (Up to Hive 2.3.6)

#### Notes:

- PXF does not support Hive when running Java 11. As a workaround run PXF on Java 8.

---

## 5.8.2 (09/08/2019)

#### Bug Fixes:

- [#228](https://github.com/greenplum-db/pxf/pull/228) Make JDBC profile not fail when MAPR JAR files override default Hadoop ones
- [#217](https://github.com/greenplum-db/pxf/pull/217) CLI: reset on standby master and don't allow cluster init without PXF_CONF set #217
- [#224](https://github.com/greenplum-db/pxf/pull/224) Fix cloud access when Kerberized Hadoop is present

---

## 5.8.1 (08/22/2019)

#### Enhancements:

- Enable multinode testing against GCP dataproc. Run automation tests against Hadoop 2.9.2 and Hive 2.3.5

---

## 5.8.0 (08/01/2019)

#### Bug Fixes:

- [#211](https://github.com/greenplum-db/pxf/pull/211) Preserve error when re-throwing IOException (#211)

#### Enhancements:

- [#187](https://github.com/greenplum-db/pxf/pull/187) Implement S3 Select
- [#189](https://github.com/greenplum-db/pxf/pull/189) Implement cluster reset command
- [#191](https://github.com/greenplum-db/pxf/pull/191) Support config option to specify the server configuration directory (#191)
- [#198](https://github.com/greenplum-db/pxf/pull/198) Support serializing a list of OneFields to CSV (#198)
- [#201](https://github.com/greenplum-db/pxf/pull/201) Add JDK11 to PXF docker base dev image (#201)
- [#202](https://github.com/greenplum-db/pxf/pull/202) Support NOT and OR operators for S3 Select
- [#203](https://github.com/greenplum-db/pxf/pull/203) Support reading and writing of timestamp with time zone for Parquet (#203)
- [#206](https://github.com/greenplum-db/pxf/pull/206) Add S3 Select support for Parquet using S3-SELECT=AUTO (#206)
- [#207](https://github.com/greenplum-db/pxf/pull/207) Enable support for PXF server to run with Java-11 (#207)
- [#212](https://github.com/greenplum-db/pxf/pull/212) Use format options for S3 Select (#212)
- [#213](https://github.com/greenplum-db/pxf/pull/213) Rename S3-SELECT option -> S3_SELECT (#213)

---

## 5.7.0 (07/08/2019)

#### Bug Fixes:

- [#193](https://github.com/greenplum-db/pxf/pull/193) Fix uncompressed write parquet (#193)
- [#170](https://github.com/greenplum-db/pxf/pull/170) JDBC: Query data from ranges outside of partition range (#170)
- [#192](https://github.com/greenplum-db/pxf/pull/192) JsonResolver: throw BadRecordException on bad JSON

#### Enhancements:

- [#196](https://github.com/greenplum-db/pxf/pull/196) Purge codehaus from codebase, and replace it with fasterXML library (#196)

---

## 5.6.0 (06/19/2019)

#### Bug Fixes:

- [#182](https://github.com/greenplum-db/pxf/pull/182) Run a named query that ends with semicolon

#### Enhancements:

- [#188](https://github.com/greenplum-db/pxf/pull/188) Support for FDW
- [#186](https://github.com/greenplum-db/pxf/pull/186) Enable JDBC Connection Pooling
- [#183](https://github.com/greenplum-db/pxf/pull/183) Upgrade postgres driver to version 42.2.5

---

## 5.5.1 (06/03/2019)

#### Bug Fixes:

- [#180](https://github.com/greenplum-db/pxf/pull/180) Upgrade the Postgres JDBC Driver version

---

## 5.5.0 (05/31/2019)

#### Bug Fixes:

- [#176](https://github.com/greenplum-db/pxf/pull/176) Upgrade jackson 2 version from 2.9.8 -> 2.9.9

#### Enhancements:

- [#171](https://github.com/greenplum-db/pxf/pull/171) Enable JDBC connection to Hive and JDBC-specific user impersonation per server
- [#178](https://github.com/greenplum-db/pxf/pull/178) Make pxf threads configurable
- [#172](https://github.com/greenplum-db/pxf/pull/172) Support user-specific configuration in server
- [#169](https://github.com/greenplum-db/pxf/pull/169) HdfsFileFragmenter
- [#162](https://github.com/greenplum-db/pxf/pull/162) Kill JVM/Tomcat on OutOfMemoryError
- [#160](https://github.com/greenplum-db/pxf/pull/160) JDBC: Optimize resolver for INSERT queries

---

## 5.4.0 (05/13/2019)

#### Bug Fixes:

- [#161](https://github.com/greenplum-db/pxf/pull/161) Fix Oracle timestamp wrapping

#### Enhancements:

- [#157](https://github.com/greenplum-db/pxf/pull/157) JDBC profile can execute a query to read data from an external DB
- [#158](https://github.com/greenplum-db/pxf/pull/158) Add support to read multiline files as a single row

---

## 5.3.2 (05/06/2019)

#### Bug Fixes:

- [#156](https://github.com/greenplum-db/pxf/pull/156) JDBC statement properties including fetch size and timeout

---

## 5.3.1 (04/27/2019)

#### Bug Fixes:

- [#151](https://github.com/greenplum-db/pxf/pull/151) PXF cli: fix regression with version command

#### Enhancements:

- [#152](https://github.com/greenplum-db/pxf/pull/152) Ensure pxf version can be run before pxf init

---

## 5.3.0 (04/26/2019)

#### Bug Fixes:

- [#147](https://github.com/greenplum-db/pxf/pull/147) Reverse direction of rsync in pxf sync command
- [#144](https://github.com/greenplum-db/pxf/pull/144) Remove support for Logical operator NOT with Hive Partition Filtering. NOT is an unsupported logical operator
- [#138](https://github.com/greenplum-db/pxf/pull/138) Hive partition filtering with support for all Logical Operators
- [#134](https://github.com/greenplum-db/pxf/pull/134) pxf cluster: stop checking that hostname is master

#### Enhancements:

- [#150](https://github.com/greenplum-db/pxf/pull/150) Add debug statements for the JDBC connection
- [#149](https://github.com/greenplum-db/pxf/pull/149) pxf cli: Add cluster status command
- [#142](https://github.com/greenplum-db/pxf/pull/142) Allow configuration of JDBC transaction isolation. Implements [#130](https://github.com/greenplum-db/pxf/issues/130)
- [#145](https://github.com/greenplum-db/pxf/pull/145) Added integration test for JDBC session parameters
- [#135](https://github.com/greenplum-db/pxf/pull/135) Cache Fragmenter calls to improve memory consumption during the fragmenter call
- [#136](https://github.com/greenplum-db/pxf/pull/136) pxf-cli: Support sync and init on standby master
- [#141](https://github.com/greenplum-db/pxf/pull/141) pxf-api: Fix BaseConfigurationFactory logging
- [#118](https://github.com/greenplum-db/pxf/pull/118) PXF-JDBC: Enable external database configuration and connection settings modification. Implements [#129](https://github.com/greenplum-db/pxf/issues/129)
- [#133](https://github.com/greenplum-db/pxf/pull/133) Add Changelog
- pxf-cli: Use rsync on master host

---

## 5.2.1 (04/04/2019)

#### Enhancements:

- [#115](https://github.com/greenplum-db/pxf/pull/115)
  PXF no longer expects the path to contain
  transaction and segment IDs during write. PXF will
  now construct the write path for Hadoop-Compatible
  FileSystems to include transaction and segment IDs. 

---

## 5.2.0 (04/04/2019)

#### Enhancements:

- [#119](https://github.com/greenplum-db/pxf/pull/119) Remove PXF-Ignite plugin. The Ignite plugin is
  removed in favor of Ignite's JDBC driver.
- Adds more visibility to external contributors by exposing
  Pull Request pipelines. It allows external contributors
  to debug issues when submitting Pull Requests.
  
#### Bug Fixes:

- [#126](https://github.com/greenplum-db/pxf/pull/126) PXF Port Fix. Fixes issue in PXF when setting
  PXF_PORT and then starting PXF. Fixes issue [#93](https://github.com/greenplum-db/pxf/issues/93)

---

## 5.1.1 (03/26/2019)

#### Bug Fixes:

- [#116](https://github.com/greenplum-db/pxf/pull/116) Always use doAs for Kerberos with Hive, add
  request's hive-site.xml to HiveConf explicitly. Fixes
  issues with Kerberized Hive, where UGI was not being set.

---

## 5.1.0 (03/26/2019)

#### Bug Fixes:

- [#80](https://github.com/greenplum-db/pxf/pull/80) Throw IOException when fs.mkdirs() returns false

#### Enhancements:

- Improve Documentation
- [#114](https://github.com/greenplum-db/pxf/pull/114) enable file-based configuration for JDBC plugin
- [#113](https://github.com/greenplum-db/pxf/pull/113) added PARQUET_VERSION parameter and tests
- [#112](https://github.com/greenplum-db/pxf/pull/112) Support additional parquet write config options
- [#111](https://github.com/greenplum-db/pxf/pull/111) Fixed propagation of write exception from the JDBC plugin
- [#110](https://github.com/greenplum-db/pxf/pull/110) Parquet column projection
- [#108](https://github.com/greenplum-db/pxf/pull/108) Enabled column projection pushdown for JDBC profile
- [#101](https://github.com/greenplum-db/pxf/pull/101) Update logging configuration to limit Hadoop INFO logging
- Add descriptive message when JAVA_HOME is not set
- [#98](https://github.com/greenplum-db/pxf/pull/98) PXF-JDBC: quote column names
- [#95](https://github.com/greenplum-db/pxf/pull/95) Enable license generation for PXF
- [#94](https://github.com/greenplum-db/pxf/pull/94) Column projection support changes
- [#92](https://github.com/greenplum-db/pxf/pull/92) Enhanced unit test for repeated primitive Parquet types
- [#91](https://github.com/greenplum-db/pxf/pull/91) Create new groups for hive and hbase tests
- [#89](https://github.com/greenplum-db/pxf/pull/89) Implement optimized version of isTextForm
- [#88](https://github.com/greenplum-db/pxf/pull/88) Support Parquet repeated primitive types serialized into JSON
- [#87](https://github.com/greenplum-db/pxf/pull/87) Updated library versions with security issues
- [#86](https://github.com/greenplum-db/pxf/pull/86) Remove Parquet fragmenter; defer schema read to accessor
- Performance Tests
- [#81](https://github.com/greenplum-db/pxf/pull/81) Upgrade to hadoop version 2.9.2
- [#77](https://github.com/greenplum-db/pxf/pull/77) Add MapR Support for HDFS

---

## 5.0.1 (01/15/2019)
*No changelog for this release.*

---

## 5.0.0 (01/14/2019)
*Changelog needed here.*

---

## 4.1.0 (12/04/2018)
*Changelog needed here.*

---

## 4.0.3 (10/16/2018)
*Changelog needed here.*

---

## 4.0.2 (10/12/2018)
*Changelog needed here.*

---

## 4.0.1 (10/12/2018)
*Changelog needed here.*

---

## 4.0.0 (10/11/2018)
*Changelog needed here.*

---
