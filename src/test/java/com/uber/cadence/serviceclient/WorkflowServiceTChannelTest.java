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
package com.uber.cadence.serviceclient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.SubChannel;
import com.uber.tchannel.api.TFuture;
import com.uber.tchannel.api.errors.TChannelError;
import com.uber.tchannel.headers.ArgScheme;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import com.uber.tchannel.messages.generated.HealthStatus;
import com.uber.tchannel.messages.generated.Meta;
import java.util.Arrays;
import java.util.Collections;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class WorkflowServiceTChannelTest {

  interface RemoteCallAsync<T> {
    void apply(AsyncMethodCallback<T> callback) throws TException;
  }

  private static class BaseTest<T, R> {
    SubChannel mockSubChannel;
    WorkflowServiceTChannel service;

    @Before
    public void setUp() {
      mockSubChannel = mock(SubChannel.class);
      service = new WorkflowServiceTChannel(mockSubChannel, ClientOptions.newBuilder().build());
    }

    @Parameterized.Parameter(0)
    public ResponseCode responseCode;

    @Parameterized.Parameter(1)
    public T responseBody;

    @Parameterized.Parameter(2)
    public Class<? extends TException> expectedException;

    @Parameterized.Parameter(3)
    public R expectedResponse;

    TFuture<ThriftResponse<T>> mockResponse(ResponseCode responseCode, T body) {
      // TFuture
      TFuture<ThriftResponse<T>> tFuture = TFuture.create(ArgScheme.THRIFT, null);
      tFuture.set(
          new ThriftResponse.Builder<T>(new ThriftRequest.Builder<>("", "").build())
              .setResponseCode(responseCode)
              .setBody(body)
              .build());
      return tFuture;
    }

    void testHelper(WorkflowServiceTChannel.RemoteCall<R> method) throws TException, TChannelError {
      when(mockSubChannel.send(any(ThriftRequest.class)))
          .thenReturn(mockResponse(responseCode, responseBody));
      if (expectedException != null) {
        assertThrows(expectedException, method::apply);
      } else {
        assertEquals(expectedResponse, method.apply());
      }
    }

    void testHelperWithCallback(RemoteCallAsync<R> method) throws TChannelError, TException {
      when(mockSubChannel.send(any(ThriftRequest.class)))
          .thenReturn(mockResponse(responseCode, responseBody));
      method.apply(
          new AsyncMethodCallback<R>() {
            @Override
            public void onComplete(R r) {
              if (expectedException != null) {
                fail("Expected exception but got response: " + r);
              } else {
                assertEquals(expectedResponse, r);
              }
            }

            @Override
            public void onError(Exception e) {
              assertEquals(expectedException, e.getClass());
            }
          });
    }

    void assertUnimplementedWithCallback(RemoteCallAsync<R> method) {
      assertThrows(
          UnsupportedOperationException.class,
          () ->
              method.apply(
                  new AsyncMethodCallback<R>() {
                    @Override
                    public void onComplete(R r) {
                      fail("shouldn't reach this line");
                    }

                    @Override
                    public void onError(Exception e) {
                      fail("shouldn't reach this line");
                    }
                  }));
    }

    void assertNoOpWithCallback(RemoteCallAsync<R> method) {
      try {
        method.apply(
            new AsyncMethodCallback<R>() {
              @Override
              public void onComplete(R r) {
                fail("shouldn't reach this line");
              }

              @Override
              public void onError(Exception e) {
                fail("shouldn't reach this line");
              }
            });
      } catch (TException e) {
        fail("should not throw exception:" + e);
      }
    }
  }

  @RunWith(Parameterized.class)
  public static class RegisterDomainTest
      extends BaseTest<WorkflowService.RegisterDomain_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RegisterDomain_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setDomainExistsError(new DomainAlreadyExistsError("")),
              DomainAlreadyExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RegisterDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.RegisterDomain(new RegisterDomainRequest());
            return null;
          });
    }

    @Test
    public void responseIsHandledCorrectlyWithCallback() throws Exception {
      testHelperWithCallback(
          callback -> {
            service.SignalWorkflowExecution(new SignalWorkflowExecutionRequest(), callback);
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeDomainTest
      extends BaseTest<WorkflowService.DescribeDomain_result, DescribeDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeDomain_result().setSuccess(new DescribeDomainResponse()),
              null,
              new DescribeDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              new DescribeDomainResponse(),
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setSuccess(new DescribeDomainResponse())
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeDomain(new DescribeDomainRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.DescribeDomain(new DescribeDomainRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListDomainsTest
      extends BaseTest<WorkflowService.ListDomains_result, ListDomainsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListDomains_result().setSuccess(new ListDomainsResponse()),
              null,
              new ListDomainsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListDomains_result().setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListDomains_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListDomains_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {ResponseCode.Error, new WorkflowService.ListDomains_result(), TException.class, null},
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListDomains(new ListDomainsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.ListDomains(new ListDomainsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ResetWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.ResetWorkflowExecution_result, ResetWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setSuccess(new ResetWorkflowExecutionResponse()),
              null,
              new ResetWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ResetWorkflowExecution(new ResetWorkflowExecutionRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.ListDomains(new ListDomainsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class TerminateWorkflowExecutionTest
      extends BaseTest<WorkflowService.TerminateWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.TerminateWorkflowExecution_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.TerminateWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.TerminateWorkflowExecution(new TerminateWorkflowExecutionRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.TerminateWorkflowExecution(
                  new TerminateWorkflowExecutionRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListOpenWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListOpenWorkflowExecutions_result, ListOpenWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setSuccess(new ListOpenWorkflowExecutionsResponse()),
              null,
              new ListOpenWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListOpenWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListOpenWorkflowExecutions(new ListOpenWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.ListOpenWorkflowExecutions(
                  new ListOpenWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListClosedWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListClosedWorkflowExecutions_result,
          ListClosedWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setSuccess(new ListClosedWorkflowExecutionsResponse()),
              null,
              new ListClosedWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListClosedWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> service.ListClosedWorkflowExecutions(new ListClosedWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.ListClosedWorkflowExecutions(
                  new ListClosedWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListWorkflowExecutions_result, ListWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setSuccess(new ListWorkflowExecutionsResponse()),
              null,
              new ListWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.ListWorkflowExecutions(new ListWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListArchivedWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ListArchivedWorkflowExecutions_result,
          ListArchivedWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setSuccess(new ListArchivedWorkflowExecutionsResponse()),
              null,
              new ListArchivedWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListArchivedWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () ->
              service.ListArchivedWorkflowExecutions(new ListArchivedWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.ListArchivedWorkflowExecutions(
                  new ListArchivedWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ScanWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.ScanWorkflowExecutions_result, ListWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setSuccess(new ListWorkflowExecutionsResponse()),
              null,
              new ListWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ScanWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.ScanWorkflowExecutions(new ListWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class CountWorkflowExecutionsTest
      extends BaseTest<
          WorkflowService.CountWorkflowExecutions_result, CountWorkflowExecutionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setSuccess(new CountWorkflowExecutionsResponse()),
              null,
              new CountWorkflowExecutionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.CountWorkflowExecutions_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.CountWorkflowExecutions(new CountWorkflowExecutionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.CountWorkflowExecutions(new CountWorkflowExecutionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetSearchAttributesTest
      extends BaseTest<WorkflowService.GetSearchAttributes_result, GetSearchAttributesResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetSearchAttributes_result()
                  .setSuccess(new GetSearchAttributesResponse()),
              null,
              new GetSearchAttributesResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetSearchAttributes_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.GetSearchAttributes());
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(callback -> service.GetSearchAttributes(callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondQueryTaskCompletedTest
      extends BaseTest<WorkflowService.RespondQueryTaskCompleted_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondQueryTaskCompleted_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondQueryTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(
          () -> {
            service.RespondQueryTaskCompleted(new RespondQueryTaskCompletedRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondQueryTaskCompleted(new RespondQueryTaskCompletedRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ResetStickyTaskListTest
      extends BaseTest<WorkflowService.ResetStickyTaskList_result, ResetStickyTaskListResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ResetStickyTaskList_result()
                  .setSuccess(new ResetStickyTaskListResponse()),
              null,
              new ResetStickyTaskListResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError()),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError()),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ResetStickyTaskList_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ResetStickyTaskList(new ResetStickyTaskListRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.ResetStickyTaskList(new ResetStickyTaskListRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class QueryWorkflowTest
      extends BaseTest<WorkflowService.QueryWorkflow_result, QueryWorkflowResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.QueryWorkflow_result().setSuccess(new QueryWorkflowResponse()),
              null,
              new QueryWorkflowResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.QueryWorkflow_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error, new WorkflowService.QueryWorkflow_result(), TException.class, null
            },
            {
              ResponseCode.Error, new WorkflowService.QueryWorkflow_result(), TException.class, null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.QueryWorkflow(new QueryWorkflowRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.QueryWorkflow(new QueryWorkflowRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.DescribeWorkflowExecution_result, DescribeWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setSuccess(new DescribeWorkflowExecutionResponse()),
              null,
              new DescribeWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeWorkflowExecution(new DescribeWorkflowExecutionRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.DescribeWorkflowExecution(new DescribeWorkflowExecutionRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class DescribeTaskListTest
      extends BaseTest<WorkflowService.DescribeTaskList_result, DescribeTaskListResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.DescribeTaskList_result()
                  .setSuccess(new DescribeTaskListResponse()),
              null,
              new DescribeTaskListResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DescribeTaskList_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.DescribeTaskList(new DescribeTaskListRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.DescribeTaskList(new DescribeTaskListRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetClusterInfoTest
      extends BaseTest<WorkflowService.GetClusterInfo_result, ClusterInfo> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetClusterInfo_result().setSuccess(new ClusterInfo()),
              null,
              new ClusterInfo()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result()
                  .setInternalServiceError(new InternalServiceError("")),
              InternalServiceError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetClusterInfo_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.GetClusterInfo());
    }

    @Test
    public void callbackIsNotSupported() {
      assertNoOpWithCallback(callback -> service.GetClusterInfo(callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class ListTaskListPartitionsTest
      extends BaseTest<
          WorkflowService.ListTaskListPartitions_result, ListTaskListPartitionsResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.ListTaskListPartitions_result()
                  .setSuccess(new ListTaskListPartitionsResponse()),
              null,
              new ListTaskListPartitionsResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null,
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.ListTaskListPartitions_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void testResponse() throws Exception {
      testHelper(() -> service.ListTaskListPartitions(new ListTaskListPartitionsRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertNoOpWithCallback(
          callback ->
              service.ListTaskListPartitions(new ListTaskListPartitionsRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class UpdateDomainTest
      extends BaseTest<WorkflowService.UpdateDomain_result, UpdateDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.UpdateDomain_result().setSuccess(new UpdateDomainResponse()),
              null,
              new UpdateDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result().setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.UpdateDomain_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {ResponseCode.Error, new WorkflowService.UpdateDomain_result(), TException.class, null},
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.UpdateDomain(new UpdateDomainRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.UpdateDomain(new UpdateDomainRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetWorkflowExecutionHistoryTest
      extends BaseTest<
          WorkflowService.GetWorkflowExecutionHistory_result, GetWorkflowExecutionHistoryResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setSuccess(new GetWorkflowExecutionHistoryResponse()),
              null,
              new GetWorkflowExecutionHistoryResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetWorkflowExecutionHistory_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> service.GetWorkflowExecutionHistory(new GetWorkflowExecutionHistoryRequest()));
    }

    @Test
    public void responseIsHandledCorrectlyWithCallback() throws Exception {
      testHelperWithCallback(
          callback ->
              service.GetWorkflowExecutionHistory(
                  new GetWorkflowExecutionHistoryRequest(), callback));
      testHelperWithCallback(
          callback ->
              service.GetWorkflowExecutionHistoryWithTimeout(
                  new GetWorkflowExecutionHistoryRequest(), callback, 1000L));
    }

    @Test
    public void responseIsHandledCorrectlyWithTimeout() throws Exception {
      testHelper(
          () ->
              service.GetWorkflowExecutionHistoryWithTimeout(
                  new GetWorkflowExecutionHistoryRequest(), 1000L));
    }
  }

  @RunWith(Parameterized.class)
  public static class StartWorkflowExecutionAsyncTest
      extends BaseTest<
          WorkflowService.StartWorkflowExecutionAsync_result, StartWorkflowExecutionAsyncResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setSuccess(new StartWorkflowExecutionAsyncResponse()),
              null,
              new StartWorkflowExecutionAsyncResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setSessionAlreadyExistError(new WorkflowExecutionAlreadyStartedError()),
              WorkflowExecutionAlreadyStartedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecutionAsync_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.StartWorkflowExecutionAsync(
                  new StartWorkflowExecutionAsyncRequest()
                      .setRequest(new StartWorkflowExecutionRequest())));
    }

    @Test
    public void responseIsHandledCorrectWithCallback() throws TChannelError, TException {
      testHelperWithCallback(
          callback ->
              service.StartWorkflowExecutionAsync(
                  new StartWorkflowExecutionAsyncRequest()
                      .setRequest(new StartWorkflowExecutionRequest()),
                  callback));
      testHelperWithCallback(
          callback ->
              service.StartWorkflowExecutionAsyncWithTimeout(
                  new StartWorkflowExecutionAsyncRequest()
                      .setRequest(new StartWorkflowExecutionRequest()),
                  callback,
                  1000L));
    }
  }

  @RunWith(Parameterized.class)
  public static class StartWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.StartWorkflowExecution_result, StartWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.StartWorkflowExecution_result()
                  .setSuccess(new StartWorkflowExecutionResponse()),
              null,
              new StartWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setSessionAlreadyExistError(new WorkflowExecutionAlreadyStartedError()),
              WorkflowExecutionAlreadyStartedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.StartWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.StartWorkflowExecution(new StartWorkflowExecutionRequest()));
    }

    @Test
    public void responseIsHandledCorrectWithCallback() throws TChannelError, TException {
      testHelperWithCallback(
          callback ->
              service.StartWorkflowExecution(new StartWorkflowExecutionRequest(), callback));
      testHelperWithCallback(
          callback ->
              service.StartWorkflowExecutionWithTimeout(
                  new StartWorkflowExecutionRequest(), callback, 1000L));
    }
  }

  @RunWith(Parameterized.class)
  public static class GetTaskListsByDomainTest
      extends BaseTest<WorkflowService.GetTaskListsByDomain_result, GetTaskListsByDomainResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setSuccess(new GetTaskListsByDomainResponse()),
              null,
              new GetTaskListsByDomainResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.GetTaskListsByDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.GetTaskListsByDomain(new GetTaskListsByDomainRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.GetTaskListsByDomain(new GetTaskListsByDomainRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class DeprecateDomainTest
      extends BaseTest<WorkflowService.DeprecateDomain_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.DeprecateDomain_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.DeprecateDomain_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.DeprecateDomain(new DeprecateDomainRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.DeprecateDomain(new DeprecateDomainRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class PollForDecisionTaskTest
      extends BaseTest<WorkflowService.PollForDecisionTask_result, PollForDecisionTaskResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.PollForDecisionTask_result()
                  .setSuccess(new PollForDecisionTaskResponse()),
              null,
              new PollForDecisionTaskResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForDecisionTask_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.PollForDecisionTask(new PollForDecisionTaskRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.PollForDecisionTask(new PollForDecisionTaskRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondDecisionTaskCompletedTest
      extends BaseTest<
          WorkflowService.RespondDecisionTaskCompleted_result,
          RespondDecisionTaskCompletedResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setSuccess(new RespondDecisionTaskCompletedResponse()),
              null,
              new RespondDecisionTaskCompletedResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> service.RespondDecisionTaskCompleted(new RespondDecisionTaskCompletedRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondDecisionTaskCompleted(
                  new RespondDecisionTaskCompletedRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class PollForActivityTaskTest
      extends BaseTest<WorkflowService.PollForActivityTask_result, PollForActivityTaskResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.PollForActivityTask_result()
                  .setSuccess(new PollForActivityTaskResponse()),
              null,
              new PollForActivityTaskResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.PollForActivityTask_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(() -> service.PollForActivityTask(new PollForActivityTaskRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback -> service.PollForActivityTask(new PollForActivityTaskRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RecordActivityTaskHeartbeatTest
      extends BaseTest<
          WorkflowService.RecordActivityTaskHeartbeat_result, RecordActivityTaskHeartbeatResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setSuccess(new RecordActivityTaskHeartbeatResponse()),
              null,
              new RecordActivityTaskHeartbeatResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError()),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeat_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> service.RecordActivityTaskHeartbeat(new RecordActivityTaskHeartbeatRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RecordActivityTaskHeartbeat(
                  new RecordActivityTaskHeartbeatRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RecordActivityTaskHeartbeatByIDTest
      extends BaseTest<
          WorkflowService.RecordActivityTaskHeartbeatByID_result,
          RecordActivityTaskHeartbeatResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setSuccess(new RecordActivityTaskHeartbeatResponse()),
              null,
              new RecordActivityTaskHeartbeatResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError()),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RecordActivityTaskHeartbeatByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.RecordActivityTaskHeartbeatByID(
                  new RecordActivityTaskHeartbeatByIDRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RecordActivityTaskHeartbeatByID(
                  new RecordActivityTaskHeartbeatByIDRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCompletedTest
      extends BaseTest<WorkflowService.RespondActivityTaskCompleted_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK, new WorkflowService.RespondActivityTaskCompleted_result(), null, null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompleted_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCompleted(new RespondActivityTaskCompletedRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskCompleted(
                  new RespondActivityTaskCompletedRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCompletedByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskCompletedByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskCompletedByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCompletedByID_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCompletedByID(new RespondActivityTaskCompletedByIDRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskCompletedByID(
                  new RespondActivityTaskCompletedByIDRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskFailedTest
      extends BaseTest<WorkflowService.RespondActivityTaskFailed_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondActivityTaskFailed_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailed_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskFailed(new RespondActivityTaskFailedRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskFailed(new RespondActivityTaskFailedRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskFailedByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskFailedByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskFailedByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskFailedByID_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskFailedByID(new RespondActivityTaskFailedByIDRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskFailedByID(
                  new RespondActivityTaskFailedByIDRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCanceledTest
      extends BaseTest<WorkflowService.RespondActivityTaskCanceled_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondActivityTaskCanceled_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceled_result(),
              TException.class,
              null
            }
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCanceled(new RespondActivityTaskCanceledRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskCanceled(
                  new RespondActivityTaskCanceledRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondActivityTaskCanceledByIDTest
      extends BaseTest<WorkflowService.RespondActivityTaskCanceledByID_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RespondActivityTaskCanceledByID_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondActivityTaskCanceledByID_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondActivityTaskCanceledByID(new RespondActivityTaskCanceledByIDRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondActivityTaskCanceledByID(
                  new RespondActivityTaskCanceledByIDRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RequestCancelWorkflowExecutionTest
      extends BaseTest<WorkflowService.RequestCancelWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.RequestCancelWorkflowExecution_result(),
              null,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RequestCancelWorkflowExecution_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RequestCancelWorkflowExecution(new RequestCancelWorkflowExecutionRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RequestCancelWorkflowExecution(
                  new RequestCancelWorkflowExecutionRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWorkflowExecutionTest
      extends BaseTest<WorkflowService.SignalWorkflowExecution_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.SignalWorkflowExecution_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWorkflowExecution_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.SignalWorkflowExecution(new SignalWorkflowExecutionRequest());
            return null;
          });
    }

    @Test
    public void responseIsHandledCorrectlyWithCallback() throws Exception {
      testHelperWithCallback(
          callback -> {
            service.SignalWorkflowExecution(new SignalWorkflowExecutionRequest(), callback);
          });
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWithStartWorkflowExecutionTest
      extends BaseTest<
          WorkflowService.SignalWithStartWorkflowExecution_result, StartWorkflowExecutionResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setSuccess(new StartWorkflowExecutionResponse()),
              null,
              new StartWorkflowExecutionResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecution_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.SignalWithStartWorkflowExecution(
                  new SignalWithStartWorkflowExecutionRequest()));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.SignalWithStartWorkflowExecution(
                  new SignalWithStartWorkflowExecutionRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RefreshWorkflowTasksTest
      extends BaseTest<WorkflowService.RefreshWorkflowTasks_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RefreshWorkflowTasks_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RefreshWorkflowTasks_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RefreshWorkflowTasks(new RefreshWorkflowTasksRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertNoOpWithCallback(
          callback -> service.RefreshWorkflowTasks(new RefreshWorkflowTasksRequest(), callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class SignalWithStartWorkflowExecutionAsyncTest
      extends BaseTest<
          WorkflowService.SignalWithStartWorkflowExecutionAsync_result,
          SignalWithStartWorkflowExecutionAsyncResponse> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setSuccess(new SignalWithStartWorkflowExecutionAsyncResponse()),
              null,
              new SignalWithStartWorkflowExecutionAsyncResponse()
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result(),
              TException.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setLimitExceededError(new LimitExceededError("")),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.SignalWithStartWorkflowExecutionAsync_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            }
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () ->
              service.SignalWithStartWorkflowExecutionAsync(
                  new SignalWithStartWorkflowExecutionAsyncRequest()
                      .setRequest(new SignalWithStartWorkflowExecutionRequest())));
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.SignalWithStartWorkflowExecutionAsync(
                  new SignalWithStartWorkflowExecutionAsyncRequest()
                      .setRequest(new SignalWithStartWorkflowExecutionRequest()),
                  callback));
    }
  }

  @RunWith(Parameterized.class)
  public static class RespondDecisionTaskFailedTest
      extends BaseTest<WorkflowService.RespondDecisionTaskFailed_result, Void> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {ResponseCode.OK, new WorkflowService.RespondDecisionTaskFailed_result(), null, null},
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setBadRequestError(new BadRequestError("")),
              BadRequestError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setEntityNotExistError(new EntityNotExistsError("", Collections.emptyList())),
              EntityNotExistsError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setWorkflowExecutionAlreadyCompletedError(
                      new WorkflowExecutionAlreadyCompletedError("")),
              WorkflowExecutionAlreadyCompletedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setServiceBusyError(new ServiceBusyError("")),
              ServiceBusyError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setDomainNotActiveError(new DomainNotActiveError()),
              DomainNotActiveError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setClientVersionNotSupportedError(new ClientVersionNotSupportedError()),
              ClientVersionNotSupportedError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result()
                  .setLimitExceededError(new LimitExceededError()),
              LimitExceededError.class,
              null
            },
            {
              ResponseCode.Error,
              new WorkflowService.RespondDecisionTaskFailed_result(),
              TException.class,
              null
            },
          });
    }

    @Test
    public void responseIsHandledCorrectly() throws Exception {
      testHelper(
          () -> {
            service.RespondDecisionTaskFailed(new RespondDecisionTaskFailedRequest());
            return null;
          });
    }

    @Test
    public void callbackIsNotSupported() {
      assertUnimplementedWithCallback(
          callback ->
              service.RespondDecisionTaskFailed(new RespondDecisionTaskFailedRequest(), callback));
    }
  }

  public static class ConstructorTest {
    @Test
    public void testDefault() {
      IWorkflowService service = new WorkflowServiceTChannel(ClientOptions.newBuilder().build());
      assertNotNull(service);
    }
  }

  @RunWith(Parameterized.class)
  public static class IsHealthyTest extends BaseTest<Meta.health_result, Boolean> {
    @Parameterized.Parameters(name = "{index}: Response Code {0}, Response {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {
              ResponseCode.OK,
              new Meta.health_result().setSuccess(new HealthStatus().setOk(true)),
              null,
              Boolean.TRUE
            },
            {
              ResponseCode.OK,
              new Meta.health_result().setSuccess(new HealthStatus().setOk(false)),
              null,
              Boolean.FALSE
            },
          });
    }

    @Test
    public void testResult() throws TException, TChannelError {
      testHelper(
          () -> {
            try {
              return service.isHealthy().get();
            } catch (Exception e) {
              fail("should not throw exception: " + e);
            }
            return null;
          });
    }
  }
}
