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
package com.uber.cadence.internal.compatibility.thrift;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.uber.cadence.AccessDeniedError;
import com.uber.cadence.CancellationAlreadyRequestedError;
import com.uber.cadence.ClientVersionNotSupportedError;
import com.uber.cadence.DomainAlreadyExistsError;
import com.uber.cadence.DomainNotActiveError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.FeatureNotEnabledError;
import com.uber.cadence.InternalDataInconsistencyError;
import com.uber.cadence.InternalServiceError;
import com.uber.cadence.LimitExceededError;
import com.uber.cadence.QueryFailedError;
import com.uber.cadence.ServiceBusyError;
import com.uber.cadence.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.WorkflowExecutionAlreadyStartedError;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.junit.Test;

public class ErrorMapperTest {

  @Test
  public void testPermissionDeniedError() {
    assertEquals(
        new AccessDeniedError("no access"),
        ErrorMapper.mapError(toException(Status.Code.PERMISSION_DENIED, "no access")));
  }

  @Test
  public void testInternalServiceError() {
    assertEquals(
        new InternalServiceError("no bueno"),
        ErrorMapper.mapError(toException(Status.Code.INTERNAL, "no bueno")));
  }

  @Test
  public void testInternalDataInconsistencyError() {
    assertEquals(
        new InternalDataInconsistencyError("no data"),
        ErrorMapper.mapError(toException(Status.Code.DATA_LOSS, "no data")));
  }

  @Test
  public void testWorkflowExecutionAlreadyCompletedError() {
    assertEquals(
        new WorkflowExecutionAlreadyCompletedError().setMessage("done"),
        ErrorMapper.mapError(
            toException(
                Status.Code.NOT_FOUND,
                "done",
                com.uber.cadence.api.v1.WorkflowExecutionAlreadyCompletedError
                    .getDefaultInstance())));
  }

  @Test
  public void testWorkflowExecutionAlreadyStartedError() {
    assertEquals(
        new WorkflowExecutionAlreadyStartedError()
            .setMessage("already started")
            .setStartRequestId("start-request-id")
            .setRunId("run-id"),
        ErrorMapper.mapError(
            toException(
                Status.Code.ALREADY_EXISTS,
                "already started",
                com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError.newBuilder()
                    .setStartRequestId("start-request-id")
                    .setRunId("run-id")
                    .build())));
  }

  @Test
  public void testEntityNotExistsError() {
    assertEquals(
        new EntityNotExistsError()
            .setMessage("not found")
            .setActiveCluster("active-cluster")
            .setCurrentCluster("current-cluster"),
        ErrorMapper.mapError(
            toException(
                Status.Code.NOT_FOUND,
                "not found",
                com.uber.cadence.api.v1.EntityNotExistsError.newBuilder()
                    .setActiveCluster("active-cluster")
                    .setCurrentCluster("current-cluster")
                    .build())));
  }

  @Test
  public void testDomainNotActiveError() {
    assertEquals(
        new DomainNotActiveError()
            .setMessage("domain inactive")
            .setDomainName("domain-name")
            .setActiveCluster("active-cluster")
            .setCurrentCluster("current-cluster"),
        ErrorMapper.mapError(
            toException(
                Status.Code.FAILED_PRECONDITION,
                "domain inactive",
                com.uber.cadence.api.v1.DomainNotActiveError.newBuilder()
                    .setDomain("domain-name")
                    .setActiveCluster("active-cluster")
                    .setCurrentCluster("current-cluster")
                    .build())));
  }

  @Test
  public void testClientVersionNotSupportedError() {
    assertEquals(
        new ClientVersionNotSupportedError()
            .setFeatureVersion("feature-version")
            .setClientImpl("client-impl")
            .setSupportedVersions("supported-versions"),
        ErrorMapper.mapError(
            toException(
                Status.Code.FAILED_PRECONDITION,
                "unsupported version",
                com.uber.cadence.api.v1.ClientVersionNotSupportedError.newBuilder()
                    .setFeatureVersion("feature-version")
                    .setClientImpl("client-impl")
                    .setSupportedVersions("supported-versions")
                    .build())));
  }

  @Test
  public void testFeatureNotEnabledError() {
    assertEquals(
        new FeatureNotEnabledError().setFeatureFlag("feature-flag"),
        ErrorMapper.mapError(
            toException(
                Status.Code.FAILED_PRECONDITION,
                "feature not enabled",
                com.uber.cadence.api.v1.FeatureNotEnabledError.newBuilder()
                    .setFeatureFlag("feature-flag")
                    .build())));
  }

  @Test
  public void testCancellationAlreadyRequestedError() {
    assertEquals(
        new CancellationAlreadyRequestedError().setMessage("cancellation requested"),
        ErrorMapper.mapError(
            toException(
                Status.Code.FAILED_PRECONDITION,
                "cancellation requested",
                com.uber.cadence.api.v1.CancellationAlreadyRequestedError.newBuilder().build())));
  }

  @Test
  public void testDomainAlreadyExistsError() {
    assertEquals(
        new DomainAlreadyExistsError().setMessage("domain exists"),
        ErrorMapper.mapError(
            toException(
                Status.Code.ALREADY_EXISTS,
                "domain exists",
                com.uber.cadence.api.v1.DomainAlreadyExistsError.newBuilder().build())));
  }

  @Test
  public void testLimitExceededError() {
    assertEquals(
        new LimitExceededError().setMessage("limit exceeded"),
        ErrorMapper.mapError(
            toException(
                Status.Code.RESOURCE_EXHAUSTED,
                "limit exceeded",
                com.uber.cadence.api.v1.LimitExceededError.newBuilder().build())));
  }

  @Test
  public void testQueryFailedError() {
    assertEquals(
        new QueryFailedError().setMessage("query failed"),
        ErrorMapper.mapError(
            toException(
                Status.Code.INVALID_ARGUMENT,
                "query failed",
                com.uber.cadence.api.v1.QueryFailedError.newBuilder().build())));
  }

  @Test
  public void testServiceBusyError() {
    assertEquals(
        new ServiceBusyError().setMessage("service busy"),
        ErrorMapper.mapError(
            toException(
                Status.Code.UNAVAILABLE,
                "service busy",
                com.uber.cadence.api.v1.ServiceBusyError.newBuilder().build())));
  }

  private static StatusRuntimeException toException(Status.Code code, String message) {
    return StatusProto.toStatusRuntimeException(
        com.google.rpc.Status.newBuilder().setCode(code.value()).setMessage(message).build());
  }

  private static StatusRuntimeException toException(
      Status.Code code, String message, Message details) {
    return StatusProto.toStatusRuntimeException(
        com.google.rpc.Status.newBuilder()
            .setCode(code.value())
            .addDetails(Any.pack(details))
            .setMessage(message)
            .build());
  }
}
