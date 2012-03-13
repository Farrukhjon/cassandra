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
package org.apache.cassandra.cql3.statements;

import java.io.IOException;

import org.apache.cassandra.cql3.*;
import org.apache.cassandra.config.*;
import org.apache.cassandra.db.migration.Migration;
import org.apache.cassandra.db.migration.UpdateColumnFamily;
import org.apache.cassandra.thrift.CfDef;
import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.InvalidRequestException;

public class DropIndexStatement extends SchemaAlteringStatement
{
    public final CharSequence index;

    public DropIndexStatement(String indexName)
    {
        super(new CFName());
        index = indexName;
    }

    public Migration getMigration() throws InvalidRequestException, ConfigurationException, IOException
    {
        CfDef cfDef = null;

        KSMetaData ksm = Schema.instance.getTableDefinition(keyspace());

        for (CFMetaData cfm : ksm.cfMetaData().values())
        {
            cfDef = getUpdatedCFDef(cfm.toThrift());
            if (cfDef != null)
                break;
        }

        if (cfDef == null)
            throw new InvalidRequestException("Index '" + index + "' could not be found in any of the column families of keyspace '" + keyspace() + "'");

        return new UpdateColumnFamily(CFMetaData.fromThrift(cfDef));
    }

    private CfDef getUpdatedCFDef(CfDef cfDef) throws InvalidRequestException
    {
        for (ColumnDef column : cfDef.column_metadata)
        {
            if (column.index_type != null && column.index_name != null && column.index_name.equals(index))
            {
                column.index_name = null;
                column.index_type = null;
                return cfDef;
            }
        }

        return null;
    }
}
