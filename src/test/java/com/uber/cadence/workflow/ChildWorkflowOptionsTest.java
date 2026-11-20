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

import com.uber.cadence.ActiveClusterSelectionPolicy;
import com.uber.cadence.ClusterAttribute;
import com.uber.cadence.CronOverlapPolicy;
import java.time.Duration;
import org.junit.Assert;
import org.junit.Test;

public class ChildWorkflowOptionsTest {

  @WorkflowMethod
  public void defaultWorkflowOptions() {}

  private static ActiveClusterSelectionPolicy testPolicy() {
    return new ActiveClusterSelectionPolicy()
        .setClusterAttribute(new ClusterAttribute().setScope("location").setName("lisbon"));
  }

  private static ChildWorkflowOptions.Builder optionsWithStartPolicies() {
    return new ChildWorkflowOptions.Builder()
        .setTaskList("foo")
        .setExecutionStartToCloseTimeout(Duration.ofSeconds(321))
        .setCronOverlapPolicy(CronOverlapPolicy.BUFFERONE)
        .setActiveClusterSelectionPolicy(testPolicy());
  }

  private static void assertStartPolicies(ChildWorkflowOptions o) {
    Assert.assertEquals(CronOverlapPolicy.BUFFERONE, o.getCronOverlapPolicy());
    Assert.assertEquals(testPolicy(), o.getActiveClusterSelectionPolicy());
  }

  @Test
  public void testStartPoliciesSetOnBuilder() {
    assertStartPolicies(optionsWithStartPolicies().build());
    assertStartPolicies(optionsWithStartPolicies().validateAndBuildWithDefaults());
  }

  @Test
  public void testStartPoliciesDefaultToNull() {
    ChildWorkflowOptions o = new ChildWorkflowOptions.Builder().build();
    Assert.assertNull(o.getCronOverlapPolicy());
    Assert.assertNull(o.getActiveClusterSelectionPolicy());
  }

  @Test
  public void testStartPoliciesKeptByCopyConstructor() {
    assertStartPolicies(
        new ChildWorkflowOptions.Builder(optionsWithStartPolicies().build()).build());
  }

  @Test
  public void testStartPoliciesKeptByMerge() throws NoSuchMethodException {
    WorkflowMethod a =
        ChildWorkflowOptionsTest.class
            .getMethod("defaultWorkflowOptions")
            .getAnnotation(WorkflowMethod.class);
    assertStartPolicies(
        ChildWorkflowOptions.merge(a, null, null, optionsWithStartPolicies().build()));
  }

  @Test
  public void testStartPoliciesConsideredByEqualsAndHashCode() {
    ChildWorkflowOptions o = optionsWithStartPolicies().build();
    Assert.assertEquals(o, optionsWithStartPolicies().build());
    Assert.assertEquals(o.hashCode(), optionsWithStartPolicies().build().hashCode());
    Assert.assertNotEquals(
        o, optionsWithStartPolicies().setCronOverlapPolicy(CronOverlapPolicy.SKIPPED).build());
    Assert.assertNotEquals(
        o, optionsWithStartPolicies().setActiveClusterSelectionPolicy(null).build());
  }
}
