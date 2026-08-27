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

package com.uber.cadence.internal.compatibility.proto.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.uber.cadence.api.v1.ActiveClusterSelectionPolicy;
import com.uber.cadence.api.v1.ClusterAttribute;
import com.uber.cadence.api.v1.CronOverlapPolicy;
import com.uber.cadence.api.v1.WorkflowExecutionStartedEventAttributes;
import com.uber.cadence.api.v1.WorkflowType;
import org.junit.Test;

public class HistoryMapperTest {

  @Test
  public void workflowExecutionStartedEventAttributesMapsActiveClusterSelectionPolicy() {
    WorkflowExecutionStartedEventAttributes proto =
        WorkflowExecutionStartedEventAttributes.newBuilder()
            .setWorkflowType(WorkflowType.newBuilder().setName("wf").build())
            .setActiveClusterSelectionPolicy(
                ActiveClusterSelectionPolicy.newBuilder()
                    .setClusterAttribute(
                        ClusterAttribute.newBuilder()
                            .setScope("location")
                            .setName("lisbon")
                            .build())
                    .build())
            .build();

    com.uber.cadence.WorkflowExecutionStartedEventAttributes result =
        HistoryMapper.workflowExecutionStartedEventAttributes(proto);

    assertEquals(
        "location", result.getActiveClusterSelectionPolicy().getClusterAttribute().getScope());
    assertEquals(
        "lisbon", result.getActiveClusterSelectionPolicy().getClusterAttribute().getName());
  }

  @Test
  public void workflowExecutionStartedEventAttributesMapsCronOverlapPolicy() {
    WorkflowExecutionStartedEventAttributes proto =
        WorkflowExecutionStartedEventAttributes.newBuilder()
            .setWorkflowType(WorkflowType.newBuilder().setName("wf").build())
            .setCronOverlapPolicy(CronOverlapPolicy.CRON_OVERLAP_POLICY_BUFFER_ONE)
            .build();

    com.uber.cadence.WorkflowExecutionStartedEventAttributes result =
        HistoryMapper.workflowExecutionStartedEventAttributes(proto);

    assertEquals(com.uber.cadence.CronOverlapPolicy.BUFFERONE, result.getCronOverlapPolicy());
  }

  @Test
  public void workflowExecutionStartedEventAttributesLeavesAbsentFieldsNull() {
    WorkflowExecutionStartedEventAttributes proto =
        WorkflowExecutionStartedEventAttributes.newBuilder()
            .setWorkflowType(WorkflowType.newBuilder().setName("wf").build())
            .build();

    com.uber.cadence.WorkflowExecutionStartedEventAttributes result =
        HistoryMapper.workflowExecutionStartedEventAttributes(proto);

    assertNull(result.getActiveClusterSelectionPolicy());
    assertNull(result.getCronOverlapPolicy());
  }
}
