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

package com.uber.cadence.testing;

import static org.junit.Assert.*;

import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.worker.Worker;
import com.uber.cadence.workflow.GetVersionOptions;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Test to verify that TestWorkflowEnvironment supports the new GetVersionOptions functionality.
 * This test ensures that workflows using enhanced version control features can be tested properly.
 */
public class TestWorkflowEnvironmentGetVersionTest {

  private static final String TASK_LIST = "TestWorkflowEnvironmentGetVersionTest";

  public interface TestWorkflowWithVersionControl {
    @WorkflowMethod
    String workflowWithVersionControl(String input);
  }

  public static class TestWorkflowWithVersionControlImpl implements TestWorkflowWithVersionControl {

    @Override
    public String workflowWithVersionControl(String input) {
      // Test the new getVersion method with options
      GetVersionOptions options = GetVersionOptions.newBuilder().executeWithVersion(2).build();

      int version = Workflow.getVersion("test-change", 1, 3, options);

      // Test the convenience methods
      int versionWithCustom = Workflow.getVersionWithCustomVersion("test-change", 1, 3, 2);
      int versionWithMin = Workflow.getVersionWithMinVersion("test-change", 1, 3);

      return String.format(
          "input=%s, version=%d, custom=%d, min=%d",
          input, version, versionWithCustom, versionWithMin);
    }
  }

  @Test
  public void testWorkflowWithVersionControl() throws ExecutionException, InterruptedException {
    TestWorkflowEnvironment testEnvironment = TestWorkflowEnvironment.newInstance();

    // Create a worker that polls tasks from the service owned by the testEnvironment
    Worker worker = testEnvironment.newWorker(TASK_LIST);
    worker.registerWorkflowImplementationTypes(TestWorkflowWithVersionControlImpl.class);

    // Create a WorkflowClient that interacts with the server owned by the testEnvironment
    WorkflowClient client = testEnvironment.newWorkflowClient();

    // Create workflow options with required timeout
    WorkflowOptions options =
        new WorkflowOptions.Builder()
            .setExecutionStartToCloseTimeout(Duration.ofMinutes(5))
            .setTaskList(TASK_LIST)
            .build();

    TestWorkflowWithVersionControl workflow =
        client.newWorkflowStub(TestWorkflowWithVersionControl.class, options);

    // Start the test environment (this starts the worker)
    testEnvironment.start();

    // Start a workflow execution
    String result = workflow.workflowWithVersionControl("test-input");

    // Verify the result contains the expected version information
    assertNotNull("Result should not be null", result);
    assertTrue("Result should contain version information", result.contains("version="));
    assertTrue("Result should contain custom version information", result.contains("custom="));
    assertTrue("Result should contain min version information", result.contains("min="));

    // Close workers and release in-memory service
    testEnvironment.close();
  }

  @Test
  public void testGetVersionOptionsBuilder() {
    // Test the builder pattern for GetVersionOptions
    GetVersionOptions options1 = GetVersionOptions.newBuilder().executeWithVersion(5).build();

    assertTrue("Should have custom version", options1.getCustomVersion().isPresent());
    assertEquals(
        "Custom version should be 5", Integer.valueOf(5), options1.getCustomVersion().get());
    assertFalse("Should not use min version", options1.isUseMinVersion());

    GetVersionOptions options2 = GetVersionOptions.newBuilder().executeWithMinVersion().build();

    assertFalse("Should not have custom version", options2.getCustomVersion().isPresent());
    assertTrue("Should use min version", options2.isUseMinVersion());

    GetVersionOptions options3 =
        GetVersionOptions.newBuilder().executeWithVersion(10).executeWithMinVersion().build();

    assertTrue("Should have custom version", options3.getCustomVersion().isPresent());
    assertEquals(
        "Custom version should be 10", Integer.valueOf(10), options3.getCustomVersion().get());
    assertTrue("Should use min version", options3.isUseMinVersion());
  }

  @Test
  public void testDefaultGetVersionOptions() {
    // Test default options
    GetVersionOptions defaultOptions = GetVersionOptions.newBuilder().build();

    assertFalse(
        "Default should not have custom version", defaultOptions.getCustomVersion().isPresent());
    assertFalse("Default should not use min version", defaultOptions.isUseMinVersion());
  }
}
