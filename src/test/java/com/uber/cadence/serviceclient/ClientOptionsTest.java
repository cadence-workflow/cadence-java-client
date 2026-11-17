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
package com.uber.cadence.serviceclient;

import com.uber.cadence.internal.compatibility.proto.serviceclient.IGrpcServiceStubs;
import com.uber.cadence.serviceclient.auth.OAuthAuthorizationProvider;
import com.uber.m3.tally.NoopScope;
import com.uber.m3.tally.Scope;
import io.grpc.*;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

public class ClientOptionsTest extends TestCase {

  public void testBuilderWithGRPC() {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
    Scope scope = new NoopScope();
    ClientOptions options =
        ClientOptions.newBuilder()
            .setAuthorizationProvider(
                new OAuthAuthorizationProvider(
                    "clientID", "clientSecret", "https://128.1.1.1", null))
            .setServiceName("serviceName")
            .setClientAppName("clientAppName")
            .setGRPCChannel(channel)
            .setRpcTimeout(TimeUnit.SECONDS.toMillis(10))
            .setRpcLongPollTimeout(TimeUnit.SECONDS.toMillis(10))
            .setQueryRpcTimeout(TimeUnit.SECONDS.toMillis(10))
            .setListArchivedWorkflowRpcTimeout(TimeUnit.SECONDS.toMillis(10))
            .setMetricsScope(scope)
            .setTransportHeaders(Collections.singletonMap("transportKey", "value"))
            .setHeaders(Collections.singletonMap("key", "value"))
            .setIsolationGroup("isolation-group")
            .build();
    assertEquals("serviceName", options.getServiceName());
    assertEquals("clientAppName", options.getClientAppName());
    assertEquals(channel, options.getGRPCChannel());
    assertEquals(TimeUnit.SECONDS.toMillis(10), options.getRpcTimeoutMillis());
    assertEquals(TimeUnit.SECONDS.toMillis(10), options.getRpcLongPollTimeoutMillis());
    assertEquals(TimeUnit.SECONDS.toMillis(10), options.getRpcQueryTimeoutMillis());
    assertEquals(TimeUnit.SECONDS.toMillis(10), options.getRpcListArchivedWorkflowTimeoutMillis());
    assertEquals(scope, options.getMetricsScope());
    assertEquals("value", options.getTransportHeaders().get("transportKey"));
    assertEquals("value", options.getHeaders().get("key"));
    assertEquals("isolation-group", options.getIsolationGroup());
  }

  public void testDefaultPortIsLocalServerGrpcPort() {
    assertEquals(
        IGrpcServiceStubs.DEFAULT_LOCAL_CADENCE_SERVER_GRPC_PORT,
        ClientOptions.defaultInstance().getPort());
    assertEquals(
        IGrpcServiceStubs.DEFAULT_LOCAL_CADENCE_SERVER_GRPC_PORT,
        ClientOptions.newBuilder().build().getPort());
  }
}
