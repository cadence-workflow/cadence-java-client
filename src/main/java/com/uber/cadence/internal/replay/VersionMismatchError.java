/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.uber.cadence.internal.replay;

/**
 * Thrown by {@code Workflow.getVersion} when the recorded version is outside the range the workflow
 * code supports. It is a separate type so that decision handling can tell it apart from other
 * errors: it fails the decision task so it is retried, but never fails the workflow and is not
 * reported as a decision execution failure.
 */
public final class VersionMismatchError extends Error {

  /** Why the version was rejected. */
  public enum Type {
    /**
     * The version is newer than this worker's code supports. This is expected during a rolling
     * deployment, when an old worker replays a workflow that a newer worker has already moved to a
     * higher version. It resolves itself once an upgraded worker picks up the decision task.
     */
    TOO_NEW("toonew"),
    /**
     * The workflow code dropped support for a version that is still recorded in a running workflow.
     * This is a bug in the workflow code.
     */
    TOO_OLD("tooold");

    private final String tagValue;

    Type(String tagValue) {
      this.tagValue = tagValue;
    }

    /** Value used for the {@code VersionMismatchType} metric and log tag. */
    public String getTagValue() {
      return tagValue;
    }
  }

  private final Type type;
  private final String changeId;
  private final int version;
  private final int minSupported;
  private final int maxSupported;

  public VersionMismatchError(
      Type type, String changeId, int version, int minSupported, int maxSupported) {
    // Same message as before this type existed, so existing log searches keep working.
    super(
        String.format(
            "Version %d of changeID %s is not supported. Supported version is between %d and %d.",
            version, changeId, minSupported, maxSupported));
    this.type = type;
    this.changeId = changeId;
    this.version = version;
    this.minSupported = minSupported;
    this.maxSupported = maxSupported;
  }

  public Type getType() {
    return type;
  }

  public String getChangeId() {
    return changeId;
  }

  public int getVersion() {
    return version;
  }

  public int getMinSupported() {
    return minSupported;
  }

  public int getMaxSupported() {
    return maxSupported;
  }
}
