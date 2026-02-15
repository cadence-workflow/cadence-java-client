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

package com.uber.cadence.largeblob;

import com.uber.cadence.converter.DataConverterException;

import java.io.IOException;

/**
 * Future is used for passing around potentially large parameters between activities. Future can never be used in Workflow code because it uses an external storage and that will make
 * workflow code non deterministic. Small amounts of data are stored in the future instance itself, while larger amounts of data are stored in an external storage.
 */
public class Future<T> {

  private byte[] encoded;
  private String url;
  private final Configuration config;
  private final Class<T> clazz;

  public Future(Configuration config, Class<T> clazz, byte[] encoded) {
    this.config = config;
    this.encoded = encoded;
    this.clazz = clazz;
  }

  public Future(Configuration config, Class<T> clazz, String url) {
    this.url = url;
    this.config = config;
    this.clazz = clazz;
  }

  public Future(T obj, Configuration configuration) throws IOException {
    byte[] bytes;
    try {
      bytes = configuration.getDataConverter().toData(obj);
    } catch (DataConverterException e) {
      throw new IOException(e);
    }

    this.config = configuration;
    this.clazz = (Class<T>) obj.getClass();
    if (bytes.length <= configuration.getMaxBytes()) {
      this.encoded = bytes;
    } else {
      this.url = configuration.getStorage().put(bytes);
    }
  }

  public T get() throws IOException {
    if (encoded != null) {
      return config.getDataConverter().fromData(encoded, clazz, clazz);
    }

    if (url != null) {
      return config .getDataConverter().fromData(config.getStorage().get(url), clazz, clazz);
    }

    return null;
  }

  public void delete() throws IOException {
    if (this.encoded == null) {
      config.getStorage().delete(url);
    } else {
      this.encoded = null;
    }
  }

  public byte[] getEncoded() {
    return encoded;
  }

  public String getUrl() {
    return url;
  }
}
