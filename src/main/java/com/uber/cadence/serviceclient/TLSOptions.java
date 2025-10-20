/*
 *  Modifications Copyright (c) 2017-2020 Uber Technologies Inc.
 *  Portions of the Software are attributed to Copyright (c) 2020 Temporal Technologies Inc.
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

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.File;
import java.io.InputStream;

/**
 * TLSOptions encapsulates TLS/SSL configuration for gRPC connections.
 *
 * <p>This class provides options for configuring secure connections to Cadence servers, including
 * support for custom CA certificates, mutual TLS (mTLS) with client certificates, and custom SSL
 * contexts.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple TLS with custom CA certificate
 * TLSOptions tlsOptions = TLSOptions.newBuilder()
 *     .setTrustManagerCertFile(new File("path/to/ca.crt"))
 *     .build();
 *
 * // Mutual TLS with client certificate
 * TLSOptions tlsOptions = TLSOptions.newBuilder()
 *     .setTrustManagerCertFile(new File("path/to/ca.crt"))
 *     .setClientCertFile(new File("path/to/client.crt"))
 *     .setClientKeyFile(new File("path/to/client.key"))
 *     .build();
 *
 * // Use with ClientOptions
 * ClientOptions options = ClientOptions.newBuilder()
 *     .setHost("cadence.example.com")
 *     .setPort(443)
 *     .setTLSOptions(tlsOptions)
 *     .build();
 * }</pre>
 */
public class TLSOptions {

  private final File trustManagerCertFile;
  private final InputStream trustManagerCertInputStream;
  private final File clientCertFile;
  private final InputStream clientCertInputStream;
  private final File clientKeyFile;
  private final InputStream clientKeyInputStream;
  private final String clientKeyPassword;
  private final SslContext customSslContext;
  private final boolean disableHostVerification;

