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

package com.uber.cadence.internal.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.uber.cadence.common.RetryOptions;
import com.uber.cadence.workflow.Async;
import com.uber.cadence.workflow.CancellationScope;
import com.uber.cadence.workflow.CompletablePromise;
import com.uber.cadence.workflow.Functions;
import com.uber.cadence.workflow.Promise;
import com.uber.cadence.workflow.Workflow;
import com.uber.cadence.workflow.WorkflowTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DeterministicRunnerTest {

  @Rule public final Tracer trace = new Tracer();

  private String status;
  private boolean unblock1;
  private boolean unblock2;
  private Throwable failure;
  private long currentTime;
  private ExecutorService threadPool;

  @Before
  public void setUp() {
    unblock1 = false;
    unblock2 = false;
    failure = null;
    status = "initial";
    currentTime = 0;
    threadPool = new ThreadPoolExecutor(1, 1000, 1, TimeUnit.SECONDS, new SynchronousQueue<>());
  }

  @After
  public void tearDown() throws InterruptedException {
    threadPool.shutdownNow();
    threadPool.awaitTermination(10, TimeUnit.SECONDS);
  }

  @Test
  public void testYield() throws Throwable {
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              status = "started";
              WorkflowThread.await("reason1", () -> unblock1);
              status = "after1";
              WorkflowThread.await("reason2", () -> unblock2);
              status = "done";
            });
    assertEquals("initial", status);
    d.runUntilAllBlocked();
    assertEquals("started", status);
    assertFalse(d.isDone());
    unblock1 = true;
    d.runUntilAllBlocked();
    assertEquals("after1", status);
    // Just check that running again doesn't make any progress.
    d.runUntilAllBlocked();
    assertEquals("after1", status);
    unblock2 = true;
    d.runUntilAllBlocked();
    assertEquals("done", status);
    assertTrue(d.isDone());
  }

  @Test
  public void testSleep() throws Throwable {
    DeterministicRunnerImpl d =
        new DeterministicRunnerImpl(
            threadPool,
            null,
            () -> currentTime, // clock override
            () -> {
              status = "started";
              Workflow.sleep(60000);
              status = "afterSleep1";
              Workflow.sleep(60000);
              status = "done";
            });
    currentTime = 1000;
    assertEquals("initial", status);
    assertEquals(1000, d.currentTimeMillis());
    d.runUntilAllBlocked();
    currentTime = 20000;
    assertEquals("started", status);
    assertEquals(20000, d.currentTimeMillis());
    d.runUntilAllBlocked();
    assertEquals("started", status);
    assertFalse(d.isDone());

    currentTime = 70000; // unblocks first sleep
    d.runUntilAllBlocked();
    assertEquals("afterSleep1", status);
    // Just check that running again doesn't make any progress.
    d.runUntilAllBlocked();
    assertEquals("afterSleep1", status);
    assertEquals(70000, d.currentTimeMillis());

    currentTime = 200000; // unblock second sleep
    d.runUntilAllBlocked();
    assertEquals("done", status);
    assertTrue(d.isDone());
  }

  /**
   * Async retry cannot be tested here as it relies on timer that is implemented outside of
   * Dispatcher.
   *
   * @see WorkflowTest#testAsyncRetry()
   */
  @Test
  public void testRetry() throws Throwable {
    RetryOptions retryOptions =
        new RetryOptions.Builder()
            .setInitialInterval(Duration.ofSeconds(10))
            .setMaximumInterval(Duration.ofSeconds(100))
            .setExpiration(Duration.ofMinutes(5))
            .setBackoffCoefficient(2.0)
            .build();
    DeterministicRunnerImpl d =
        new DeterministicRunnerImpl(
            threadPool,
            null,
            () -> currentTime, // clock override
            () -> {
              trace.add("started");
              Workflow.retry(
                  retryOptions,
                  () -> {
                    trace.add("retry at " + Workflow.currentTimeMillis());
                    throw new IllegalThreadStateException("simulated");
                  });
              trace.add("beforeSleep");
              Workflow.sleep(60000);
              trace.add("done");
            });
    try {
      for (int i = 0; i < Duration.ofSeconds(400).toMillis(); i += 10) {
        currentTime = i;
        d.runUntilAllBlocked();
      }
      fail("failure expected");
    } catch (IllegalThreadStateException e) {
      assertEquals("simulated", e.getMessage());
    }
    int attempt = 1;
    long time = 0;
    trace.addExpected("started");
    while (time < retryOptions.getExpiration().toMillis()) {
      trace.addExpected("retry at " + time);
      long sleepMillis =
          (long)
              ((Math.pow(retryOptions.getBackoffCoefficient(), attempt - 1))
                  * retryOptions.getInitialInterval().toMillis());
      sleepMillis = Math.min(sleepMillis, retryOptions.getMaximumInterval().toMillis());
      attempt++;
      time += sleepMillis;
    }
  }

  @Test
  public void testRootFailure() throws Throwable {
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              status = "started";
              WorkflowThread.await("reason1", () -> unblock1);
              throw new RuntimeException("simulated");
            });
    assertEquals("initial", status);
    d.runUntilAllBlocked();
    assertEquals("started", status);
    assertFalse(d.isDone());
    unblock1 = true;
    try {
      d.runUntilAllBlocked();
      fail("exception expected");
    } catch (Exception ignored) {
    }
    assertTrue(d.isDone());
  }

  @Test
  public void testDispatcherStop() throws Throwable {
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              status = "started";
              WorkflowThread.await("reason1", () -> unblock1);
              status = "after1";
              try {
                WorkflowThread.await("reason2", () -> unblock2);
              } catch (DestroyWorkflowThreadError e) {
                failure = e;
                throw e;
              }
              status = "done";
            });
    assertEquals("initial", status);
    d.runUntilAllBlocked();
    assertEquals("started", status);
    assertFalse(d.isDone());
    unblock1 = true;
    d.runUntilAllBlocked();
    assertEquals("after1", status);
    d.close();
    assertTrue(d.isDone());
    assertNotNull(failure);
  }

  @Test
  public void testDispatcherExit() throws Throwable {
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              Promise<Void> thread1 =
                  Async.procedure(
                      () -> {
                        trace.add("child1 started");
                        WorkflowThread.await("reason1", () -> unblock1);
                        trace.add("child1 done");
                      });
              Promise<Void> thread2 =
                  Async.procedure(
                      () -> {
                        trace.add("child2 started");
                        WorkflowThread.await("reason2", () -> unblock2);
                        trace.add("child2 exiting");
                        WorkflowThread.exit("exitValue");
                        trace.add("child2 done");
                      });
              thread1.get();
              thread2.get();
              trace.add("root done");
            });
    d.runUntilAllBlocked();
    assertFalse(d.isDone());
    unblock2 = true;
    d.runUntilAllBlocked();
    assertTrue(d.isDone());
    assertEquals("exitValue", d.getExitValue());
    String[] expected =
        new String[] {
          "root started", "child1 started", "child2 started", "child2 exiting",
        };
    trace.setExpected(expected);
  }

  @Test
  public void testRootCancellation() throws Throwable {
    trace.add("init");
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              WorkflowThread.await(
                  "reason1", () -> CancellationScope.current().isCancelRequested());
              trace.add("second await: " + CancellationScope.current().getCancellationReason());
              WorkflowThread.await(
                  "reason1", () -> CancellationScope.current().isCancelRequested());
              trace.add("root done");
            });
    d.runUntilAllBlocked();
    assertFalse(d.isDone());
    d.cancel("I just feel like it");
    d.runUntilAllBlocked();
    assertTrue(d.isDone());
    String[] expected =
        new String[] {
          "init", "root started", "second await: I just feel like it", "root done",
        };
    trace.setExpected(expected);
  }

  @Test
  public void testExplicitScopeCancellation() throws Throwable {
    trace.add("init");
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              CompletablePromise<Void> var = Workflow.newPromise();
              CancellationScope scope =
                  Workflow.newCancellationScope(
                      () -> {
                        trace.add("scope started");
                        var.completeFrom(newTimer(300));
                        trace.add("scope done");
                      });
              trace.add("root before cancel");
              scope.cancel("from root");
              try {
                var.get();
                trace.add("after get");
              } catch (CancellationException e) {
                trace.add("scope cancelled");
              }
              trace.add("root done");
            });
    d.runUntilAllBlocked();
    assertTrue(trace.toString(), d.isDone());
    String[] expected =
        new String[] {
          "init",
          "root started",
          "scope started",
          "scope done",
          "root before cancel",
          "timer cancelled",
          "scope cancelled",
          "root done",
        };
    trace.setExpected(expected);
  }

  @Test
  public void testExplicitDetachedScopeCancellation() throws Throwable {
    trace.add("init");
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              CompletablePromise<Void> var = Workflow.newPromise();
              CancellationScope scope =
                  Workflow.newDetachedCancellationScope(
                      () -> {
                        trace.add("scope started");
                        var.completeFrom(newTimer(300));
                        trace.add("scope done");
                      });
              trace.add("root before cancel");
              scope.cancel("from root");
              try {
                var.get();
                trace.add("after get");
              } catch (CancellationException e) {
                trace.add("scope cancelled");
              }
              trace.add("root done");
            });
    d.runUntilAllBlocked();
    assertTrue(trace.toString(), d.isDone());
    String[] expected =
        new String[] {
          "init",
          "root started",
          "scope started",
          "scope done",
          "root before cancel",
          "timer cancelled",
          "scope cancelled",
          "root done",
        };
    trace.setExpected(expected);
  }

  private Promise<Void> newTimer(int milliseconds) {
    return Async.procedure(
        () -> {
          try {
            Workflow.sleep(milliseconds);
            trace.add("timer fired");
          } catch (CancellationException e) {
            trace.add("timer cancelled");
            throw e;
          }
        });
  }

  @Test
  public void testExplicitThreadCancellation() throws Throwable {
    trace.add("init");
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              CompletablePromise<String> threadDone = Workflow.newPromise();
              CancellationScope scope =
                  Workflow.newCancellationScope(
                      () -> {
                        Async.procedure(
                            () -> {
                              trace.add("thread started");
                              Promise<String> cancellation =
                                  CancellationScope.current().getCancellationRequest();
                              WorkflowThread.await(
                                  "reason1", () -> CancellationScope.current().isCancelRequested());
                              threadDone.completeFrom(cancellation);
                              trace.add("thread done: " + cancellation.get());
                            });
                      });
              trace.add("root before cancel");
              scope.cancel("from root");
              threadDone.get();
              trace.add("root done");
            });

    d.runUntilAllBlocked();
    assertTrue(d.stackTrace(), d.isDone());
    String[] expected =
        new String[] {
          "init",
          "root started",
          "root before cancel",
          "thread started",
          "thread done: from root",
          "root done",
        };
    trace.setExpected(expected);
  }

  @Test
  public void testDetachedCancellation() throws Throwable {
    trace.add("init");
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              trace.add("root started");
              CompletablePromise<Void> done = Workflow.newPromise();
              Workflow.newDetachedCancellationScope(
                  () -> {
                    Async.procedure(
                        () -> {
                          trace.add("thread started");
                          WorkflowThread.await(
                              "reason1",
                              () -> unblock1 || CancellationScope.current().isCancelRequested());
                          if (CancellationScope.current().isCancelRequested()) {
                            done.completeExceptionally(new CancellationException());
                          } else {
                            done.complete(null);
                          }
                          trace.add("await done");
                        });
                  });
              try {
                done.get();
              } catch (CancellationException e) {
                trace.add("done cancelled");
              }
              trace.add("root done");
            });
    d.runUntilAllBlocked();
    assertFalse(trace.toString(), d.isDone());
    d.cancel("I just feel like it");
    d.runUntilAllBlocked();
    assertFalse(d.isDone());
    String[] expected =
        new String[] {
          "init", "root started", "thread started",
        };
    trace.setExpected(expected);
    trace.assertExpected();
    unblock1 = true;
    d.runUntilAllBlocked();
    assertTrue(d.stackTrace(), d.isDone());
    expected =
        new String[] {
          "init", "root started", "thread started", "await done", "root done",
        };
    trace.setExpected(expected);
  }

  @Test
  public void testChild() throws Throwable {
    DeterministicRunner d =
        new DeterministicRunnerImpl(
            () -> {
              Promise<Void> async =
                  Async.procedure(
                      () -> {
                        status = "started";
                        WorkflowThread.await("reason1", () -> unblock1);
                        status = "after1";
                        WorkflowThread.await("reason2", () -> unblock2);
                        status = "done";
                      });
              async.get();
            });
    assertEquals("initial", status);
    d.runUntilAllBlocked();
    assertEquals("started", status);
    assertFalse(d.isDone());
    unblock1 = true;
    d.runUntilAllBlocked();
    assertEquals("after1", status);
    // Just check that running again doesn't make any progress.
    d.runUntilAllBlocked();
    assertEquals("after1", status);
    unblock2 = true;
    d.runUntilAllBlocked();
    assertEquals("done", status);
    assertTrue(d.isDone());
  }

  @Test
  public void testJoinTimeout() throws Throwable {
    DeterministicRunnerImpl d =
        new DeterministicRunnerImpl(
            threadPool,
            null,
            () -> currentTime, // clock override
            () -> {
              trace.add("root started");

              Promise<Void> thread =
                  Async.procedure(
                      () -> {
                        trace.add("child started");
                        WorkflowThread.await("blockForever", () -> false);
                        trace.add("child done");
                      });
              try {
                thread.get(60000, TimeUnit.MILLISECONDS);
              } catch (TimeoutException e) {
                trace.add("timeout exception");
              }
              trace.add("root done");
            });
    currentTime = 1000;
    d.runUntilAllBlocked();
    assertEquals(61000, d.getNextWakeUpTime());
    assertFalse(d.isDone());
    String[] expected =
        new String[] {
          "root started", "child started",
        };
    trace.setExpected(expected);
    trace.assertExpected();
    // Just check that running again doesn't make any progress.
    d.runUntilAllBlocked();
    assertEquals(61000, d.getNextWakeUpTime());
    currentTime = 70000;
    d.runUntilAllBlocked();
    assertFalse(d.isDone());
    expected = new String[] {"root started", "child started", "timeout exception", "root done"};
    trace.setExpected(expected);
    d.close();
  }

  private static final int CHILDREN = 10;

  private class TestChildTreeRunnable implements Functions.Proc {

    final int depth;

    private TestChildTreeRunnable(int depth) {
      this.depth = depth;
    }

    @Override
    public void apply() {
      trace.add("child " + depth + " started");
      if (depth >= CHILDREN) {
        trace.add("child " + depth + " done");
        return;
      }
      Promise<Void> thread = Async.procedure(new TestChildTreeRunnable(depth + 1));
      WorkflowThread.await("reason1", () -> unblock1);
      thread.get();
      trace.add("child " + depth + " done");
    }
  }

  @Test
  public void testChildTree() throws Throwable {
    DeterministicRunner d = new DeterministicRunnerImpl(new TestChildTreeRunnable(0)::apply);
    d.runUntilAllBlocked();
    unblock1 = true;
    d.runUntilAllBlocked();
    assertTrue(d.isDone());
    List<String> expected = new ArrayList<>();
    for (int i = 0; i <= CHILDREN; i++) {
      expected.add("child " + i + " started");
    }
    for (int i = CHILDREN; i >= 0; i--) {
      expected.add("child " + i + " done");
    }
    trace.setExpected(expected.toArray(new String[0]));
  }
}
