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
package com.uber.cadence.entities;

import java.util.Map;
import lombok.Data;

/**
 * GetTaskListsByDomainResponse contains the list of all task lists for a domain. It includes both
 * decision task lists and activity task lists.
 */
@Data
public class GetTaskListsByDomainResponse {
  private Map<String, DescribeTaskListResponse> decisionTaskListMap;
  private Map<String, DescribeTaskListResponse> activityTaskListMap;
}
