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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import com.uber.cadence.serviceclient.exceptions.AccessDeniedError;
import com.uber.cadence.serviceclient.exceptions.CancellationAlreadyRequestedError;
import com.uber.cadence.serviceclient.exceptions.ClientVersionNotSupportedError;
import com.uber.cadence.serviceclient.exceptions.DomainAlreadyExistsError;
import com.uber.cadence.serviceclient.exceptions.DomainNotActiveError;
import com.uber.cadence.serviceclient.exceptions.EntityNotExistsError;
import com.uber.cadence.serviceclient.exceptions.FeatureNotEnabledError;
import com.uber.cadence.serviceclient.exceptions.InternalDataInconsistencyError;
import com.uber.cadence.serviceclient.exceptions.InternalServiceError;
import com.uber.cadence.serviceclient.exceptions.LimitExceededError;
import com.uber.cadence.serviceclient.exceptions.ServiceBusyError;
import com.uber.cadence.serviceclient.exceptions.ServiceClientError;
import com.uber.cadence.serviceclient.exceptions.WorkflowExecutionAlreadyCompletedError;
import com.uber.cadence.serviceclient.exceptions.WorkflowExecutionAlreadyStartedError;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

public class ErrorMapper {
  public static ServiceClientError Error(StatusRuntimeException e) {

    Status status = StatusProto.fromThrowable(e);
    if (status == null) {
      return new ServiceClientError("empty status", e);
    }

    Any detail = Any.getDefaultInstance();
    if (status.getDetailsCount() > 0) {
      detail = status.getDetails(0);
    }

    try {
      switch (e.getStatus().getCode()) {
        case PERMISSION_DENIED:
          return new AccessDeniedError(e);
        case INTERNAL:
          return new InternalServiceError(e);
        case NOT_FOUND:
          if (detail.is(com.uber.cadence.api.v1.WorkflowExecutionAlreadyCompletedError.class)) {
            return new WorkflowExecutionAlreadyCompletedError(e);
          } else {
            return new EntityNotExistsError(e);
          }
        case ALREADY_EXISTS:
          if (detail.is(com.uber.cadence.api.v1.CancellationAlreadyRequestedError.class)) {
            return new CancellationAlreadyRequestedError(e);
          } else if (detail.is(com.uber.cadence.api.v1.DomainAlreadyExistsError.class)) {
            return new DomainAlreadyExistsError(e);
          } else if (detail.is(
              com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError.class)) {
            com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError error =
                detail.unpack(com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError.class);
            return new WorkflowExecutionAlreadyStartedError(
                error.getStartRequestId(), error.getRunId());
          }
        case DATA_LOSS:
          return new InternalDataInconsistencyError(e);
        case FAILED_PRECONDITION:
          if (detail.is(com.uber.cadence.api.v1.ClientVersionNotSupportedError.class)) {
            com.uber.cadence.api.v1.ClientVersionNotSupportedError error =
                detail.unpack(com.uber.cadence.api.v1.ClientVersionNotSupportedError.class);
            return new ClientVersionNotSupportedError(
                error.getFeatureVersion(), error.getClientImpl(), error.getSupportedVersions());
          } else if (detail.is(com.uber.cadence.api.v1.FeatureNotEnabledError.class)) {
            com.uber.cadence.api.v1.FeatureNotEnabledError error =
                detail.unpack(com.uber.cadence.api.v1.FeatureNotEnabledError.class);
            return new FeatureNotEnabledError(error.getFeatureFlag());
          } else if (detail.is(com.uber.cadence.api.v1.DomainNotActiveError.class)) {
            com.uber.cadence.api.v1.DomainNotActiveError error =
                detail.unpack(com.uber.cadence.api.v1.DomainNotActiveError.class);
            return new DomainNotActiveError(
                error.getDomain(), error.getCurrentCluster(), error.getActiveCluster());
          }
        case RESOURCE_EXHAUSTED:
          if (detail.is(com.uber.cadence.api.v1.LimitExceededError.class)) {
            return new LimitExceededError(e);
          } else {
            return new ServiceBusyError(e);
          }
        case UNKNOWN:
        default:
          return new ServiceClientError(e);
      }
    } catch (InvalidProtocolBufferException ex) {
      return new ServiceClientError(ex);
    }
  }
}
