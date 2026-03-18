/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.workflow;

/**
 * Options for controlling version selection behavior in {@link Workflow#getVersion}.
 *
 * <p>When no cached version exists for a changeID, these options control which version is recorded:
 *
 * <ul>
 *   <li>{@link #executeWithVersion(int)} — Forces a specific version to be used.
 *   <li>{@link #executeWithMinVersion()} — Forces {@code minSupported} to be used.
 *   <li>Default (no options) — Uses {@code maxSupported}.
 * </ul>
 *
 * <p>If a version is already cached for the changeID, options are ignored and the cached version is
 * returned.
 */
public final class GetVersionOptions {
  private final Integer customVersion; // null = not set; boxed to distinguish "absent" from 0
  private final boolean useMinVersion;

  private GetVersionOptions(Integer customVersion, boolean useMinVersion) {
    this.customVersion = customVersion;
    this.useMinVersion = useMinVersion;
  }

  /** Returns the custom version to use, or {@code null} if not set. */
  public Integer getCustomVersion() {
    return customVersion;
  }

  /** Returns {@code true} if minSupported should be used instead of maxSupported. */
  public boolean isUseMinVersion() {
    return useMinVersion;
  }

  /**
   * Creates options that force execution with a specific version.
   *
   * @param version the version to use; must not be DEFAULT_VERSION (-1)
   * @throws IllegalArgumentException if version is DEFAULT_VERSION (-1)
   */
  public static GetVersionOptions executeWithVersion(int version) {
    return new Builder().setCustomVersion(version).build();
  }

  /** Creates options that force execution with minSupported version. */
  public static GetVersionOptions executeWithMinVersion() {
    return new Builder().setUseMinVersion(true).build();
  }

  @Override
  public String toString() {
    return "GetVersionOptions{customVersion="
        + customVersion
        + ", useMinVersion="
        + useMinVersion
        + "}";
  }

  public static final class Builder {
    private Integer customVersion;
    private boolean useMinVersion;

    /**
     * Sets a specific version to use when recording a new version marker.
     *
     * @param version the version to use; must be non-negative
     * @throws IllegalArgumentException if version is negative
     */
    public Builder setCustomVersion(int version) {
      if (version < 0) {
        throw new IllegalArgumentException("customVersion must be non-negative, got: " + version);
      }
      this.customVersion = version;
      return this;
    }

    /** Sets whether to use minSupported instead of maxSupported. */
    public Builder setUseMinVersion(boolean useMinVersion) {
      this.useMinVersion = useMinVersion;
      return this;
    }

    /**
     * Builds the options.
     *
     * @throws IllegalArgumentException if both customVersion and useMinVersion are set
     */
    public GetVersionOptions build() {
      if (customVersion != null && useMinVersion) {
        throw new IllegalArgumentException("Cannot set both customVersion and useMinVersion");
      }
      return new GetVersionOptions(customVersion, useMinVersion);
    }
  }
}
