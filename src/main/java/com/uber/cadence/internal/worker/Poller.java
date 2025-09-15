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

package com.uber.cadence.internal.worker;

import com.uber.cadence.BaseError;
import com.uber.cadence.internal.common.BackoffThrottler;
import com.uber.cadence.internal.common.InternalUtils;
import com.uber.cadence.internal.metrics.MetricsType;
import com.uber.cadence.internal.worker.autoscaler.AutoScaler;
import com.uber.cadence.internal.worker.autoscaler.AutoScalerFactory;
import com.uber.cadence.worker.ExecutorWrapper;
import com.uber.m3.tally.Scope;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Poller<T> implements SuspendableWorker {

  public interface PollTask<TT> {
    TT poll() throws BaseError;
  }

  interface ThrowingRunnable {
    void run() throws Throwable;
  }

  private final String identity;
  private final ShutdownableTaskExecutor<T> taskExecutor;
  private final PollTask<T> pollTask;
  private final PollerOptions pollerOptions;
  private static final Logger log = LoggerFactory.getLogger(Poller.class);
  private ThreadPoolExecutor pollExecutor;
  private final Scope metricsScope;

  private final AtomicReference<CountDownLatch> suspendLatch = new AtomicReference<>();

  private BackoffThrottler pollBackoffThrottler;
  private Throttler pollRateThrottler;

  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
      (t, e) -> {
        if (e instanceof TTransportException) {
          TTransportException te = (TTransportException) e;
          if (te.getType() == TTransportException.TIMED_OUT) {
            log.warn("Failure in thread " + t.getName(), e);
            return;
          }
        }

        log.error("Failure in thread " + t.getName(), e);
      };

  private final AutoScaler pollerAutoScaler;

  private final ExecutorWrapper executorWrapper;

  public Poller(
      String identity,
      PollTask<T> pollTask,
      ShutdownableTaskExecutor<T> taskExecutor,
      PollerOptions pollerOptions,
      Scope metricsScope,
      ExecutorWrapper executorWrapper) {
    Objects.requireNonNull(identity, "identity cannot be null");
    Objects.requireNonNull(pollTask, "poll service should not be null");
    Objects.requireNonNull(taskExecutor, "taskExecutor should not be null");
    Objects.requireNonNull(pollerOptions, "pollerOptions should not be null");
    Objects.requireNonNull(metricsScope, "metricsScope should not be null");
    Objects.requireNonNull(metricsScope, "executorWrapper should not be null");

    this.identity = identity;
    this.pollTask = pollTask;
    this.taskExecutor = taskExecutor;
    this.pollerOptions = pollerOptions;
    this.metricsScope = metricsScope;
    this.pollerAutoScaler = AutoScalerFactory.getInstance().createAutoScaler(pollerOptions);
    this.executorWrapper = executorWrapper;
  }

  @Override
  public void start() {
    if (log.isDebugEnabled()) {
      log.debug("start(): " + toString());
    }
    if (pollerOptions.getMaximumPollRatePerSecond() > 0.0) {
      pollRateThrottler =
          new Throttler(
              "poller",
              pollerOptions.getMaximumPollRatePerSecond(),
              pollerOptions.getMaximumPollRateIntervalMilliseconds());
    }

    // It is important to pass blocking queue of at least options.getPollThreadCount() capacity.
    // As task enqueues next task the buffering is needed to queue task until the previous one
    // releases a thread.
    pollExecutor =
        executorWrapper.wrap(
            new ThreadPoolExecutor(
                pollerOptions.getPollThreadCount(),
                pollerOptions.getPollThreadCount(),
                1,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(pollerOptions.getPollThreadCount())));
    pollExecutor.setThreadFactory(
        new ExecutorThreadFactory(
            pollerOptions.getPollThreadNamePrefix(), pollerOptions.getUncaughtExceptionHandler()));

    pollBackoffThrottler =
        new BackoffThrottler(
            pollerOptions.getPollBackoffInitialInterval(),
            pollerOptions.getPollBackoffMaximumInterval(),
            pollerOptions.getPollBackoffCoefficient());
    for (int i = 0; i < pollerOptions.getPollThreadCount(); i++) {
      pollExecutor.execute(new PollLoopTask(new PollExecutionTask()));
      metricsScope.counter(MetricsType.POLLER_START_COUNTER).inc(1);
    }

    pollerAutoScaler.start();
  }

  @Override
  public boolean isStarted() {
    return pollExecutor != null;
  }

  @Override
  public boolean isShutdown() {
    return pollExecutor.isShutdown() && taskExecutor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return pollExecutor.isTerminated() && taskExecutor.isTerminated();
  }

  @Override
  public void shutdown() {
    log.debug("shutdown");
    if (!isStarted()) {
      return;
    }
    // shutdownNow and then await to stop long polling and ensure that no new tasks
    // are dispatched to the taskExecutor.
    pollExecutor.shutdownNow();
    try {
      pollExecutor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
    }
    taskExecutor.shutdown();
    pollerAutoScaler.stop();
  }

  @Override
  public void shutdownNow() {
    if (log.isDebugEnabled()) {
      log.debug("shutdownNow poller=" + this.pollerOptions.getPollThreadNamePrefix());
    }
    if (!isStarted()) {
      return;
    }
    pollExecutor.shutdownNow();
    taskExecutor.shutdownNow();
  }

  @Override
  public void awaitTermination(long timeout, TimeUnit unit) {
    if (!isStarted()) {
      return;
    }
    long timeoutMillis = unit.toMillis(timeout);
    timeoutMillis = InternalUtils.awaitTermination(pollExecutor, timeoutMillis);
    InternalUtils.awaitTermination(taskExecutor, timeoutMillis);
  }

  @Override
  public void suspendPolling() {
    log.info("suspendPolling");
    suspendLatch.set(new CountDownLatch(1));
  }

  @Override
  public void resumePolling() {
    log.info("resumePolling");
    CountDownLatch existing = suspendLatch.getAndSet(null);
    if (existing != null) {
      existing.countDown();
    }
  }

  @Override
  public boolean isSuspended() {
    return suspendLatch.get() != null;
  }

  @Override
  public String toString() {
    return "Poller{" + "options=" + pollerOptions + ", identity=" + identity + '}';
  }

  private class PollLoopTask implements Runnable {

    private final Poller.ThrowingRunnable task;

    PollLoopTask(Poller.ThrowingRunnable task) {
      this.task = task;
    }

    @Override
    public void run() {
      try {
        if (pollExecutor.isTerminating()) {
          return;
        }
        pollBackoffThrottler.throttle();
        if (pollExecutor.isTerminating()) {
          return;
        }
        if (pollRateThrottler != null) {
          pollRateThrottler.throttle();
        }

        CountDownLatch suspender = Poller.this.suspendLatch.get();
        if (suspender != null) {
          if (log.isDebugEnabled()) {
            log.debug("poll task suspending latchCount=" + suspender.getCount());
          }
          suspender.await();
        }

        if (pollExecutor.isTerminating()) {
          return;
        }
        task.run();
        pollBackoffThrottler.success();
      } catch (Throwable e) {
        pollBackoffThrottler.failure();
        if (!(e.getCause() instanceof InterruptedException)) {
          uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
        }
      } finally {
        // Resubmit itself back to pollExecutor
        if (!pollExecutor.isTerminating()) {
          pollExecutor.execute(this);
        } else {
          log.debug("poll loop done");
        }
      }
    }
  }

  private class PollExecutionTask implements Poller.ThrowingRunnable {
    private static final int EXECUTOR_CAPACITY_CHECK_INTERVAL_MS = 100;
    private static final int EXECUTOR_CAPACITY_CHECK_OFFSET_MS = 10;

    PollExecutionTask() {}

    @Override
    public void run() throws Exception {
      try {
        pollerAutoScaler.acquire();
        try {
          T task = pollTask.poll();
          if (task == null) {
            pollerAutoScaler.increaseNoopPollCount();
            return;
          }

          pollerAutoScaler.increaseActionablePollCount();
          taskExecutor.process(task);
        } finally {
          checkIfTaskHasExecutorHasCapacity();
        }
      } finally {
        pollerAutoScaler.release();
      }
    }

    private void checkIfTaskHasExecutorHasCapacity() {
      if (pollerOptions.getPollOnlyIfExecutorHasCapacity()) {
        while (true) {
          // sleep to avoid racing condition
          try {
            Thread.sleep(EXECUTOR_CAPACITY_CHECK_OFFSET_MS);
          } catch (InterruptedException ignored) {
          }
          if (taskExecutor.hasCapacity()) {
            break;
          } else {
            // sleep to avoid busy loop
            try {
              Thread.sleep(EXECUTOR_CAPACITY_CHECK_INTERVAL_MS);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }
    }
  }
}
