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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.cadence.ActivityType;
import com.uber.cadence.PollForActivityTaskResponse;
import com.uber.cadence.converter.JsonDataConverter;
import com.uber.cadence.internal.worker.ActivityTask;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.m3.tally.NoopScope;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Test;

public class POJOActivityTaskHandlerTest {

  public interface GreetingActivities {
    String greet(String name);
  }

  public static class GreetingActivitiesImpl implements GreetingActivities {
    @Override
    public String greet(String name) {
      return "Hello " + name;
    }
  }

  @After
  public void clearContext() {
    if (CurrentActivityExecutionContext.isSet()) {
      CurrentActivityExecutionContext.unset();
    }
  }

  // Regression test for https://github.com/cadence-workflow/cadence-java-client/issues/1047
  @Test
  public void localActivityDoesNotLeakContextWhenReadingInputFails() {
    POJOActivityTaskHandler handler =
        new POJOActivityTaskHandler(
            mock(IWorkflowService.class),
            "test-domain",
            JsonDataConverter.getInstance(),
            mock(ScheduledExecutorService.class));
    handler.setLocalActivitiesImplementation(new Object[] {new GreetingActivitiesImpl()});

    PollForActivityTaskResponse response = mock(PollForActivityTaskResponse.class);
    when(response.getActivityType())
        .thenReturn(new ActivityType().setName("GreetingActivities::greet"));
    when(response.getInput()).thenThrow(new IllegalStateException("corrupted task input"));

    try {
      handler.handle(new ActivityTask(response, () -> {}), new NoopScope(), true);
      fail("expected the input failure to propagate");
    } catch (IllegalStateException e) {
      assertEquals("corrupted task input", e.getMessage());
      // The input must be read inside the local activity's execute(), where the context is set
      // and cleared. If it were ever read earlier (for example eagerly by ActivityTaskImpl), this
      // test would pass without exercising that code, so fail loudly instead.
      assertTrue(
          "task input should be read while executing the local activity",
          Arrays.stream(e.getStackTrace())
              .anyMatch(
                  frame ->
                      frame.getClassName().endsWith("POJOLocalActivityImplementation")
                          && frame.getMethodName().equals("execute")));
    }

    assertFalse(
        "local activity context must not stay attached to the worker thread",
        CurrentActivityExecutionContext.isSet());
  }
}
