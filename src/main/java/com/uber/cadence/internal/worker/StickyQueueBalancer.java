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
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class StickyQueueBalancer {

  private final int pollersCount;
  private final boolean stickyQueueEnabled;
  private int stickyPollers = 0;
  private int normalPollers = 0;
  private long stickyBacklogSize = 0;

  public StickyQueueBalancer(int pollersCount, boolean stickyQueueEnabled) {
    this.pollersCount = pollersCount;
    this.stickyQueueEnabled = stickyQueueEnabled;
  }

  /** @return task list kind that should be used for the next poll */
  public synchronized TaskListKind makePoll() {
    if (stickyQueueEnabled) {
      // If pollersCount >= stickyBacklogSize > 0 we want to go back to a normal ratio to avoid a
      // situation that too many pollers (all of them in the worst case) will open only sticky queue
      // polls observing a stickyBacklogSize == 1 for example (which actually can be 0 already at
      // that moment) and get stuck causing dip in worker load.
      if (stickyBacklogSize > pollersCount || stickyPollers <= normalPollers) {
        stickyPollers++;
        return TaskListKind.STICKY;
      }
    }
    normalPollers++;
    return TaskListKind.NORMAL;
  }

  /** @param taskListKind what kind of task list poll was just finished */
  public synchronized void finishPoll(TaskListKind taskListKind) {
    switch (taskListKind) {
      case NORMAL:
        normalPollers--;
        break;
      case STICKY:
        stickyPollers--;
        break;
      default:
        throw new IllegalArgumentException("Invalid task list kind: " + taskListKind);
    }
  }

  /**
   * @param taskListKind what kind of task list poll was just finished
   * @param backlogSize backlog size from the poll response
   */
  public synchronized void finishPoll(TaskListKind taskListKind, long backlogSize) {
    finishPoll(taskListKind);
    if (TaskListKind.STICKY.equals(taskListKind)) {
      stickyBacklogSize = backlogSize;
    }
  }
}
