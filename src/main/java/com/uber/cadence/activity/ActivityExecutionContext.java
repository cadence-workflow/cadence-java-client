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

package com.uber.cadence.activity;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.client.ActivityCompletionClient;
import com.uber.cadence.client.ActivityCompletionException;
import com.uber.cadence.serviceclient.IWorkflowService;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Context object passed to an activity implementation. Use {@link Activity#getExecutionContext()}
 * from an activity implementation to access.
 */
public interface ActivityExecutionContext {

  /** Information about activity invocation and the caller workflow */
  ActivityTask getInfo();

  /**
   * Use to notify Cadence service that activity execution is alive.
   *
   * @param details In case of activity timeout details are returned as a field of the exception
   *     thrown.
   * @throws ActivityCompletionException Indicates that activity cancellation was requested by the
   *     workflow or any other reason for activity to stop execution. Should be rethrown from
   *     activity implementation to indicate successful cancellation.
   */
  <V> void heartbeat(V details) throws ActivityCompletionException;

  /**
   * Extracts heartbeat details from the last failed attempt. This is used in combination with retry
   * options. An activity could be scheduled with an optional {@link
   * com.uber.cadence.common.RetryOptions} on {@link ActivityOptions}. If an activity failed then
   * the server would attempt to dispatch another activity task to retry according to the retry
   * options. If there was heartbeat details reported by the activity from the failed attempt, the
   * details would be delivered along with the activity task for the retry attempt. The activity
   * could extract the details by {@link #getHeartbeatDetails(Class)}() and resume from the
   * progress.
   *
   * @param detailsClass type of the heartbeat details
   */
  <V> Optional<V> getHeartbeatDetails(Class<V> detailsClass);

  /**
   * Extracts heartbeat details from the last failed attempt. This is used in combination with retry
   * options. An activity could be scheduled with an optional {@link
   * com.uber.cadence.common.RetryOptions} on {@link ActivityOptions}. If an activity failed then
   * the server would attempt to dispatch another activity task to retry according to the retry
   * options. If there was heartbeat details reported by the activity from the failed attempt, the
   * details would be delivered along with the activity task for the retry attempt. The activity
   * could extract the details by {@link #getHeartbeatDetails(Class)}() and resume from the
   * progress.
   *
   * @param detailsClass type of the heartbeat details
   * @param detailsType type including generic information of the heartbeat details
   */
  <V> Optional<V> getHeartbeatDetails(Class<V> detailsClass, Type detailsType);

  /**
   * A correlation token that can be used to complete the activity asynchronously through {@link
   * com.uber.cadence.client.ActivityCompletionClient#complete(byte[], Object)}.
   */
  byte[] getTaskToken();

  /**
   * If this method is called during an activity execution then activity is not going to complete
   * when its method returns. It is expected to be completed asynchronously using {@link
   * com.uber.cadence.client.ActivityCompletionClient}.
   */
  void doNotCompleteOnReturn();

  boolean isDoNotCompleteOnReturn();

  /**
   * Use for asynchronous completion within the activity worker process. Calling this method
   * prevents automatic activity completion on return and returns a client that can be used to
   * complete the activity asynchronously.
   */
  ActivityCompletionClient useLocalManualCompletion();

  boolean isUseLocalManualCompletion();

  /** @return workfow execution that requested the activity execution */
  WorkflowExecution getWorkflowExecution();

  /**
   * @return an instance of the Cadence service client that is the same used by the invoked activity
   *     worker.
   */
  IWorkflowService getService();

  /** @return the domain of the activity execution */
  String getDomain();
}
