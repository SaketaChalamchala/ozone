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

package org.apache.hadoop.ozone.om.snapshot;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport.DiffReportEntry;
import org.apache.hadoop.hdfs.protocol.SnapshotDiffReport.DiffType;

/**
 * Metadata for a classified snapshot diff entry used to build the dependency
 * graph for dependency-ordered report emission.
 */
public final class SnapDiffDependencyEntry {

  private final long objectId;
  private final long parentObjectId;
  private final DiffReportEntry reportEntry;

  public SnapDiffDependencyEntry(long objectId, long parentObjectId,
      DiffReportEntry reportEntry) {
    this.objectId = objectId;
    this.parentObjectId = parentObjectId;
    this.reportEntry = Objects.requireNonNull(reportEntry, "reportEntry");
  }

  public long getObjectId() {
    return objectId;
  }

  public long getParentObjectId() {
    return parentObjectId;
  }

  public DiffReportEntry getReportEntry() {
    return reportEntry;
  }

  public DiffType getDiffType() {
    return reportEntry.getType();
  }

  public String getSourcePath() {
    return new String(reportEntry.getSourcePath(), StandardCharsets.UTF_8);
  }

  public String getTargetPath() {
    if (reportEntry.getTargetPath() == null) {
      return null;
    }
    return new String(reportEntry.getTargetPath(), StandardCharsets.UTF_8);
  }

  public boolean isDelete() {
    return getDiffType() == DiffType.DELETE;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SnapDiffDependencyEntry)) {
      return false;
    }
    SnapDiffDependencyEntry that = (SnapDiffDependencyEntry) other;
    return objectId == that.objectId
        && parentObjectId == that.parentObjectId
        && reportEntry.equals(that.reportEntry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectId, parentObjectId, reportEntry);
  }
}
