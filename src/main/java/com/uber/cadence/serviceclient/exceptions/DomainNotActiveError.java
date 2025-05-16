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

package com.uber.cadence.serviceclient.exceptions;

public class DomainNotActiveError extends ServiceClientError {
  private final String domain;
  private final String currentCluster;
  private final String activeCluster;

  public DomainNotActiveError(String domain, String currentCluster, String activeCluster) {
    this.domain = domain;
    this.currentCluster = currentCluster;
    this.activeCluster = activeCluster;
  }

  public String getDomain() {
    return domain;
  }

  public String getCurrentCluster() {
    return currentCluster;
  }

  public String getActiveCluster() {
    return activeCluster;
  }
}
