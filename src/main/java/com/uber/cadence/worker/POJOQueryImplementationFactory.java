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
package com.uber.cadence.worker;

import com.google.common.reflect.TypeToken;
import com.uber.cadence.DataConverter;
import com.uber.cadence.common.FlowHelpers;
import com.uber.cadence.internal.dispatcher.Functions;
import com.uber.cadence.internal.dispatcher.QueryMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class POJOQueryImplementationFactory {

    private static final Log log = LogFactory.getLog(POJOQueryImplementationFactory.class);

    private static final byte[] EMPTY_BLOB = {};
    private final DataConverter dataConverter;
    private final Map<String, POJOQueryImplementation> queries = Collections.synchronizedMap(new HashMap<>());

    public POJOQueryImplementationFactory(DataConverter dataConverter, Object queryImplementation) {
        this.dataConverter = dataConverter;
        Class<?> cls = queryImplementation.getClass();
        TypeToken<?>.TypeSet interfaces = TypeToken.of(cls).getTypes().interfaces();
        if (interfaces.isEmpty()) {
            throw new IllegalArgumentException(cls.getName() + " must implement at least one interface");
        }
        for (TypeToken<?> i : interfaces) {
            for (Method method : i.getRawType().getMethods()) {
                QueryMethod queryMethod = method.getAnnotation(QueryMethod.class);
                if (queryMethod != null) {
                    POJOQueryImplementation implementation = new POJOQueryImplementation(method, queryImplementation);
                    String name = queryMethod.name();
                    if (name.isEmpty()) {
                        name = FlowHelpers.getSimpleName(method);
                    }
                    queries.put(name, implementation);
                }
            }
        }
    }

    public Set<String> getQueryFunctionNames() {
        return queries.keySet();
    }

    public POJOQueryImplementation getQueryFunction(String queryType) {
        return queries.get(queryType);
    }

    private class POJOQueryImplementation implements Functions.Func1<byte[], byte[]> {
        private final Method method;
        private final Object activity;

        public POJOQueryImplementation(Method method, Object activity) {
            this.method = method;
            this.activity = activity;
        }

        @Override
        public byte[] apply(byte[] input) throws Exception {
            Object[] args = dataConverter.fromData(input, Object[].class);
            Object result = method.invoke(activity, args);
            if (method.getReturnType() == Void.TYPE) {
                return EMPTY_BLOB;
            }
            log.info("POJO query result=" + result);
            return dataConverter.toData(result);
        }
    }
}
