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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.uber.cadence.api.v1.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ErrorMapperTest {

  @Parameterized.Parameter(0)
  public Status status;

  @Parameterized.Parameter(1)
  public Message detail;

  @Parameterized.Parameter(2)
  public Class<Throwable> expectedException;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Object[][] data =
        new Object[][] {
          {Status.PERMISSION_DENIED, null, com.uber.cadence.entities.AccessDeniedError.class},
          {Status.INTERNAL, null, com.uber.cadence.entities.InternalServiceError.class},
          {Status.NOT_FOUND, null, com.uber.cadence.entities.EntityNotExistsError.class},
          {
            Status.ALREADY_EXISTS,
            DomainAlreadyExistsError.getDefaultInstance(),
            com.uber.cadence.entities.DomainAlreadyExistsError.class
          },
          {
            Status.FAILED_PRECONDITION,
            FeatureNotEnabledError.getDefaultInstance(),
            com.uber.cadence.entities.FeatureNotEnabledError.class
          },
          {
            Status.RESOURCE_EXHAUSTED,
            LimitExceededError.getDefaultInstance(),
            com.uber.cadence.entities.LimitExceededError.class
          },
          {Status.UNKNOWN, null, com.uber.cadence.entities.BaseError.class},
          {
            Status.NOT_FOUND,
            WorkflowExecutionAlreadyCompletedError.getDefaultInstance(),
            com.uber.cadence.entities.WorkflowExecutionAlreadyCompletedError.class
          },
          {
            Status.ALREADY_EXISTS,
            WorkflowExecutionAlreadyStartedError.getDefaultInstance(),
            com.uber.cadence.entities.WorkflowExecutionAlreadyStartedError.class
          },
          {
            Status.FAILED_PRECONDITION,
            DomainNotActiveError.getDefaultInstance(),
            com.uber.cadence.entities.DomainNotActiveError.class
          },
          {
            Status.FAILED_PRECONDITION,
            ClientVersionNotSupportedError.getDefaultInstance(),
            com.uber.cadence.entities.ClientVersionNotSupportedError.class
          },
          {
            Status.FAILED_PRECONDITION,
            FeatureNotEnabledError.getDefaultInstance(),
            com.uber.cadence.entities.FeatureNotEnabledError.class
          },
          {
            Status.FAILED_PRECONDITION,
            DomainNotActiveError.getDefaultInstance(),
            com.uber.cadence.entities.DomainNotActiveError.class
          },
          {
            Status.FAILED_PRECONDITION,
            ClientVersionNotSupportedError.getDefaultInstance(),
            com.uber.cadence.entities.ClientVersionNotSupportedError.class
          },
          {
            Status.FAILED_PRECONDITION,
            FeatureNotEnabledError.getDefaultInstance(),
            com.uber.cadence.entities.FeatureNotEnabledError.class
          },
          {
            Status.RESOURCE_EXHAUSTED,
            LimitExceededError.getDefaultInstance(),
            com.uber.cadence.entities.LimitExceededError.class
          },
          {Status.DATA_LOSS, null, com.uber.cadence.entities.InternalDataInconsistencyError.class},
          {
            Status.RESOURCE_EXHAUSTED,
            ServiceBusyError.getDefaultInstance(),
            com.uber.cadence.entities.ServiceBusyError.class
          },
          {Status.INTERNAL, null, com.uber.cadence.entities.InternalServiceError.class}
        };
    return Arrays.asList(data);
  }

  @Test
  public void testErrorMapper() {
    com.google.rpc.Status.Builder builder =
        com.google.rpc.Status.newBuilder().setCode(status.getCode().value());

    if (detail != null) {
      builder.addDetails(Any.pack(detail));
    }

    StatusRuntimeException ex = StatusProto.toStatusRuntimeException(builder.build());
    com.uber.cadence.entities.BaseError result = ErrorMapper.Error(ex);
    assertEquals(expectedException, result.getClass());
  }
}
