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

package com.uber.cadence.workflow;

import static org.junit.Assert.*;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.testing.TestWorkflowEnvironment;
import com.uber.cadence.worker.Worker;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnhancedGetVersionTest {

  private TestWorkflowEnvironment testEnvironment;
  private Worker worker;

  @Before
  public void setUp() {
    testEnvironment = TestWorkflowEnvironment.newInstance();
    worker = testEnvironment.newWorker("test-task-list");
  }

  @After
  public void tearDown() {
    testEnvironment.close();
  }

  @Test
  public void testGetVersionWithCustomVersion() throws ExecutionException, InterruptedException {
    worker.registerWorkflowImplementationTypes(TestWorkflowWithCustomVersionImpl.class);
    testEnvironment.start();

    WorkflowClient client = testEnvironment.newWorkflowClient();

    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofMinutes(5))
            .setTaskList("test-task-list")
            .build();

    TestWorkflowWithCustomVersion workflow =
        client.newWorkflowStub(TestWorkflowWithCustomVersion.class, options);
    String result = workflow.execute("test-input");

    assertEquals("custom-version-result", result);
  }

  @Test
  public void testGetVersionWithMinVersion() throws ExecutionException, InterruptedException {
    worker.registerWorkflowImplementationTypes(TestWorkflowWithMinVersionImpl.class);
    testEnvironment.start();

    WorkflowClient client = testEnvironment.newWorkflowClient();

    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofMinutes(5))
            .setTaskList("test-task-list")
            .build();

    TestWorkflowWithMinVersion workflow =
        client.newWorkflowStub(TestWorkflowWithMinVersion.class, options);
    String result = workflow.execute("test-input");

    assertEquals("min-version-result", result);
  }

  @Test
  public void testGetVersionWithOptions() throws ExecutionException, InterruptedException {
    worker.registerWorkflowImplementationTypes(TestWorkflowWithOptionsImpl.class);
    testEnvironment.start();

    WorkflowClient client = testEnvironment.newWorkflowClient();

    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofMinutes(5))
            .setTaskList("test-task-list")
            .build();

    TestWorkflowWithOptions workflow =
        client.newWorkflowStub(TestWorkflowWithOptions.class, options);
    String result = workflow.execute("test-input");

    assertEquals("options-result", result);
  }

  @Test
  public void testConvenienceMethods() throws ExecutionException, InterruptedException {
    worker.registerWorkflowImplementationTypes(TestWorkflowWithConvenienceMethodsImpl.class);
    testEnvironment.start();

    WorkflowClient client = testEnvironment.newWorkflowClient();

    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofMinutes(5))
            .setTaskList("test-task-list")
            .build();

    TestWorkflowWithConvenienceMethods workflow =
        client.newWorkflowStub(TestWorkflowWithConvenienceMethods.class, options);
    String result = workflow.execute("test-input");

    assertEquals("convenience-result", result);
  }

  public interface TestWorkflowWithCustomVersion {
    @WorkflowMethod
    String execute(String input);
  }

  public static class TestWorkflowWithCustomVersionImpl implements TestWorkflowWithCustomVersion {
    @Override
    public String execute(String input) {
      int version =
          Workflow.getVersion(
              "test-change", 1, 3, GetVersionOptions.newBuilder().executeWithVersion(2).build());

      if (version == 2) {
        return "custom-version-result";
      } else {
        return "other-result";
      }
    }
  }

  public interface TestWorkflowWithMinVersion {
    @WorkflowMethod
    String execute(String input);
  }

  public static class TestWorkflowWithMinVersionImpl implements TestWorkflowWithMinVersion {
    @Override
    public String execute(String input) {
      int version =
          Workflow.getVersion(
              "test-change", 1, 3, GetVersionOptions.newBuilder().executeWithMinVersion().build());

      if (version == 1) {
        return "min-version-result";
      } else {
        return "other-result";
      }
    }
  }

  public interface TestWorkflowWithOptions {
    @WorkflowMethod
    String execute(String input);
  }

  public static class TestWorkflowWithOptionsImpl implements TestWorkflowWithOptions {
    @Override
    public String execute(String input) {
      GetVersionOptions options = GetVersionOptions.newBuilder().executeWithVersion(2).build();

      int version = Workflow.getVersion("test-change", 1, 3, options);

      if (version == 2) {
        return "options-result";
      } else {
        return "other-result";
      }
    }
  }

  public interface TestWorkflowWithConvenienceMethods {
    @WorkflowMethod
    String execute(String input);
  }

  public static class TestWorkflowWithConvenienceMethodsImpl
      implements TestWorkflowWithConvenienceMethods {
    @Override
    public String execute(String input) {
      int version1 = Workflow.getVersionWithCustomVersion("test-change-1", 1, 3, 2);
      int version2 = Workflow.getVersionWithMinVersion("test-change-2", 1, 3);

      if (version1 == 2 && version2 == 1) {
        return "convenience-result";
      } else {
        return "other-result";
      }
    }
  }
}
