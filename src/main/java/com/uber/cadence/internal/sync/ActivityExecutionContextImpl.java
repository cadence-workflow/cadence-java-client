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

import com.uber.cadence.BadRequestError;
import com.uber.cadence.EntityNotExistsError;
import com.uber.cadence.RecordActivityTaskHeartbeatRequest;
import com.uber.cadence.RecordActivityTaskHeartbeatResponse;
import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.activity.ActivityTask;
import com.uber.cadence.client.ActivityCancelledException;
import com.uber.cadence.client.ActivityCompletionException;
import com.uber.cadence.client.ActivityCompletionFailureException;
import com.uber.cadence.client.ActivityNotExistsException;
import com.uber.cadence.converter.DataConverter;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of an {@link ActivityExecutionContext}.
 *
 * @author fateev, suskin
 * @see ActivityExecutionContext
 */
class ActivityExecutionContextImpl implements ActivityExecutionContext {

  private static final Logger log = LoggerFactory.getLogger(ActivityExecutionContextImpl.class);
  private static final long HEARTBEAT_RETRY_WAIT_MILLIS = 1000;

  private final IWorkflowService service;
  private final String domain;
  private final ActivityTask task;
  private final DataConverter dataConverter;
  private boolean doNotCompleteOnReturn;
  private final long heartbeatIntervalMillis;
  private Optional<Object> lastDetails;
  private final ScheduledExecutorService heartbeatExecutor;
  private Lock lock = new ReentrantLock();
  private ScheduledFuture future;
  private ActivityCompletionException lastException;

  /** Create an ActivityExecutionContextImpl with the given attributes. */
  ActivityExecutionContextImpl(
      IWorkflowService service,
      String domain,
      ActivityTask task,
      DataConverter dataConverter,
      ScheduledExecutorService heartbeatExecutor) {
    this.domain = domain;
    this.service = service;
    this.task = task;
    this.dataConverter = dataConverter;
    this.heartbeatIntervalMillis = (long) (0.8 * task.getHeartbeatTimeout().toMillis());
    this.heartbeatExecutor = heartbeatExecutor;
  }

  /** @see ActivityExecutionContext#recordActivityHeartbeat(Object) */
  @Override
  public void recordActivityHeartbeat(Object details) throws ActivityCompletionException {
    lock.lock();
    try {
      // always set lastDetail. Successful heartbeat will clear it.
      lastDetails = details == null ? Optional.empty() : Optional.of(details);

      // Only do sync heartbeat if there is no such call scheduled.
      if (future == null) {
        doHeartBeat(details);
      }

      if (lastException != null) {
        throw lastException;
      }
    } finally {
      lock.unlock();
    }
  }

  private void doHeartBeat(Object details) {
    long nextHeartbeatDelay;
    try {
      sendHeartbeatRequest(details);
      // Clear lastDetails only if heartbeat succeeds.
      lastDetails = null;
      nextHeartbeatDelay = heartbeatIntervalMillis;
    } catch (TException e) {
      // Not rethrowing to not fail activity implementation on intermittent connection or Cadence
      // errors.
      log.warn("Heartbeat failed.", e);
      nextHeartbeatDelay = HEARTBEAT_RETRY_WAIT_MILLIS;
    }

    scheduleNextHeartbeat(nextHeartbeatDelay);
  }

  private void scheduleNextHeartbeat(long delay) {
    future =
        heartbeatExecutor.schedule(
            () -> {
              lock.lock();
              try {
                if (lastDetails != null) {
                  Object details = lastDetails.orElse(null);
                  doHeartBeat(details);
                } else {
                  future = null;
                }
              } finally {
                lock.unlock();
              }
            },
            delay,
            TimeUnit.MILLISECONDS);
  }

  private void sendHeartbeatRequest(Object details) throws TException {
    RecordActivityTaskHeartbeatRequest r = new RecordActivityTaskHeartbeatRequest();
    r.setTaskToken(task.getTaskToken());
    byte[] serialized = dataConverter.toData(details);
    r.setDetails(serialized);
    RecordActivityTaskHeartbeatResponse status;
    try {
      status = service.RecordActivityTaskHeartbeat(r);
      if (status.isCancelRequested()) {
        lastException = new ActivityCancelledException(task);
      } else {
        lastException = null;
      }
    } catch (EntityNotExistsError e) {
      lastException = new ActivityNotExistsException(task, e);
    } catch (BadRequestError e) {
      lastException = new ActivityCompletionFailureException(task, e);
    }
  }

  @Override
  public void doNotCompleteOnReturn() {
    doNotCompleteOnReturn = true;
  }

  @Override
  public boolean isDoNotCompleteOnReturn() {
    return doNotCompleteOnReturn;
  }

  /** @see ActivityExecutionContext#getTask() */
  @Override
  public ActivityTask getTask() {
    return task;
  }

  /** @see ActivityExecutionContext#getService() */
  @Override
  public IWorkflowService getService() {
    return service;
  }

  @Override
  public byte[] getTaskToken() {
    return task.getTaskToken();
  }

  @Override
  public WorkflowExecution getWorkflowExecution() {
    return task.getWorkflowExecution();
  }

  @Override
  public String getDomain() {
    return domain;
  }
}
