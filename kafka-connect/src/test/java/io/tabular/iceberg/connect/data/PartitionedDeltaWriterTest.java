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
package io.tabular.iceberg.connect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.tabular.iceberg.connect.IcebergSinkConfig;
import io.tabular.iceberg.connect.TableSinkConfig;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PartitionedDeltaWriterTest extends BaseWriterTest {

  @ParameterizedTest
  @ValueSource(strings = {"parquet", "orc"})
  public void testPartitionedDeltaWriter(String format) {
    IcebergSinkConfig config = mock(IcebergSinkConfig.class);
    when(config.upsertModeEnabled()).thenReturn(true);
    when(config.tableConfig(any())).thenReturn(mock(TableSinkConfig.class));
    when(config.writeProps()).thenReturn(ImmutableMap.of("write.format.default", format));

    when(table.spec()).thenReturn(SPEC);

    Record row1 = GenericRecord.create(SCHEMA);
    row1.setField("id", 123L);
    row1.setField("data", "hello world!");
    row1.setField("id2", 123L);

    Record row2 = GenericRecord.create(SCHEMA);
    row2.setField("id", 234L);
    row2.setField("data", "foobar");
    row2.setField("id2", 234L);

    WriteResult result =
        writeTest(ImmutableList.of(row1, row2), config, PartitionedDeltaWriter.class);

    // in upsert mode, each write is a delete + append, so we'll have 1 data file
    // and 1 delete file for each partition (2 total)
    assertThat(result.dataFiles()).hasSize(2);
    assertThat(result.dataFiles()).allMatch(file -> file.format() == FileFormat.fromString(format));
    assertThat(result.deleteFiles()).hasSize(2);
    assertThat(result.deleteFiles())
        .allMatch(file -> file.format() == FileFormat.fromString(format));
  }
}
