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

package com.uber.cadence.workflow.interceptors;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityOptions;
import com.uber.cadence.activity.LocalActivityOptions;
import com.uber.cadence.internal.sync.SyncWorkflowDefinition;
import com.uber.cadence.internal.worker.WorkflowExecutionException;
import com.uber.cadence.workflow.*;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracingWorkflowInterceptorFactory
    implements Function<WorkflowInterceptor, WorkflowInterceptor> {

  private final FilteredTrace trace = new FilteredTrace();
  private List<String> expected;
  private static final Logger log =
      LoggerFactory.getLogger(TracingWorkflowInterceptorFactory.class);

  @Override
  public WorkflowInterceptor apply(WorkflowInterceptor next) {
    return new TracingWorkflowInterceptor(trace, next);
  }

  public String getTrace() {
    return String.join("\n", trace.getImpl());
  }

  public void setExpected(String... expected) {
    this.expected = Arrays.asList(expected);
  }

  public void assertExpected() {
    if (expected != null) {
      List<String> traceElements = trace.getImpl();
      for (int i = 0; i < traceElements.size(); i++) {
        String t = traceElements.get(i);
        String expectedRegExp = expected.get(i);
        Assert.assertTrue(t + " doesn't match " + expectedRegExp, t.matches(expectedRegExp));
      }
    }
  }

  private static class FilteredTrace {

    private final List<String> impl = Collections.synchronizedList(new ArrayList<>());

    public boolean add(String s) {
      log.trace("FilteredTrace isReplaying=" + Workflow.isReplaying());
      if (!Workflow.isReplaying()) {
        return impl.add(s);
      }
      return true;
    }

    List<String> getImpl() {
      return impl;
    }
  }

  private static class TracingWorkflowInterceptor implements WorkflowInterceptor {

    private final FilteredTrace trace;
    private final WorkflowInterceptor next;

    private TracingWorkflowInterceptor(FilteredTrace trace, WorkflowInterceptor next) {
      this.trace = trace;
      this.next = Objects.requireNonNull(next);
    }

    @Override
    public byte[] executeWorkflow(
        SyncWorkflowDefinition workflowDefinition, WorkflowExecuteInput input)
        throws CancellationException, WorkflowExecutionException {
      trace.add("executeWorkflow: " + input.getWorkflowType().getName());
      return next.executeWorkflow(workflowDefinition, input);
    }

    @Override
    public <R> Promise<R> executeActivity(
        String activityName,
        Class<R> resultClass,
        Type resultType,
        Object[] args,
        ActivityOptions options) {
      trace.add("executeActivity " + activityName);
      return next.executeActivity(activityName, resultClass, resultType, args, options);
    }

    @Override
    public <R> Promise<R> executeLocalActivity(
        String activityName,
        Class<R> resultClass,
        Type resultType,
        Object[] args,
        LocalActivityOptions options) {
      trace.add("executeLocalActivity " + activityName);
      return next.executeLocalActivity(activityName, resultClass, resultType, args, options);
    }

    @Override
    public <R> WorkflowResult<R> executeChildWorkflow(
        String workflowType,
        Class<R> resultClass,
        Type resultType,
        Object[] args,
        ChildWorkflowOptions options) {
      trace.add("executeChildWorkflow " + workflowType);
      return next.executeChildWorkflow(workflowType, resultClass, resultType, args, options);
    }

    @Override
    public Random newRandom() {
      trace.add("newRandom");
      return next.newRandom();
    }

    @Override
    public Promise<Void> signalExternalWorkflow(
        String domain, WorkflowExecution execution, String signalName, Object[] args) {
      trace.add("signalExternalWorkflow " + execution.getWorkflowId() + " " + signalName);
      return next.signalExternalWorkflow(domain, execution, signalName, args);
    }

    @Override
    public Promise<Void> signalExternalWorkflow(
        WorkflowExecution execution, String signalName, Object[] args) {
      trace.add("signalExternalWorkflow " + execution.getWorkflowId() + " " + signalName);
      return next.signalExternalWorkflow(execution, signalName, args);
    }

    @Override
    public Promise<Void> cancelWorkflow(WorkflowExecution execution) {
      trace.add("cancelWorkflow " + execution.getWorkflowId());
      return next.cancelWorkflow(execution);
    }

    @Override
    public void sleep(Duration duration) {
      trace.add("sleep " + duration);
      next.sleep(duration);
    }

    @Override
    public boolean await(Duration timeout, String reason, Supplier<Boolean> unblockCondition) {
      trace.add("await " + timeout + " " + reason);
      return next.await(timeout, reason, unblockCondition);
    }

    @Override
    public void await(String reason, Supplier<Boolean> unblockCondition) {
      trace.add("await " + reason);
      next.await(reason, unblockCondition);
    }

    @Override
    public Promise<Void> newTimer(Duration duration) {
      trace.add("newTimer " + duration);
      return next.newTimer(duration);
    }

    @Override
    public <R> R sideEffect(Class<R> resultClass, Type resultType, Functions.Func<R> func) {
      trace.add("sideEffect");
      return next.sideEffect(resultClass, resultType, func);
    }

    @Override
    public <R> R mutableSideEffect(
        String id,
        Class<R> resultClass,
        Type resultType,
        BiPredicate<R, R> updated,
        Functions.Func<R> func) {
      trace.add("mutableSideEffect");
      return next.mutableSideEffect(id, resultClass, resultType, updated, func);
    }

    @Override
    public int getVersion(String changeID, int minSupported, int maxSupported) {
      trace.add("getVersion");
      return next.getVersion(changeID, minSupported, maxSupported);
    }

    @Override
    public int getVersion(
        String changeID, int minSupported, int maxSupported, GetVersionOptions options) {
      trace.add("getVersion with options");
      return next.getVersion(changeID, minSupported, maxSupported, options);
    }

    @Override
    public void continueAsNew(
        Optional<String> workflowType, Optional<ContinueAsNewOptions> options, Object[] args) {
      trace.add("continueAsNew");
      next.continueAsNew(workflowType, options, args);
    }

    @Override
    public void registerQuery(
        String queryType, Type[] argTypes, Functions.Func1<Object[], Object> callback) {
      trace.add("registerQuery " + queryType);
      next.registerQuery(queryType, argTypes, callback);
    }

    @Override
    public UUID randomUUID() {
      trace.add("randomUUID");
      return next.randomUUID();
    }

    @Override
    public void upsertSearchAttributes(Map<String, Object> searchAttributes) {
      trace.add("upsertSearchAttributes");
      next.upsertSearchAttributes(searchAttributes);
    }
  }
}
