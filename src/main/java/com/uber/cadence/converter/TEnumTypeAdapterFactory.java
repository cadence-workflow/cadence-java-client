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

package com.uber.cadence.converter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import org.apache.thrift.TEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special handling of TEnum serialization and deserialization. This is to support for inline TEnum
 * fields in Java class. The default gson serde serialize the TEnum with its String name
 * representation, this adapter serialize the TEnum class with its int representation.
 */
public class TEnumTypeAdapterFactory implements TypeAdapterFactory {

  private static final Logger logger = LoggerFactory.getLogger(TEnumTypeAdapterFactory.class);

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    // this class only serializes 'TEnum' and its subtypes
    if (!TEnum.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }
    TypeAdapter<T> result =
        new TypeAdapter<T>() {
          @Override
          public void write(JsonWriter jsonWriter, T value) throws IOException {
            logger.warn(
                "Thrift message will no longer be supported for data conversion in cadence-java-client V4, payload class name {}",
                value.getClass().getName());
            jsonWriter.value(((TEnum) value).getValue());
          }

          @Override
          public T read(JsonReader jsonReader) throws IOException {
            int value = jsonReader.nextInt();
            try {
              Method m = (typeToken.getRawType().getDeclaredMethod("findByValue", Integer.TYPE));
              @SuppressWarnings("unchecked")
              T instance = (T) m.invoke(null, value);
              logger.warn(
                  "Thrift message will no longer be supported for data conversion in cadence-java-client V4, payload class name {}",
                  instance.getClass().getName());
              return instance;
            } catch (Exception e) {
              throw new DataConverterException("Failed to deserilize TEnum", e);
            }
          }
        }.nullSafe();
    return result;
  }
}
