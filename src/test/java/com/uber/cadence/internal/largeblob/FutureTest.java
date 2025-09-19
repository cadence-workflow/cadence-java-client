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

package com.uber.cadence.internal.largeblob;

import com.uber.cadence.largeblob.Configuration;
import com.uber.cadence.largeblob.Future;
import com.uber.cadence.largeblob.Storage;
import com.uber.cadence.largeblob.impl.InMemoryStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class FutureTest {

  private final Storage storage = new InMemoryStorage();

  @Test
  public void testPutSmallElement() throws Exception {
    Configuration config = Configuration.newBuilder().setMaxBytes(20L).setStorage(storage).build();
    Future<String> future = new Future<String>("testValue", config);

    assertEquals("testValue", future.get());
    assertNull(future.getUrl());
  }

  @Test
  public void testLargerValuesGetPutInStorage() throws Exception {
    Configuration config = Configuration.newBuilder().setMaxBytes(20L).setStorage(storage).build();
    String testValue = "testValuetestValuetestValue";
    Future future = new Future(testValue, config);

    assertEquals("testValuetestValuetestValue", future.get());
    assertNotNull(storage.get(future.getUrl()));
  }

  @Test
  public void testDeleteValueFromStorage() throws Exception {
    Configuration config = Configuration.newBuilder().setMaxBytes(20L).setStorage(storage).build();
    String testValue = "testValuetestValuetestValue";
    Future<String> future = new Future<>(testValue, config);

    assertEquals("testValuetestValuetestValue", future.get());
    assertEquals(
        "testValuetestValuetestValue", config.getDataConverter().fromData(storage.get(future.getUrl()), String.class, String.class));

    future.delete();

    assertNull(storage.get("test"));

    assertNull(future.get());
  }

  @Test
  public void testSmallValueIsDelete() throws Exception {
    Configuration config = Configuration.newBuilder().setMaxBytes(20L).setStorage(storage).build();
    Future<String> future = new Future<>("testValue", config);

    assertEquals("testValue", future.get());
    assertNull(storage.get(future.getUrl()));

    future.delete();
    assertNull(future.get());
  }

  @Test
  public void getWorksWhenInitialisingWithEncodedData() throws Exception {
    Configuration configuration = Configuration.newBuilder().setStorage(storage).build();
    Future<String> future = new Future<>(configuration, String.class, "testValue".getBytes(StandardCharsets.UTF_8));

    assertEquals("testValue", future.get());
  }

  @Test
  public void getWorksWhenInitialisingWithUrl() throws Exception {
    Configuration configuration = Configuration.newBuilder().setStorage(storage).build();
    Future<String> future = new Future<>(configuration, String.class, "test");

    assertNull(future.get());

    storage.put("test", "testValue".getBytes(StandardCharsets.UTF_8));

    assertEquals("testValue", future.get());
  }
}
