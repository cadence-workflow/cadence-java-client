/*
 *  Modifications Copyright (c) 2017-2021 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.uber.cadence.internal.compatibility.thrift;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.rpc.Status;
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
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.function.BiFunction;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorMapper.class);

  public static TException mapError(StatusRuntimeException ex) {
    Status status = StatusProto.fromThrowable(ex);
    // No details, fall back to code mapping
    if (status == null || status.getDetailsCount() == 0) {
      return fromCode(ex);
    }
    String message = status.getMessage();
    Any firstDetail = status.getDetails(0);

    if (firstDetail.is(com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError.class,
          message,
          firstDetail,
          ErrorMapper::mapWorkflowExecutionAlreadyStarted);
    } else if (firstDetail.is(com.uber.cadence.api.v1.EntityNotExistsError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.EntityNotExistsError.class,
          message,
          firstDetail,
          ErrorMapper::mapEntityNotExistsError);
    } else if (firstDetail.is(
        com.uber.cadence.api.v1.WorkflowExecutionAlreadyCompletedError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.WorkflowExecutionAlreadyCompletedError.class,
          message,
          firstDetail,
          ErrorMapper::mapWorkflowExecutionAlreadyCompleted);
    } else if (firstDetail.is(com.uber.cadence.api.v1.DomainNotActiveError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.DomainNotActiveError.class,
          message,
          firstDetail,
          ErrorMapper::mapDomainNotActive);
    } else if (firstDetail.is(com.uber.cadence.api.v1.ClientVersionNotSupportedError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.ClientVersionNotSupportedError.class,
          message,
          firstDetail,
          ErrorMapper::mapClientVersionNotSupported);
    } else if (firstDetail.is(com.uber.cadence.api.v1.FeatureNotEnabledError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.FeatureNotEnabledError.class,
          message,
          firstDetail,
          ErrorMapper::mapFeatureNotEnabled);
    } else if (firstDetail.is(com.uber.cadence.api.v1.CancellationAlreadyRequestedError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.CancellationAlreadyRequestedError.class,
          message,
          firstDetail,
          ErrorMapper::mapCancellationAlreadyRequested);
    } else if (firstDetail.is(com.uber.cadence.api.v1.DomainAlreadyExistsError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.DomainAlreadyExistsError.class,
          message,
          firstDetail,
          ErrorMapper::mapDomainAlreadyExists);
    } else if (firstDetail.is(com.uber.cadence.api.v1.LimitExceededError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.LimitExceededError.class,
          message,
          firstDetail,
          ErrorMapper::mapLimitExceeded);
    } else if (firstDetail.is(com.uber.cadence.api.v1.QueryFailedError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.QueryFailedError.class,
          message,
          firstDetail,
          ErrorMapper::mapQueryFailed);
    } else if (firstDetail.is(com.uber.cadence.api.v1.ServiceBusyError.class)) {
      return mapDetails(
          com.uber.cadence.api.v1.ServiceBusyError.class,
          message,
          firstDetail,
          ErrorMapper::mapServiceBusy);
    }

    // It has details, but they're unhandled. Fall back to the error code mapping
    return fromCode(ex);
  }

  private static TException fromCode(StatusRuntimeException exception) {
    String message = exception.getStatus().getDescription();
    switch (exception.getStatus().getCode()) {
      case PERMISSION_DENIED:
        return new AccessDeniedError(message);
      case INTERNAL:
        return new InternalServiceError(message);
      case DATA_LOSS:
        return new InternalDataInconsistencyError(message);
      default:
        return new TException(exception);
    }
  }

  private static <T extends Message> TException mapDetails(
      Class<T> detailsType,
      String message,
      Any any,
      BiFunction<String, ? super T, TException> mapper) {
    try {
      if (any.is(detailsType)) {
        T details = any.unpack(detailsType);
        return mapper.apply(message, details);
      } else {
        LOGGER.error(
            "Failed to decode exception. Message of type: {} was not: {}",
            any.getTypeUrl(),
            detailsType.getCanonicalName());
        return new TException(message);
      }
    } catch (InvalidProtocolBufferException ex) {
      LOGGER.error(
          "Failed to decode exception of type: {} as {}",
          any.getTypeUrl(),
          detailsType.getCanonicalName(),
          ex);
      return new TException(message);
    }
  }

  private static WorkflowExecutionAlreadyStartedError mapWorkflowExecutionAlreadyStarted(
      String message, com.uber.cadence.api.v1.WorkflowExecutionAlreadyStartedError protoErr) {
    return new WorkflowExecutionAlreadyStartedError()
        .setMessage(message)
        .setStartRequestId(protoErr.getStartRequestId())
        .setRunId(protoErr.getRunId());
  }

  private static EntityNotExistsError mapEntityNotExistsError(
      String message, com.uber.cadence.api.v1.EntityNotExistsError protoErr) {
    return new EntityNotExistsError()
        .setMessage(message)
        .setActiveCluster(protoErr.getActiveCluster())
        .setCurrentCluster(protoErr.getCurrentCluster());
  }

  private static WorkflowExecutionAlreadyCompletedError mapWorkflowExecutionAlreadyCompleted(
      String message, com.uber.cadence.api.v1.WorkflowExecutionAlreadyCompletedError protoErr) {
    return new WorkflowExecutionAlreadyCompletedError().setMessage(message);
  }

  private static DomainNotActiveError mapDomainNotActive(
      String message, com.uber.cadence.api.v1.DomainNotActiveError protoErr) {
    return new DomainNotActiveError()
        .setMessage(message)
        .setDomainName(protoErr.getDomain())
        .setActiveCluster(protoErr.getActiveCluster())
        .setCurrentCluster(protoErr.getCurrentCluster());
  }

  private static ClientVersionNotSupportedError mapClientVersionNotSupported(
      String message, com.uber.cadence.api.v1.ClientVersionNotSupportedError protoErr) {
    return new ClientVersionNotSupportedError()
        .setFeatureVersion(protoErr.getFeatureVersion())
        .setClientImpl(protoErr.getClientImpl())
        .setSupportedVersions(protoErr.getSupportedVersions());
  }

  private static FeatureNotEnabledError mapFeatureNotEnabled(
      String message, com.uber.cadence.api.v1.FeatureNotEnabledError protoErr) {
    return new FeatureNotEnabledError().setFeatureFlag(protoErr.getFeatureFlag());
  }

  private static CancellationAlreadyRequestedError mapCancellationAlreadyRequested(
      String message, com.uber.cadence.api.v1.CancellationAlreadyRequestedError protoErr) {
    return new CancellationAlreadyRequestedError().setMessage(message);
  }

  private static DomainAlreadyExistsError mapDomainAlreadyExists(
      String message, com.uber.cadence.api.v1.DomainAlreadyExistsError protoErr) {
    return new DomainAlreadyExistsError().setMessage(message);
  }

  private static LimitExceededError mapLimitExceeded(
      String message, com.uber.cadence.api.v1.LimitExceededError protoErr) {
    return new LimitExceededError().setMessage(message);
  }

  private static QueryFailedError mapQueryFailed(
      String message, com.uber.cadence.api.v1.QueryFailedError protoErr) {
    return new QueryFailedError().setMessage(message);
  }

  private static ServiceBusyError mapServiceBusy(
      String message, com.uber.cadence.api.v1.ServiceBusyError protoErr) {
    return new ServiceBusyError().setMessage(message);
  }
}
