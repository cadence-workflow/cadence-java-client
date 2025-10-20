/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.uber.cadence.serviceclient;

import static org.junit.Assert.*;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import org.junit.Test;

public class TLSOptionsTest {

  @Test
  public void testBuilderWithTrustManagerFile() {
    File caCert = new File("path/to/ca.crt");
    TLSOptions tlsOptions = TLSOptions.newBuilder().setTrustManagerCertFile(caCert).build();

    assertEquals(caCert, tlsOptions.getTrustManagerCertFile());
    assertNull(tlsOptions.getTrustManagerCertInputStream());
    assertNull(tlsOptions.getClientCertFile());
    assertNull(tlsOptions.getClientKeyFile());
    assertFalse(tlsOptions.isDisableHostVerification());
  }

  @Test
  public void testBuilderWithTrustManagerInputStream() {
    InputStream caCertStream = new ByteArrayInputStream("test".getBytes());
    TLSOptions tlsOptions =
        TLSOptions.newBuilder().setTrustManagerCertInputStream(caCertStream).build();

    assertNull(tlsOptions.getTrustManagerCertFile());
    assertEquals(caCertStream, tlsOptions.getTrustManagerCertInputStream());
  }

  @Test
  public void testBuilderWithMutualTLS() {
    File caCert = new File("path/to/ca.crt");
    File clientCert = new File("path/to/client.crt");
    File clientKey = new File("path/to/client.key");

    TLSOptions tlsOptions =
        TLSOptions.newBuilder()
            .setTrustManagerCertFile(caCert)
            .setClientCertFile(clientCert)
            .setClientKeyFile(clientKey)
            .build();

    assertEquals(caCert, tlsOptions.getTrustManagerCertFile());
    assertEquals(clientCert, tlsOptions.getClientCertFile());
    assertEquals(clientKey, tlsOptions.getClientKeyFile());
    assertNull(tlsOptions.getClientKeyPassword());
  }

  @Test
  public void testBuilderWithEncryptedClientKey() {
    File caCert = new File("path/to/ca.crt");
    File clientCert = new File("path/to/client.crt");
    File clientKey = new File("path/to/client.key");
    String password = "secret";

    TLSOptions tlsOptions =
        TLSOptions.newBuilder()
            .setTrustManagerCertFile(caCert)
            .setClientCertFile(clientCert)
            .setClientKeyFile(clientKey)
            .setClientKeyPassword(password)
            .build();

    assertEquals(password, tlsOptions.getClientKeyPassword());
  }

  @Test
  public void testBuilderWithCustomSslContext() throws SSLException {
    SslContext customContext = GrpcSslContexts.forClient().build();
    TLSOptions tlsOptions = TLSOptions.newBuilder().setCustomSslContext(customContext).build();

    assertEquals(customContext, tlsOptions.getCustomSslContext());
  }

  @Test
  public void testBuilderWithDisableHostVerification() {
    File caCert = new File("path/to/ca.crt");
    TLSOptions tlsOptions =
        TLSOptions.newBuilder()
            .setTrustManagerCertFile(caCert)
            .setDisableHostVerification(true)
            .build();

    assertTrue(tlsOptions.isDisableHostVerification());
  }

  @Test(expected = IllegalStateException.class)
  public void testBuilderFailsWithoutTrustManager() {
    TLSOptions.newBuilder().build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuilderFailsWithBothTrustManagerFileAndStream() {
    File caCert = new File("path/to/ca.crt");
    InputStream caCertStream = new ByteArrayInputStream("test".getBytes());

    TLSOptions.newBuilder()
        .setTrustManagerCertFile(caCert)
        .setTrustManagerCertInputStream(caCertStream)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuilderFailsWithClientCertButNoKey() {
    File caCert = new File("path/to/ca.crt");
    File clientCert = new File("path/to/client.crt");

    TLSOptions.newBuilder().setTrustManagerCertFile(caCert).setClientCertFile(clientCert).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuilderFailsWithClientKeyButNoCert() {
    File caCert = new File("path/to/ca.crt");
    File clientKey = new File("path/to/client.key");

    TLSOptions.newBuilder().setTrustManagerCertFile(caCert).setClientKeyFile(clientKey).build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuilderFailsWithBothClientCertFileAndStream() {
    File caCert = new File("path/to/ca.crt");
    File clientCert = new File("path/to/client.crt");
    InputStream clientCertStream = new ByteArrayInputStream("test".getBytes());
    File clientKey = new File("path/to/client.key");

    TLSOptions.newBuilder()
        .setTrustManagerCertFile(caCert)
        .setClientCertFile(clientCert)
        .setClientCertInputStream(clientCertStream)
        .setClientKeyFile(clientKey)
        .build();
  }
}
