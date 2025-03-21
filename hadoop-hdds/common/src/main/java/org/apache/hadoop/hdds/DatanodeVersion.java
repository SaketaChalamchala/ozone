/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdds;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Map;

/**
 * Versioning for datanode.
 */
public enum DatanodeVersion implements ComponentVersion {

  DEFAULT_VERSION(0, "Initial version"),

  SEPARATE_RATIS_PORTS_AVAILABLE(1, "Version with separated Ratis port."),
  COMBINED_PUTBLOCK_WRITECHUNK_RPC(2, "WriteChunk can optionally support " +
          "a PutBlock request"),

  FUTURE_VERSION(-1, "Used internally in the client when the server side is "
      + " newer and an unknown server version has arrived to the client.");

  public static final DatanodeVersion CURRENT = latest();
  public static final int CURRENT_VERSION = CURRENT.version;

  private static final Map<Integer, DatanodeVersion> BY_PROTO_VALUE =
      Arrays.stream(values())
          .collect(toMap(DatanodeVersion::toProtoValue, identity()));

  private final int version;
  private final String description;

  DatanodeVersion(int version, String description) {
    this.version = version;
    this.description = description;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public int toProtoValue() {
    return version;
  }

  public static DatanodeVersion fromProtoValue(int value) {
    return BY_PROTO_VALUE.getOrDefault(value, FUTURE_VERSION);
  }

  private static DatanodeVersion latest() {
    DatanodeVersion[] versions = DatanodeVersion.values();
    return versions[versions.length - 2];
  }
}
