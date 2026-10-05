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

package com.uber.cadence.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.uber.cadence.activity.Activity;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.client.WorkflowException;
import com.uber.cadence.testUtils.CadenceTestRule;
import com.uber.cadence.workflow.ActivityStub;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowMethod;
import java.time.Duration;
import org.junit.Rule;
import org.junit.Test;

public class RegistryTest {

  private static final Registry WORKFLOW_REGISTRY =
      Registry.newInstance().registerWorkflowImplementationTypes(ActivityByNameWorkflowImpl.class);

  @Rule public CadenceTestRule testRule = CadenceTestRule.builder().build();

  public interface ActivityByNameWorkflow {
    @WorkflowMethod
    String execute(String activityType, boolean local, String input);
  }

  public static class ActivityByNameWorkflowImpl implements ActivityByNameWorkflow {
    @Override
    public String execute(String activityType, boolean local, String input) {
      ActivityStub stub;
      if (local) {
        stub =
            Workflow.newUntypedLocalActivityStub(
                new LocalActivityOptions.Builder()
                    .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                    .build());
      } else {
        stub =
            Workflow.newUntypedActivityStub(
                new ActivityOptions.Builder()
                    .setScheduleToCloseTimeout(Duration.ofSeconds(10))
                    .build());
      }
      return stub.execute(activityType, String.class, input);
    }
  }

  public interface EchoActivity {
    String echo(String input);
  }

  public static class EchoActivityImpl implements EchoActivity {
    @Override
    public String echo(String input) {
      return "echo:" + input;
    }
  }

  private String execute(String taskList, String activityType, boolean local, String input) {
    ActivityByNameWorkflow workflow =
        testRule
            .getWorkflowClient()
            .newWorkflowStub(
                ActivityByNameWorkflow.class, testRule.workflowOptionsBuilder(taskList).build());
    return workflow.execute(activityType, local, input);
  }

  private String otherTaskList() {
    return testRule.getDefaultTaskList() + "-other";
  }

  private String thirdTaskList() {
    return testRule.getDefaultTaskList() + "-third";
  }

  @Test
  public void testFunctionActivity() {
    Registry registry =
        WORKFLOW_REGISTRY.registerActivity(
            "upper",
            String.class,
            String.class,
            (input) -> input.toUpperCase() + ":" + Activity.getTask().getActivityType());
    testRule.getWorker().setRegistry(registry);
    testRule.start();

    String taskList = testRule.getDefaultTaskList();
    assertEquals("INPUT:upper", execute(taskList, "upper", false, "input"));
    assertEquals("INPUT:upper", execute(taskList, "upper", true, "input"));
  }

  @Test
  public void testRegistryIsImmutable() {
    Registry original =
        WORKFLOW_REGISTRY.registerActivity(
            "unrelated", String.class, String.class, (input) -> input);
    Registry extended =
        original.registerActivity("greet", String.class, String.class, (input) -> "greet:" + input);
    // The original registry still doesn't have the activity, so registering it again succeeds.
    Registry other =
        original.registerActivity("greet", String.class, String.class, (input) -> "other:" + input);
    testRule.getWorker().setRegistry(original);
    testRule.getWorker(otherTaskList()).setRegistry(extended);
    testRule.getWorker(thirdTaskList()).setRegistry(other);
    testRule.start();

    assertThrows(
        WorkflowException.class, () -> execute(testRule.getDefaultTaskList(), "greet", false, "a"));
    assertEquals("greet:a", execute(otherTaskList(), "greet", false, "a"));
    assertEquals("other:a", execute(thirdTaskList(), "greet", false, "a"));
  }

  @Test
  public void testRegisterAll() {
    Registry first =
        Registry.newInstance()
            .registerActivity("first", String.class, String.class, (input) -> "first:" + input);
    Registry second =
        Registry.newInstance()
            .registerActivity("second", String.class, String.class, (input) -> "second:" + input);
    Registry combined = WORKFLOW_REGISTRY.registerAll(first).registerAll(second);
    assertThrows(IllegalStateException.class, () -> combined.registerAll(first));
    testRule.getWorker().setRegistry(combined);
    testRule.start();

    String taskList = testRule.getDefaultTaskList();
    assertEquals("first:a", execute(taskList, "first", false, "a"));
    assertEquals("second:b", execute(taskList, "second", false, "b"));
  }

  @Test
  public void testDuplicateRegistrationThrows() {
    Registry registry =
        WORKFLOW_REGISTRY
            .registerActivity("greet", String.class, String.class, (input) -> input)
            .registerActivitiesImplementations(new EchoActivityImpl());
    assertThrows(
        IllegalStateException.class,
        () -> registry.registerActivity("greet", String.class, String.class, (input) -> input));
    assertThrows(
        IllegalStateException.class,
        () -> registry.registerActivitiesImplementations(new EchoActivityImpl()));
    assertThrows(
        IllegalStateException.class,
        () -> registry.registerWorkflowImplementationTypes(ActivityByNameWorkflowImpl.class));
  }

  @Test
  public void testWorkerRegistrationDoesNotModifyRegistry() {
    Registry registry =
        Registry.newInstance()
            .registerActivity("upper", String.class, String.class, String::toUpperCase);
    Worker worker = testRule.getWorker();
    worker.setRegistry(registry);
    // Replaces the workflows only, so the activities of the registry remain.
    worker.registerWorkflowImplementationTypes(ActivityByNameWorkflowImpl.class);
    // Replaces the activities, including the ones of the registry.
    worker.registerActivitiesImplementations(new EchoActivityImpl());
    Worker other = testRule.getWorker(otherTaskList());
    other.setRegistry(registry);
    other.registerWorkflowImplementationTypes(ActivityByNameWorkflowImpl.class);
    testRule.start();

    String taskList = testRule.getDefaultTaskList();
    assertEquals("echo:a", execute(taskList, "EchoActivity::echo", false, "a"));
    assertThrows(WorkflowException.class, () -> execute(taskList, "upper", false, "a"));
    assertEquals("A", execute(otherTaskList(), "upper", false, "a"));
    assertThrows(
        WorkflowException.class, () -> execute(otherTaskList(), "EchoActivity::echo", false, "a"));
  }

  @Test
  public void testSetRegistryAfterStartThrows() {
    Worker worker = testRule.getWorker();
    worker.setRegistry(WORKFLOW_REGISTRY);
    testRule.start();
    assertThrows(IllegalStateException.class, () -> worker.setRegistry(Registry.newInstance()));
  }
}
