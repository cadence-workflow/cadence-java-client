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
import static org.junit.Assert.assertNotEquals;

import com.uber.cadence.RequestCancelWorkflowExecutionRequest;
import com.uber.cadence.ResetWorkflowExecutionRequest;
import com.uber.cadence.internal.compatibility.ClientObjects;
import java.util.UUID;
import org.junit.Test;

public class RequestMapperOptionalRequestIdTest {

  @Test
  public void cancelWithoutRequestIdMapsToEmptyRequestId() {
    RequestCancelWorkflowExecutionRequest request =
        new RequestCancelWorkflowExecutionRequest()
            .setDomain("domain")
            .setWorkflowExecution(ClientObjects.WORKFLOW_EXECUTION);

    com.uber.cadence.api.v1.RequestCancelWorkflowExecutionRequest mapped =
        RequestMapper.requestCancelWorkflowExecutionRequest(request);

    assertEquals("", mapped.getRequestId());
    assertEquals("domain", mapped.getDomain());
  }

  @Test
  public void resetWithoutRequestIdGeneratesUniqueRequestId() {
    ResetWorkflowExecutionRequest request =
        new ResetWorkflowExecutionRequest()
            .setDomain("domain")
            .setWorkflowExecution(ClientObjects.WORKFLOW_EXECUTION)
            .setReason("reason")
            .setDecisionFinishEventId(1);

    String firstRequestId = RequestMapper.resetWorkflowExecutionRequest(request).getRequestId();
    String secondRequestId = RequestMapper.resetWorkflowExecutionRequest(request).getRequestId();

    assertEquals(firstRequestId, UUID.fromString(firstRequestId).toString());
    assertNotEquals(firstRequestId, secondRequestId);
  }
}