  private TLSOptions(Builder builder) {
    this.trustManagerCertFile = builder.trustManagerCertFile;
    this.trustManagerCertInputStream = builder.trustManagerCertInputStream;
    this.clientCertFile = builder.clientCertFile;
    this.clientCertInputStream = builder.clientCertInputStream;
    this.clientKeyFile = builder.clientKeyFile;
    this.clientKeyInputStream = builder.clientKeyInputStream;
    this.clientKeyPassword = builder.clientKeyPassword;
    this.customSslContext = builder.customSslContext;
    this.disableHostVerification = builder.disableHostVerification;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  /** @return The trust manager certificate file (CA certificate) */
  public File getTrustManagerCertFile() {
    return trustManagerCertFile;
  }

  /** @return The trust manager certificate input stream (CA certificate) */
  public InputStream getTrustManagerCertInputStream() {
    return trustManagerCertInputStream;
  }

  /** @return The client certificate file for mutual TLS */
  public File getClientCertFile() {
    return clientCertFile;
  }

  /** @return The client certificate input stream for mutual TLS */
  public InputStream getClientCertInputStream() {
    return clientCertInputStream;
  }

  /** @return The client private key file for mutual TLS */
  public File getClientKeyFile() {
    return clientKeyFile;
  }

  /** @return The client private key input stream for mutual TLS */
  public InputStream getClientKeyInputStream() {
    return clientKeyInputStream;
  }

  /** @return The password for the client private key, if encrypted */
  public String getClientKeyPassword() {
    return clientKeyPassword;
  }

  /** @return A custom SSL context, if provided */
  public SslContext getCustomSslContext() {
    return customSslContext;
  }

  /** @return Whether to disable host verification */
  public boolean isDisableHostVerification() {
    return disableHostVerification;
  }

  /** Builder for TLSOptions */
  public static class Builder {

    private File trustManagerCertFile;
    private InputStream trustManagerCertInputStream;
    private File clientCertFile;
    private InputStream clientCertInputStream;
    private File clientKeyFile;
    private InputStream clientKeyInputStream;
    private String clientKeyPassword;
    private SslContext customSslContext;
    private boolean disableHostVerification;

    private Builder() {}

    /**
     * Sets the trust manager certificate file (CA certificate) to verify the server's certificate.
     *
     * @param trustManagerCertFile File containing the CA certificate in PEM format
     * @return this Builder
     */
    public Builder setTrustManagerCertFile(File trustManagerCertFile) {
      this.trustManagerCertFile = trustManagerCertFile;
      return this;
    }

    /**
     * Sets the trust manager certificate input stream (CA certificate) to verify the server's
     * certificate.
     *
     * @param trustManagerCertInputStream InputStream containing the CA certificate in PEM format
     * @return this Builder
     */
    public Builder setTrustManagerCertInputStream(InputStream trustManagerCertInputStream) {
      this.trustManagerCertInputStream = trustManagerCertInputStream;
      return this;
    }

    /**
     * Sets the client certificate file for mutual TLS authentication.
     *
     * @param clientCertFile File containing the client certificate in PEM format
     * @return this Builder
     */
    public Builder setClientCertFile(File clientCertFile) {
      this.clientCertFile = clientCertFile;
      return this;
    }

    /**
     * Sets the client certificate input stream for mutual TLS authentication.
     *
     * @param clientCertInputStream InputStream containing the client certificate in PEM format
     * @return this Builder
     */
    public Builder setClientCertInputStream(InputStream clientCertInputStream) {
      this.clientCertInputStream = clientCertInputStream;
      return this;
    }

    /**
     * Sets the client private key file for mutual TLS authentication.
     *
     * @param clientKeyFile File containing the client private key in PEM format
     * @return this Builder
     */
    public Builder setClientKeyFile(File clientKeyFile) {
      this.clientKeyFile = clientKeyFile;
      return this;
    }

    /**
     * Sets the client private key input stream for mutual TLS authentication.
     *
     * @param clientKeyInputStream InputStream containing the client private key in PEM format
     * @return this Builder
     */
    public Builder setClientKeyInputStream(InputStream clientKeyInputStream) {
      this.clientKeyInputStream = clientKeyInputStream;
      return this;
    }

    /**
     * Sets the password for the client private key if it is encrypted.
     *
     * @param clientKeyPassword Password for the encrypted private key
     * @return this Builder
     */
    public Builder setClientKeyPassword(String clientKeyPassword) {
      this.clientKeyPassword = clientKeyPassword;
      return this;
    }

    /**
     * Sets a custom SSL context. If provided, this will be used instead of building one from
     * certificate files.
     *
     * @param customSslContext Custom SSL context
     * @return this Builder
     */
    public Builder setCustomSslContext(SslContext customSslContext) {
      this.customSslContext = customSslContext;
      return this;
    }

    /**
     * Disables host verification. Use with caution - this reduces security by not verifying the
     * server's hostname matches its certificate.
     *
     * @param disableHostVerification true to disable host verification
     * @return this Builder
     */
    public Builder setDisableHostVerification(boolean disableHostVerification) {
      this.disableHostVerification = disableHostVerification;
      return this;
    }

    /**
     * Builds the TLSOptions.
     *
     * @return TLSOptions instance
     * @throws IllegalStateException if the configuration is invalid
     */
    public TLSOptions build() {
      // Validate configuration
      if (customSslContext == null) {
        if (trustManagerCertFile == null && trustManagerCertInputStream == null) {
          throw new IllegalStateException(
              "Either trustManagerCertFile, trustManagerCertInputStream, or customSslContext must be set");
        }
        if (trustManagerCertFile != null && trustManagerCertInputStream != null) {
          throw new IllegalStateException(
              "Cannot set both trustManagerCertFile and trustManagerCertInputStream");
        }
        // Validate mutual TLS configuration
        boolean hasClientCert = clientCertFile != null || clientCertInputStream != null;
        boolean hasClientKey = clientKeyFile != null || clientKeyInputStream != null;
        if (hasClientCert != hasClientKey) {
          throw new IllegalStateException(
              "Both client certificate and client key must be provided for mutual TLS");
        }
        if (clientCertFile != null && clientCertInputStream != null) {
          throw new IllegalStateException(
              "Cannot set both clientCertFile and clientCertInputStream");
        }
        if (clientKeyFile != null && clientKeyInputStream != null) {
          throw new IllegalStateException("Cannot set both clientKeyFile and clientKeyInputStream");
        }
      }

      return new TLSOptions(this);
    }
  }
}
