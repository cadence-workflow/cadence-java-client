/**
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * <p>Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * <p>http://aws.amazon.com/apache2.0
 *
 * <p>or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.uber.cadence.internal.worker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import org.junit.Test;

public class LocalActivityPollTaskTest {

  // Mirrors the private QUEUE_SIZE in LocalActivityPollTask.
  private static final int QUEUE_SIZE = 1000;

  private static LocalActivityWorker.Task newTask() {
    return new LocalActivityWorker.Task(null, null, 0, null, null);
  }

  @Test
  public void applyReturnsFalseWhenQueueIsFull() {
    LocalActivityPollTask poller = new LocalActivityPollTask();

    // Fill the bounded queue to capacity; while there is room each offer succeeds immediately.
    for (int i = 0; i < QUEUE_SIZE; i++) {
      assertTrue(poller.apply(newTask(), Duration.ofMillis(10)));
    }

    // Queue is now full. apply must report failure (not silently drop the task and report
    // success) so the caller can force a new decision task instead of waiting for a local
    // activity completion that never arrives. It should also wait up to maxWaitAllowed first.
    long start = System.currentTimeMillis();
    assertFalse(poller.apply(newTask(), Duration.ofMillis(50)));
    long waitedMillis = System.currentTimeMillis() - start;
    assertTrue("apply should wait up to maxWaitAllowed before giving up", waitedMillis >= 40);

    // Draining a slot frees capacity, so the next offer succeeds again.
    poller.poll();
    assertTrue(poller.apply(newTask(), Duration.ofMillis(10)));
  }

  @Test
  public void applyReturnsFalseAndRestoresInterruptWhenInterrupted() {
    LocalActivityPollTask poller = new LocalActivityPollTask();

    // A set interrupt status makes the interruptible offer throw immediately.
    Thread.currentThread().interrupt();
    boolean applied = poller.apply(newTask(), Duration.ofMillis(10));

    assertFalse(applied);
    // Thread.interrupted() returns true only if apply restored the interrupt flag; it also
    // clears the status so it does not leak into other tests.
    assertTrue("interrupt status should be restored", Thread.interrupted());
  }
}
