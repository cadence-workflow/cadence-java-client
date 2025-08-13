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

package com.uber.cadence.workflow;

import java.util.Optional;

/**
 * Options for configuring GetVersion behavior. This class provides a builder pattern for
 * configuring version control options.
 *
 * <p>Example usage:
 *
 * <pre><code>
 * // Force a specific version
 * GetVersionOptions options = GetVersionOptions.newBuilder()
 *     .executeWithVersion(2)
 *     .build();
 *
 * // Use minimum supported version
 * GetVersionOptions options = GetVersionOptions.newBuilder()
 *     .executeWithMinVersion()
 *     .build();
 * </code></pre>
 */
public final class GetVersionOptions {
  private final Optional<Integer> customVersion;
  private final boolean useMinVersion;

  private GetVersionOptions(Optional<Integer> customVersion, boolean useMinVersion) {
    this.customVersion = customVersion;
    this.useMinVersion = useMinVersion;
  }

  /** Returns the custom version if specified, otherwise empty. */
  public Optional<Integer> getCustomVersion() {
    return customVersion;
  }

  /** Returns true if the minimum version should be used instead of maximum version. */
  public boolean isUseMinVersion() {
    return useMinVersion;
  }

  /** Creates a new builder for GetVersionOptions. */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Builder for GetVersionOptions. */
  public static class Builder {
    private Optional<Integer> customVersion = Optional.empty();
    private boolean useMinVersion = false;

    /**
     * Forces a specific version to be returned when executed for the first time, instead of
     * returning maxSupported version.
     *
     * @param version the specific version to use
     * @return this builder
     */
    public Builder executeWithVersion(int version) {
      this.customVersion = Optional.of(version);
      return this;
    }

    /**
     * Makes GetVersion return minSupported version when executed for the first time, instead of
     * returning maxSupported version.
     *
     * @return this builder
     */
    public Builder executeWithMinVersion() {
      this.useMinVersion = true;
      return this;
    }

    /**
     * Builds the GetVersionOptions instance.
     *
     * @return the configured GetVersionOptions
     */
    public GetVersionOptions build() {
      return new GetVersionOptions(customVersion, useMinVersion);
    }
  }
}
