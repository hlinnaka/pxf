package org.greenplum.pxf.plugins.hdfs;

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

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * Just an output buffer for the ChunkRecordReader. It must extend Writable
 * otherwise it will not fit into the next() interface method
 */
public class ChunkWritable implements Writable {

	byte [] box;

	/**
     * Serializes the fields of this object to <code>out</code>.
     *
     * @param out <code>DataOutput</code> to serialize this object into.
     * @throws UnsupportedOperationException this function is not supported
     */
	@Override
    public void write(DataOutput out)  {
		throw new UnsupportedOperationException("ChunkWritable.write() is not implemented");
    }

    /**
     * Deserializes the fields of this object from <code>in</code>.
     * <p>For efficiency, implementations should attempt to re-use storage in the
     * existing object where possible.</p>
     *
     * @param in <code>DataInput</code> to deserialize this object from.
     * @throws UnsupportedOperationException  this function is not supported
     */
	@Override
    public void readFields(DataInput in)  {
		throw new UnsupportedOperationException("ChunkWritable.readFields() is not implemented");
	}
}
