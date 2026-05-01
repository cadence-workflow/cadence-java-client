/*
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

import com.uber.cadence.TaskListKind;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class StickyQueueBalancer {

  private final int pollersCount;
  private final boolean stickyQueueEnabled;
  private final AtomicInteger stickyPollers = new AtomicInteger(0);
  private final AtomicInteger normalPollers = new AtomicInteger(0);
  private volatile long stickyBacklogSize = 0;

  public StickyQueueBalancer(int pollersCount, boolean stickyQueueEnabled) {
    this.pollersCount = pollersCount;
    this.stickyQueueEnabled = stickyQueueEnabled;
  }

  /** @return task list kind that should be used for the next poll */
  public TaskListKind makePoll() {
    if (stickyQueueEnabled) {
      if (stickyBacklogSize > pollersCount || stickyPollers.get() <= normalPollers.get()) {
        stickyPollers.incrementAndGet();
        return TaskListKind.STICKY;
      }
    }
    normalPollers.incrementAndGet();
    return TaskListKind.NORMAL;
  }

  /** @param taskListKind what kind of task list poll was just finished */
  public void finishPoll(TaskListKind taskListKind) {
    switch (taskListKind) {
      case NORMAL:
        normalPollers.decrementAndGet();
        break;
      case STICKY:
        stickyPollers.decrementAndGet();
        break;
      default:
        throw new IllegalArgumentException("Invalid task list kind: " + taskListKind);
    }
  }

  /**
   * @param taskListKind what kind of task list poll was just finished
   * @param backlogSize backlog size from the poll response
   */
  public void finishPoll(TaskListKind taskListKind, long backlogSize) {
    finishPoll(taskListKind);
    if (TaskListKind.STICKY.equals(taskListKind)) {
      stickyBacklogSize = backlogSize;
    }
  }
}
