/*
 Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.

 Modifications copyright (C) 2017 Uber Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License"). You may not
 use this file except in compliance with the License. A copy of the License is
 located at

 http://aws.amazon.com/apache2.0

 or in the "license" file accompanying this file. This file is distributed on
 an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 express or implied. See the License for the specific language governing
 permissions and limitations under the License.
*/

package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import com.uber.cadence.*;
import com.uber.cadence.internal.testservice.TestWorkflowService;
import com.uber.cadence.serviceclient.AsyncMethodCallback;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class TestWorkflowEnvironmentInternalTest {
  @Mock TestWorkflowService testService;

  TestWorkflowEnvironmentInternal.WorkflowServiceWrapper serviceWrapper;

  private final Class[] argTypes;
  private final Object[] args;
  private final Object response;
  private String methodName;

  public TestWorkflowEnvironmentInternalTest(
      String methodName, Object[] args, Class[] arguments, Object response) {
    this.methodName = methodName;
    this.args = args;
    this.argTypes = arguments;
    this.response = response;
  }

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    serviceWrapper = new TestWorkflowEnvironmentInternal.WorkflowServiceWrapper();
    try {
      Field implField =
          TestWorkflowEnvironmentInternal.WorkflowServiceWrapper.class.getDeclaredField("impl");
      implField.setAccessible(true);
      implField.set(serviceWrapper, testService);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail("Failed to set impl field: " + e.getMessage());
    }
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> cases() {
    return Arrays.asList(
        new Object[][] {
          {
            "RecordActivityTaskHeartbeat",
            new Object[] {new RecordActivityTaskHeartbeatRequest()},
            new Class[] {RecordActivityTaskHeartbeatRequest.class},
            new RecordActivityTaskHeartbeatResponse()
          },
          {
            "RecordActivityTaskHeartbeatByID",
            new Object[] {new RecordActivityTaskHeartbeatByIDRequest()},
            new Class[] {RecordActivityTaskHeartbeatByIDRequest.class},
            new RecordActivityTaskHeartbeatResponse()
          },
          {
            "RespondActivityTaskCompleted",
            new Object[] {new RespondActivityTaskCompletedRequest()},
            new Class[] {RespondActivityTaskCompletedRequest.class},
            null
          },
          {
            "RespondActivityTaskCompletedByID",
            new Object[] {new RespondActivityTaskCompletedByIDRequest()},
            new Class[] {RespondActivityTaskCompletedByIDRequest.class},
            null
          },
          {
            "RespondActivityTaskFailed",
            new Object[] {new RespondActivityTaskFailedRequest()},
            new Class[] {RespondActivityTaskFailedRequest.class},
            null
          },
          {
            "RespondActivityTaskFailedByID",
            new Object[] {new RespondActivityTaskFailedByIDRequest()},
            new Class[] {RespondActivityTaskFailedByIDRequest.class},
            null
          },
          {
            "RespondActivityTaskCanceled",
            new Object[] {new RespondActivityTaskCanceledRequest()},
            new Class[] {RespondActivityTaskCanceledRequest.class},
            null
          },
          {
            "RespondActivityTaskCanceledByID",
            new Object[] {new RespondActivityTaskCanceledByIDRequest()},
            new Class[] {RespondActivityTaskCanceledByIDRequest.class},
            null
          },
          {
            "RequestCancelWorkflowExecution",
            new Object[] {new RequestCancelWorkflowExecutionRequest()},
            new Class[] {RequestCancelWorkflowExecutionRequest.class},
            null
          },
          {
            "SignalWorkflowExecution",
            new Object[] {new SignalWorkflowExecutionRequest()},
            new Class[] {SignalWorkflowExecutionRequest.class},
            null
          },
          {
            "SignalWithStartWorkflowExecution",
            new Object[] {new SignalWithStartWorkflowExecutionRequest()},
            new Class[] {SignalWithStartWorkflowExecutionRequest.class},
            mock(StartWorkflowExecutionResponse.class)
          },
          {
            "SignalWithStartWorkflowExecutionAsync",
            new Object[] {new SignalWithStartWorkflowExecutionAsyncRequest()},
            new Class[] {SignalWithStartWorkflowExecutionAsyncRequest.class},
            new SignalWithStartWorkflowExecutionAsyncResponse()
          },
          {
            "ResetWorkflowExecution",
            new Object[] {new ResetWorkflowExecutionRequest()},
            new Class[] {ResetWorkflowExecutionRequest.class},
            new ResetWorkflowExecutionResponse()
          },
          {
            "TerminateWorkflowExecution",
            new Object[] {new TerminateWorkflowExecutionRequest()},
            new Class[] {TerminateWorkflowExecutionRequest.class},
            null
          },
          {
            "ListOpenWorkflowExecutions",
            new Object[] {new ListOpenWorkflowExecutionsRequest()},
            new Class[] {ListOpenWorkflowExecutionsRequest.class},
            new ListOpenWorkflowExecutionsResponse()
          },
          {
            "ListClosedWorkflowExecutions",
            new Object[] {new ListClosedWorkflowExecutionsRequest()},
            new Class[] {ListClosedWorkflowExecutionsRequest.class},
            new ListClosedWorkflowExecutionsResponse()
          },
          {
            "ListWorkflowExecutions",
            new Object[] {new ListWorkflowExecutionsRequest()},
            new Class[] {ListWorkflowExecutionsRequest.class},
            new ListWorkflowExecutionsResponse()
          },
          {
            "ListArchivedWorkflowExecutions",
            new Object[] {new ListArchivedWorkflowExecutionsRequest()},
            new Class[] {ListArchivedWorkflowExecutionsRequest.class},
            new ListArchivedWorkflowExecutionsResponse()
          },
          {
            "ScanWorkflowExecutions",
            new Object[] {new ListWorkflowExecutionsRequest()},
            new Class[] {ListWorkflowExecutionsRequest.class},
            new ListWorkflowExecutionsResponse()
          },
          {
            "CountWorkflowExecutions",
            new Object[] {new CountWorkflowExecutionsRequest()},
            new Class[] {CountWorkflowExecutionsRequest.class},
            new CountWorkflowExecutionsResponse()
          },
          {
            "GetSearchAttributes",
            new Object[] {},
            new Class[] {},
            new GetSearchAttributesResponse()
          },
          {
            "RespondQueryTaskCompleted",
            new Object[] {new RespondQueryTaskCompletedRequest()},
            new Class[] {RespondQueryTaskCompletedRequest.class},
            null
          },
          {
            "ResetStickyTaskList",
            new Object[] {new ResetStickyTaskListRequest()},
            new Class[] {ResetStickyTaskListRequest.class},
            new ResetStickyTaskListResponse()
          },
          {
            "QueryWorkflow",
            new Object[] {new QueryWorkflowRequest()},
            new Class[] {QueryWorkflowRequest.class},
            new QueryWorkflowResponse()
          },
          {
            "DescribeWorkflowExecution",
            new Object[] {new DescribeWorkflowExecutionRequest()},
            new Class[] {DescribeWorkflowExecutionRequest.class},
            new DescribeWorkflowExecutionResponse()
          },
          {
            "DescribeTaskList",
            new Object[] {new DescribeTaskListRequest()},
            new Class[] {DescribeTaskListRequest.class},
            new DescribeTaskListResponse()
          },
          {"GetClusterInfo", new Object[] {}, new Class[] {}, null},
          {
            "ListTaskListPartitions",
            new Object[] {new ListTaskListPartitionsRequest()},
            new Class[] {ListTaskListPartitionsRequest.class},
            new ListTaskListPartitionsResponse()
          },
          {
            "RefreshWorkflowTasks",
            new Object[] {new RefreshWorkflowTasksRequest()},
            new Class[] {RefreshWorkflowTasksRequest.class},
            null
          },
          {
            "RegisterDomain",
            new Object[] {new RegisterDomainRequest()},
            new Class[] {RegisterDomainRequest.class},
            null
          },
          {
            "DescribeDomain",
            new Object[] {new DescribeDomainRequest()},
            new Class[] {DescribeDomainRequest.class},
            new DescribeDomainResponse()
          },
          {
            "ListDomains",
            new Object[] {new ListDomainsRequest()},
            new Class[] {ListDomainsRequest.class},
            new ListDomainsResponse()
          },
          {
            "UpdateDomain",
            new Object[] {new UpdateDomainRequest()},
            new Class[] {UpdateDomainRequest.class},
            new UpdateDomainResponse()
          },
          {
            "DeprecateDomain",
            new Object[] {new DeprecateDomainRequest()},
            new Class[] {DeprecateDomainRequest.class},
            null
          },
          {
            "RestartWorkflowExecution",
            new Object[] {new RestartWorkflowExecutionRequest()},
            new Class[] {RestartWorkflowExecutionRequest.class},
            new RestartWorkflowExecutionResponse()
          },
          {
            "GetTaskListsByDomain",
            new Object[] {new GetTaskListsByDomainRequest()},
            new Class[] {GetTaskListsByDomainRequest.class},
            new GetTaskListsByDomainResponse()
          },
          {
            "StartWorkflowExecution",
            new Object[] {new StartWorkflowExecutionRequest()},
            new Class[] {StartWorkflowExecutionRequest.class},
            new StartWorkflowExecutionResponse()
          },
          {
            "StartWorkflowExecutionAsync",
            new Object[] {new StartWorkflowExecutionAsyncRequest()},
            new Class[] {StartWorkflowExecutionAsyncRequest.class},
            new StartWorkflowExecutionAsyncResponse()
          },
          {
            "StartWorkflowExecutionWithTimeout",
            new Object[] {
              new StartWorkflowExecutionRequest(), mock(AsyncMethodCallback.class), 123L
            },
            new Class[] {
              StartWorkflowExecutionRequest.class, AsyncMethodCallback.class, Long.class
            },
            null
          },
          {
            "StartWorkflowExecutionAsyncWithTimeout",
            new Object[] {
              new StartWorkflowExecutionAsyncRequest(), mock(AsyncMethodCallback.class), 1000L
            },
            new Class[] {
              StartWorkflowExecutionAsyncRequest.class, AsyncMethodCallback.class, Long.class
            },
            null
          },
          {
            "GetWorkflowExecutionHistory",
            new Object[] {
              new GetWorkflowExecutionHistoryRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {GetWorkflowExecutionHistoryRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "GetWorkflowExecutionHistoryWithTimeout",
            new Object[] {
              new GetWorkflowExecutionHistoryRequest(), mock(AsyncMethodCallback.class), 1000L
            },
            new Class[] {
              GetWorkflowExecutionHistoryRequest.class, AsyncMethodCallback.class, Long.class
            },
            null
          },
          {"isHealthy", new Object[] {}, new Class[] {}, new CompletableFuture()},
          {
            "PollForDecisionTask",
            new Object[] {new PollForDecisionTaskRequest()},
            new Class[] {PollForDecisionTaskRequest.class},
            mock(PollForDecisionTaskResponse.class)
          },
          {
            "RespondDecisionTaskCompleted",
            new Object[] {new RespondDecisionTaskCompletedRequest()},
            new Class[] {RespondDecisionTaskCompletedRequest.class},
            new RespondDecisionTaskCompletedResponse()
          },
          {
            "RespondDecisionTaskFailed",
            new Object[] {new RespondDecisionTaskFailedRequest()},
            new Class[] {RespondDecisionTaskFailedRequest.class},
            null
          },
          {
            "PollForActivityTask",
            new Object[] {new PollForActivityTaskRequest()},
            new Class[] {PollForActivityTaskRequest.class},
            new PollForActivityTaskResponse()
          },
          {
            "RecordActivityTaskHeartbeat",
            new Object[] {
              new RecordActivityTaskHeartbeatRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RecordActivityTaskHeartbeatRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RecordActivityTaskHeartbeatByID",
            new Object[] {
              new RecordActivityTaskHeartbeatByIDRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RecordActivityTaskHeartbeatByIDRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskCompleted",
            new Object[] {
              new RespondActivityTaskCompletedRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondActivityTaskCompletedRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskCompletedByID",
            new Object[] {
              new RespondActivityTaskCompletedByIDRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondActivityTaskCompletedByIDRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskFailed",
            new Object[] {new RespondActivityTaskFailedRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RespondActivityTaskFailedRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskFailedByID",
            new Object[] {
              new RespondActivityTaskFailedByIDRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondActivityTaskFailedByIDRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskCanceled",
            new Object[] {
              new RespondActivityTaskCanceledRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondActivityTaskCanceledRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondActivityTaskCanceledByID",
            new Object[] {
              new RespondActivityTaskCanceledByIDRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondActivityTaskCanceledByIDRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RequestCancelWorkflowExecution",
            new Object[] {
              new RequestCancelWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RequestCancelWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "SignalWorkflowExecution",
            new Object[] {new SignalWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {SignalWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "SignalWithStartWorkflowExecution",
            new Object[] {
              new SignalWithStartWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {SignalWithStartWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "SignalWorkflowExecutionWithTimeout",
            new Object[] {
              new SignalWorkflowExecutionRequest(), mock(AsyncMethodCallback.class), 1000L
            },
            new Class[] {
              SignalWorkflowExecutionRequest.class, AsyncMethodCallback.class, Long.class
            },
            null
          },
          {
            "SignalWithStartWorkflowExecution",
            new Object[] {
              new SignalWithStartWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {SignalWithStartWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "SignalWithStartWorkflowExecutionAsync",
            new Object[] {
              new SignalWithStartWorkflowExecutionAsyncRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {
              SignalWithStartWorkflowExecutionAsyncRequest.class, AsyncMethodCallback.class
            },
            null
          },
          {
            "ResetWorkflowExecution",
            new Object[] {new ResetWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ResetWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "TerminateWorkflowExecution",
            new Object[] {new TerminateWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {TerminateWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ListOpenWorkflowExecutions",
            new Object[] {new ListOpenWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ListOpenWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ListClosedWorkflowExecutions",
            new Object[] {
              new ListClosedWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {ListClosedWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ListWorkflowExecutions",
            new Object[] {new ListWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ListWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ListArchivedWorkflowExecutions",
            new Object[] {
              new ListArchivedWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {ListArchivedWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ScanWorkflowExecutions",
            new Object[] {new ListWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ListWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "CountWorkflowExecutions",
            new Object[] {new CountWorkflowExecutionsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {CountWorkflowExecutionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "GetSearchAttributes",
            new Object[] {mock(AsyncMethodCallback.class)},
            new Class[] {AsyncMethodCallback.class},
            null
          },
          {
            "RespondQueryTaskCompleted",
            new Object[] {new RespondQueryTaskCompletedRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RespondQueryTaskCompletedRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ResetStickyTaskList",
            new Object[] {new ResetStickyTaskListRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ResetStickyTaskListRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "QueryWorkflow",
            new Object[] {new QueryWorkflowRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {QueryWorkflowRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "DescribeWorkflowExecution",
            new Object[] {new DescribeWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {DescribeWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "DescribeTaskList",
            new Object[] {new DescribeTaskListRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {DescribeTaskListRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "GetClusterInfo",
            new Object[] {mock(AsyncMethodCallback.class)},
            new Class[] {AsyncMethodCallback.class},
            null
          },
          {
            "ListTaskListPartitions",
            new Object[] {new ListTaskListPartitionsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ListTaskListPartitionsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RefreshWorkflowTasks",
            new Object[] {new RefreshWorkflowTasksRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RefreshWorkflowTasksRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RegisterDomain",
            new Object[] {new RegisterDomainRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RegisterDomainRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "DescribeDomain",
            new Object[] {new DescribeDomainRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {DescribeDomainRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "ListDomains",
            new Object[] {new ListDomainsRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {ListDomainsRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "UpdateDomain",
            new Object[] {new UpdateDomainRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {UpdateDomainRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "DeprecateDomain",
            new Object[] {new DeprecateDomainRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {DeprecateDomainRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RestartWorkflowExecution",
            new Object[] {new RestartWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RestartWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "GetTaskListsByDomain",
            new Object[] {new GetTaskListsByDomainRequest()},
            new Class[] {GetTaskListsByDomainRequest.class},
            new GetTaskListsByDomainResponse()
          },
          {
            "StartWorkflowExecution",
            new Object[] {new StartWorkflowExecutionRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {StartWorkflowExecutionRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "StartWorkflowExecutionAsync",
            new Object[] {
              new StartWorkflowExecutionAsyncRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {StartWorkflowExecutionAsyncRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "GetWorkflowExecutionHistory",
            new Object[] {new GetWorkflowExecutionHistoryRequest()},
            new Class[] {GetWorkflowExecutionHistoryRequest.class},
            new GetWorkflowExecutionHistoryResponse()
          },
          {
            "GetWorkflowExecutionHistoryWithTimeout",
            new Object[] {new GetWorkflowExecutionHistoryRequest(), 1000L},
            new Class[] {GetWorkflowExecutionHistoryRequest.class, Long.class},
            null
          },
          {
            "PollForDecisionTask",
            new Object[] {new PollForDecisionTaskRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {PollForDecisionTaskRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondDecisionTaskCompleted",
            new Object[] {
              new RespondDecisionTaskCompletedRequest(), mock(AsyncMethodCallback.class)
            },
            new Class[] {RespondDecisionTaskCompletedRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "RespondDecisionTaskFailed",
            new Object[] {new RespondDecisionTaskFailedRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {RespondDecisionTaskFailedRequest.class, AsyncMethodCallback.class},
            null
          },
          {
            "PollForActivityTask",
            new Object[] {new PollForActivityTaskRequest(), mock(AsyncMethodCallback.class)},
            new Class[] {PollForActivityTaskRequest.class, AsyncMethodCallback.class},
            null,
          },
        });
  }

  @Test
  public void testWorkflowMethodsRequestResponse() throws Exception {
    // If the call has a response, mock the call to return the response
    if (response != null) {
      when(testService.getClass().getMethod(methodName, argTypes).invoke(testService, args))
          .thenReturn(response);
    }

    // Call the method on the service wrapper
    Object gotResponse =
        serviceWrapper.getClass().getMethod(methodName, argTypes).invoke(serviceWrapper, args);

    // Verify that the response is correct
    if (response != null) {
      assertEquals(response, gotResponse);
    }

    // Verify that the method was called on the test service
    verify(testService).getClass().getMethod(methodName, argTypes).invoke(testService, args);
  }
}
