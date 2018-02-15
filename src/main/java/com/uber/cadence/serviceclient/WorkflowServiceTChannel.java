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
package com.uber.cadence.serviceclient;

import com.google.common.collect.ImmutableMap;
import com.uber.cadence.*;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.SubChannel;
import com.uber.tchannel.api.TChannel;
import com.uber.tchannel.api.TFuture;
import com.uber.tchannel.api.errors.TChannelError;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class WorkflowServiceTChannel implements WorkflowService.Iface {

    public static final int DEFAULT_LOCAL_CADENCE_SERVER_PORT = 7933;

    private static final String LOCALHOST = "127.0.0.1";

    /**
     * Default RPC timeout used for all non long poll calls.
     */
    private static final long DEFAULT_RPC_TIMEOUT_MILLIS = 1000;
    /**
     * Default RPC timeout used for all long poll calls.
     */
    private static final long DEFAULT_POLL_RPC_TIMEOUT_MILLIS = 61 * 1000;

    /**
     * Default RPC timeout for QueryWorkflow
     */
    private static final long DEFAULT_QUERY_RPC_TIMEOUT_MILLIS = 10000;


    public static final String DEFAULT_CLIENT_APP_NAME = "cadence-client";

    /**
     * Name of the Cadence service front end as required by TChannel.
     */
    public static final String DEFAULT_SERVICE_NAME = "cadence-frontend";

    public static class ClientOptions {

        /**
         * The tChannel timeout in milliseconds
         */
        private final long rpcTimeoutMillis;

        /**
         * The tChannel timeout for long poll calls in milliseconds
         */
        private final long rpcLongPollTimeoutMillis;

        /**
         * The tChannel timeout for query workflow call in milliseconds
         */
        private final long rpcQueryTimeoutMillis;

        /**
         * TChannel service name that the Cadence service was started with.
         */
        private final String serviceName;

        /**
         * Name of the service using the cadence-client.
         */
        private final String clientAppName;

        /**
         * Client for metrics reporting.
         */
//        private MetricsClient metricsClient = new DefaultMetricsClient();
        private ClientOptions(Builder builder) {
            this.rpcTimeoutMillis = builder.rpcTimeoutMillis;
            if (builder.clientAppName == null) {
                this.clientAppName = DEFAULT_CLIENT_APP_NAME;
            } else {
                this.clientAppName = builder.clientAppName;
            }
            if (builder.serviceName == null) {
                this.serviceName = DEFAULT_SERVICE_NAME;
            } else {
                this.serviceName = builder.serviceName;
            }
            this.rpcLongPollTimeoutMillis = builder.rpcLongPollTimeoutMillis;
            this.rpcQueryTimeoutMillis = builder.rpcQueryTimeoutMillis;
//            this.metricsClient = builder.metricsClient;
        }

//        /**
//         * Copy to another client options with a different metrics client to report metrics.
//         * @param metricsClient metrics client
//         * @return client options
//         */
//        public ClientOptions copyWithMetricsClient(MetricsClient metricsClient) {
//            ClientOptions clone = new ClientOptions();
//            clone.rpcTimeoutMillis = this.rpcTimeoutMillis;
//            clone.deploymentStr = this.deploymentStr;
//            clone.clientAppName = this.clientAppName;
//            clone.metricsClient = metricsClient;
//            return clone;
//        }

        /**
         * @return Returns the rpc timeout value in millis.
         */
        public long getRpcTimeoutMillis() {
            return rpcTimeoutMillis;
        }

        /**
         * @return Returns the rpc timout for long poll requests in millis.
         */
        public long getRpcLongPollTimeoutMillis() {
            return rpcLongPollTimeoutMillis;
        }

        /**
         * @return Returns the rpc timout for query workflow requests in millis.
         */
        public long getRpcQueryTimeoutMillis() {
            return rpcQueryTimeoutMillis;
        }

        /**
         * Returns the client application name.
         */
        public String getClientAppName() {
            return this.clientAppName;
        }

        public String getServiceName() {
            return serviceName;
        }
//
//        /**
//         * @return Returns the client for metrics reporting.
//         */
//        public MetricsClient getMetricsClient() {
//            return this.metricsClient;
//        }

        /**
         * Builder is the builder for ClientOptions.
         *
         * @author venkat
         */
        public static class Builder {

            private String clientAppName = DEFAULT_CLIENT_APP_NAME;
            //            private MetricsClient metricsClient = new DefaultMetricsClient();
            private long rpcTimeoutMillis = DEFAULT_RPC_TIMEOUT_MILLIS;
            private long rpcLongPollTimeoutMillis = DEFAULT_POLL_RPC_TIMEOUT_MILLIS;
            public long rpcQueryTimeoutMillis = DEFAULT_QUERY_RPC_TIMEOUT_MILLIS;
            public String serviceName;

            /**
             * Sets the rpc timeout value for non query and non long poll calls.
             * Default is 1000.
             *
             * @param timeoutMillis timeout, in millis.
             */
            public Builder setRpcTimeout(long timeoutMillis) {
                this.rpcTimeoutMillis = timeoutMillis;
                return this;
            }

            /**
             * Sets the rpc timeout value for the following long poll based operations:
             * PollForDecisionTask, PollForActivityTask, GetWorkflowExecutionHistory.
             * Should never be below 60000 as this is server side timeout for the long poll.
             * Default is 61000.
             *
             * @param timeoutMillis timeout, in millis.
             */
            public Builder setRpcLongPollTimeout(long timeoutMillis) {
                this.rpcLongPollTimeoutMillis = timeoutMillis;
                return this;
            }

            /**
             * Sets the rpc timeout value for query calls.
             * Default is 10000.
             *
             * @param timeoutMillis timeout, in millis.
             */
            public Builder setQueryRpcTimeout(long timeoutMillis) {
                this.rpcQueryTimeoutMillis = timeoutMillis;
                return this;
            }

            /**
             * Sets the client application name.
             * <p>
             * This name will be used as the tchannel client service name. It will
             * also be reported as a tag along with metrics emitted to m3.
             *
             * @param clientAppName String representing the client application name.
             * @return Builder for ClentOptions
             */
            public Builder setClientAppName(String clientAppName) {
                this.clientAppName = clientAppName;
                return this;
            }

            /**
             * Sets the service name that Cadence service was started with.
             *
             * @param serviceName String representing the service name
             * @return Builder for ClentOptions
             */
            public Builder setServiceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }


//            /**
//             * Sets the metrics client to be used for metrics reporting.
//             *
//             * Applications must typically pass an M3 or statsd client here. By
//             * default, the builder uses M3.
//             *
//             * @param client
//             * @return
//             */
//            public Builder setMetricsClient(MetricsClient client) {
//                this.metricsClient = client;
//                return this;
//            }

            /**
             * Builds and returns a ClientOptions object.
             *
             * @return ClientOptions object with the specified params.
             */
            public ClientOptions build() {
                return new ClientOptions(this);
            }
        }
    }

    private static final String INTERFACE_NAME = "WorkflowService";

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceTChannel.class);

    private final ClientOptions options;
    private final Map<String, String> thriftHeaders;
    private final TChannel tChannel;
    private final SubChannel subChannel;

    /**
     * Creates Cadence client that connects to the local instance of the Cadence Service that listens
     * on a default port (7933).
     */
    public WorkflowServiceTChannel() {
        this(LOCALHOST, DEFAULT_LOCAL_CADENCE_SERVER_PORT, new ClientOptions.Builder().build());
    }

    /**
     * Creates Cadence client that connects to the specified host and port using default options.
     *
     * @param host host to connect
     * @param port port to connect
     */
    public WorkflowServiceTChannel(String host, int port) {
        this(host, port, new ClientOptions.Builder().build());
    }

    /**
     * Creates Cadence client that connects to the specified host and port using specified options.
     *
     * @param host    host to connect
     * @param port    port to connect
     * @param options configuration options like rpc timeouts.
     */
    public WorkflowServiceTChannel(String host, int port, ClientOptions options) {
        if (host == null) {
            throw new IllegalArgumentException("null host");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("0 or negative port");
        }
        this.options = options;
        String envUserName = System.getenv("USER");
        String envHostname;
        try {
            envHostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            envHostname = "localhost";
        }
        this.thriftHeaders = ImmutableMap.<String, String>builder()
                .put("user-name", envUserName)
                .put("host-name", envHostname)
                .build();
//        this.metricsReporter = new MetricsReporter(options.getMetricsClient());
        // Need to create tChannel last in order to prevent leaking when an exception is thrown
        this.tChannel = new TChannel.Builder(options.getClientAppName()).build();

        try {
            InetAddress address = InetAddress.getByName(host);
            ArrayList<InetSocketAddress> peers = new ArrayList<>();
            peers.add(new InetSocketAddress(address, port));
            this.subChannel = tChannel.makeSubChannel(options.getServiceName()).setPeers(peers);
            logger.info("Initialized TChannel: " + this.subChannel.toString());
        } catch (UnknownHostException e) {
            tChannel.shutdown();
            throw new RuntimeException("Unable to get name of host " + host, e);
        }
    }

    /**
     * Returns the endpoint in the format service::method"
     */
    private static String getEndpoint(String service, String method) {
        return String.format("%s::%s", service, method);
    }

    private <T> ThriftRequest<T> buildThriftRequest(String apiName, T body) {
        return buildThriftRequest(apiName, body, null);
    }

    private <T> ThriftRequest<T> buildThriftRequest(String apiName, T body, Long rpcTimeoutOverride) {
        String endpoint = getEndpoint(INTERFACE_NAME, apiName);
        ThriftRequest.Builder<T> builder = new ThriftRequest.Builder<T>(options.getServiceName(), endpoint);
        builder.setHeaders(thriftHeaders);
        if (rpcTimeoutOverride != null) {
            builder.setTimeout(rpcTimeoutOverride);
        } else {
            builder.setTimeout(this.options.getRpcTimeoutMillis());
        }
        builder.setBody(body);
        return builder.build();
    }

    private <T> ThriftResponse<T> doRemoteCall(ThriftRequest<?> request) throws TException {
        ThriftResponse<T> response = null;
        try {
            TFuture<ThriftResponse<T>> future = subChannel.send(request);
            response = future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TException(e);
        } catch (ExecutionException e) {
            throw new TException(e);
        } catch (TChannelError e) {
            throw new TException("Rpc error", e);
        }
        this.throwOnRpcError(response);
        return response;
    }

    private void throwOnRpcError(ThriftResponse<?> response) throws TException {
        if (response.isError()) {
            throw new TException("Rpc error:" + response.getError());
        }
    }

    public void close() {
        if (tChannel != null) {
            tChannel.shutdown();
        }
    }

    @Override
    public void RegisterDomain(RegisterDomainRequest registerRequest) throws BadRequestError, InternalServiceError, DomainAlreadyExistsError, TException {
        ThriftResponse<WorkflowService.RegisterDomain_result> response = null;
        try {
            ThriftRequest<WorkflowService.RegisterDomain_args> request = buildThriftRequest("RegisterDomain", new WorkflowService.RegisterDomain_args(registerRequest));
            response = doRemoteCall(request);
            WorkflowService.RegisterDomain_result result = response.getBody(WorkflowService.RegisterDomain_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetDomainExistsError()) {
                throw result.getDomainExistsError();
            }
            throw new TException("RegisterDomain failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public DescribeDomainResponse DescribeDomain(DescribeDomainRequest describeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.DescribeDomain_result> response = null;
        try {
            ThriftRequest<WorkflowService.DescribeDomain_args> request = buildThriftRequest("DescribeDomain", new WorkflowService.DescribeDomain_args(describeRequest));
            response = doRemoteCall(request);
            WorkflowService.DescribeDomain_result result = response.getBody(WorkflowService.DescribeDomain_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("DescribeDomain failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public UpdateDomainResponse UpdateDomain(UpdateDomainRequest updateRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.UpdateDomain_result> response = null;
        try {
            ThriftRequest<WorkflowService.UpdateDomain_args> request = buildThriftRequest("UpdateDomain", new WorkflowService.UpdateDomain_args(updateRequest));
            response = doRemoteCall(request);
            WorkflowService.UpdateDomain_result result = response.getBody(WorkflowService.UpdateDomain_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("UpdateDomain failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void DeprecateDomain(DeprecateDomainRequest deprecateRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.DeprecateDomain_result> response = null;
        try {
            ThriftRequest<WorkflowService.DeprecateDomain_args> request = buildThriftRequest("DeprecateDomain", new WorkflowService.DeprecateDomain_args(deprecateRequest));
            response = doRemoteCall(request);
            WorkflowService.DeprecateDomain_result result = response.getBody(WorkflowService.DeprecateDomain_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("DeprecateDomain failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public StartWorkflowExecutionResponse StartWorkflowExecution(StartWorkflowExecutionRequest startRequest) throws BadRequestError, InternalServiceError, WorkflowExecutionAlreadyStartedError, ServiceBusyError, TException {
        startRequest.setRequestId(UUID.randomUUID().toString());
        ThriftResponse<WorkflowService.StartWorkflowExecution_result> response = null;
        try {
            ThriftRequest<WorkflowService.StartWorkflowExecution_args> request = buildThriftRequest("StartWorkflowExecution", new WorkflowService.StartWorkflowExecution_args(startRequest));
            response = doRemoteCall(request);
            WorkflowService.StartWorkflowExecution_result result = response.getBody(WorkflowService.StartWorkflowExecution_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetSessionAlreadyExistError()) {
                throw result.getSessionAlreadyExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("StartWorkflowExecution failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public GetWorkflowExecutionHistoryResponse GetWorkflowExecutionHistory(GetWorkflowExecutionHistoryRequest getRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.GetWorkflowExecutionHistory_result> response = null;
        try {
            ThriftRequest<WorkflowService.GetWorkflowExecutionHistory_args> request = buildThriftRequest(
                    "GetWorkflowExecutionHistory", new WorkflowService.GetWorkflowExecutionHistory_args(getRequest), options.getRpcLongPollTimeoutMillis());
            response = doRemoteCall(request);
            WorkflowService.GetWorkflowExecutionHistory_result result = response.getBody(WorkflowService.GetWorkflowExecutionHistory_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("GetWorkflowExecutionHistory failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public PollForDecisionTaskResponse PollForDecisionTask(PollForDecisionTaskRequest pollRequest) throws BadRequestError, InternalServiceError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.PollForDecisionTask_result> response = null;
        try {
            ThriftRequest<WorkflowService.PollForDecisionTask_args> request = buildThriftRequest(
                    "PollForDecisionTask", new WorkflowService.PollForDecisionTask_args(pollRequest), options.getRpcLongPollTimeoutMillis());
            response = doRemoteCall(request);
            WorkflowService.PollForDecisionTask_result result = response.getBody(WorkflowService.PollForDecisionTask_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("PollForDecisionTask failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondDecisionTaskCompleted(RespondDecisionTaskCompletedRequest completeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondDecisionTaskCompleted_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondDecisionTaskCompleted_args> request = buildThriftRequest("RespondDecisionTaskCompleted", new WorkflowService.RespondDecisionTaskCompleted_args(completeRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondDecisionTaskCompleted_result result = response.getBody(WorkflowService.RespondDecisionTaskCompleted_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondDecisionTaskCompleted failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondDecisionTaskFailed(RespondDecisionTaskFailedRequest failedRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondDecisionTaskFailed_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondDecisionTaskFailed_args> request = buildThriftRequest("RespondDecisionTaskFailed", new WorkflowService.RespondDecisionTaskFailed_args(failedRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondDecisionTaskFailed_result result = response.getBody(WorkflowService.RespondDecisionTaskFailed_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondDecisionTaskFailed failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public PollForActivityTaskResponse PollForActivityTask(PollForActivityTaskRequest pollRequest) throws BadRequestError, InternalServiceError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.PollForActivityTask_result> response = null;
        try {
            ThriftRequest<WorkflowService.PollForActivityTask_args> request = buildThriftRequest(
                    "PollForActivityTask", new WorkflowService.PollForActivityTask_args(pollRequest), options.getRpcLongPollTimeoutMillis());
            response = doRemoteCall(request);
            WorkflowService.PollForActivityTask_result result = response.getBody(WorkflowService.PollForActivityTask_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("PollForActivityTask failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public RecordActivityTaskHeartbeatResponse RecordActivityTaskHeartbeat(RecordActivityTaskHeartbeatRequest heartbeatRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RecordActivityTaskHeartbeat_result> response = null;
        try {
            ThriftRequest<WorkflowService.RecordActivityTaskHeartbeat_args> request = buildThriftRequest("RecordActivityTaskHeartbeat", new WorkflowService.RecordActivityTaskHeartbeat_args(heartbeatRequest));
            response = doRemoteCall(request);
            WorkflowService.RecordActivityTaskHeartbeat_result result = response.getBody(WorkflowService.RecordActivityTaskHeartbeat_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RecordActivityTaskHeartbeat failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskCompleted(RespondActivityTaskCompletedRequest completeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskCompleted_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskCompleted_args> request = buildThriftRequest("RespondActivityTaskCompleted", new WorkflowService.RespondActivityTaskCompleted_args(completeRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskCompleted_result result = response.getBody(WorkflowService.RespondActivityTaskCompleted_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskCompleted failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskCompletedByID(RespondActivityTaskCompletedByIDRequest completeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskCompletedByID_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskCompletedByID_args> request = buildThriftRequest("RespondActivityTaskCompletedByID", new WorkflowService.RespondActivityTaskCompletedByID_args(completeRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskCompletedByID_result result = response.getBody(WorkflowService.RespondActivityTaskCompletedByID_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskCompletedByID failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskFailed(RespondActivityTaskFailedRequest failRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskFailed_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskFailed_args> request = buildThriftRequest("RespondActivityTaskFailed", new WorkflowService.RespondActivityTaskFailed_args(failRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskFailed_result result = response.getBody(WorkflowService.RespondActivityTaskFailed_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskFailed failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskFailedByID(RespondActivityTaskFailedByIDRequest failRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskFailedByID_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskFailedByID_args> request = buildThriftRequest("RespondActivityTaskFailedByID", new WorkflowService.RespondActivityTaskFailedByID_args(failRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskFailedByID_result result = response.getBody(WorkflowService.RespondActivityTaskFailedByID_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskFailedByID failedByID with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskCanceled(RespondActivityTaskCanceledRequest canceledRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskCanceled_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskCanceled_args> request = buildThriftRequest("RespondActivityTaskCanceled", new WorkflowService.RespondActivityTaskCanceled_args(canceledRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskCanceled_result result = response.getBody(WorkflowService.RespondActivityTaskCanceled_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskCanceled failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondActivityTaskCanceledByID(RespondActivityTaskCanceledByIDRequest canceledByIDRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondActivityTaskCanceledByID_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondActivityTaskCanceledByID_args> request = buildThriftRequest("RespondActivityTaskCanceledByID", new WorkflowService.RespondActivityTaskCanceledByID_args(canceledByIDRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondActivityTaskCanceledByID_result result = response.getBody(WorkflowService.RespondActivityTaskCanceledByID_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondActivityTaskCanceledByID failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RequestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest cancelRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, CancellationAlreadyRequestedError, ServiceBusyError, TException {
        cancelRequest.setRequestId(UUID.randomUUID().toString());
        ThriftResponse<WorkflowService.RequestCancelWorkflowExecution_result> response = null;
        try {
            ThriftRequest<WorkflowService.RequestCancelWorkflowExecution_args> request = buildThriftRequest("RequestCancelWorkflowExecution", new WorkflowService.RequestCancelWorkflowExecution_args(cancelRequest));
            response = doRemoteCall(request);
            WorkflowService.RequestCancelWorkflowExecution_result result = response.getBody(WorkflowService.RequestCancelWorkflowExecution_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetCancellationAlreadyRequestedError()) {
                throw result.getCancellationAlreadyRequestedError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("RequestCancelWorkflowExecution failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void SignalWorkflowExecution(SignalWorkflowExecutionRequest signalRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.SignalWorkflowExecution_result> response = null;
        try {
            ThriftRequest<WorkflowService.SignalWorkflowExecution_args> request = buildThriftRequest("SignalWorkflowExecution", new WorkflowService.SignalWorkflowExecution_args(signalRequest));
            response = doRemoteCall(request);
            WorkflowService.SignalWorkflowExecution_result result = response.getBody(WorkflowService.SignalWorkflowExecution_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("SignalWorkflowExecution failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void TerminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.TerminateWorkflowExecution_result> response = null;
        try {
            ThriftRequest<WorkflowService.TerminateWorkflowExecution_args> request = buildThriftRequest("TerminateWorkflowExecution", new WorkflowService.TerminateWorkflowExecution_args(terminateRequest));
            response = doRemoteCall(request);
            WorkflowService.TerminateWorkflowExecution_result result = response.getBody(WorkflowService.TerminateWorkflowExecution_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("TerminateWorkflowExecution failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public ListOpenWorkflowExecutionsResponse ListOpenWorkflowExecutions(ListOpenWorkflowExecutionsRequest listRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.ListOpenWorkflowExecutions_result> response = null;
        try {
            ThriftRequest<WorkflowService.ListOpenWorkflowExecutions_args> request = buildThriftRequest("ListOpenWorkflowExecutions", new WorkflowService.ListOpenWorkflowExecutions_args(listRequest));
            response = doRemoteCall(request);
            WorkflowService.ListOpenWorkflowExecutions_result result = response.getBody(WorkflowService.ListOpenWorkflowExecutions_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("ListOpenWorkflowExecutions failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public ListClosedWorkflowExecutionsResponse ListClosedWorkflowExecutions(ListClosedWorkflowExecutionsRequest listRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, ServiceBusyError, TException {
        ThriftResponse<WorkflowService.ListClosedWorkflowExecutions_result> response = null;
        try {
            ThriftRequest<WorkflowService.ListClosedWorkflowExecutions_args> request = buildThriftRequest("ListClosedWorkflowExecutions", new WorkflowService.ListClosedWorkflowExecutions_args(listRequest));
            response = doRemoteCall(request);
            WorkflowService.ListClosedWorkflowExecutions_result result = response.getBody(WorkflowService.ListClosedWorkflowExecutions_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetServiceBusyError()) {
                throw result.getServiceBusyError();
            }
            throw new TException("ListClosedWorkflowExecutions failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public void RespondQueryTaskCompleted(RespondQueryTaskCompletedRequest completeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.RespondQueryTaskCompleted_result> response = null;
        try {
            ThriftRequest<WorkflowService.RespondQueryTaskCompleted_args> request = buildThriftRequest("RespondQueryTaskCompleted", new WorkflowService.RespondQueryTaskCompleted_args(completeRequest));
            response = doRemoteCall(request);
            WorkflowService.RespondQueryTaskCompleted_result result = response.getBody(WorkflowService.RespondQueryTaskCompleted_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return;
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("RespondQueryTaskCompleted failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public QueryWorkflowResponse QueryWorkflow(QueryWorkflowRequest queryRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, QueryFailedError, TException {
        ThriftResponse<WorkflowService.QueryWorkflow_result> response = null;
        try {
            ThriftRequest<WorkflowService.QueryWorkflow_args> request = buildThriftRequest(
                    "QueryWorkflow", new WorkflowService.QueryWorkflow_args(queryRequest), options.getRpcQueryTimeoutMillis());
            response = doRemoteCall(request);
            WorkflowService.QueryWorkflow_result result = response.getBody(WorkflowService.QueryWorkflow_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            if (result.isSetQueryFailedError()) {
                throw result.getQueryFailedError();
            }

            throw new TException("QueryWorkflow failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public DescribeWorkflowExecutionResponse DescribeWorkflowExecution(DescribeWorkflowExecutionRequest describeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.DescribeWorkflowExecution_result> response = null;
        try {
            ThriftRequest<WorkflowService.DescribeWorkflowExecution_args> request = buildThriftRequest("DescribeWorkflowExecution", new WorkflowService.DescribeWorkflowExecution_args(describeRequest));
            response = doRemoteCall(request);
            WorkflowService.DescribeWorkflowExecution_result result = response.getBody(WorkflowService.DescribeWorkflowExecution_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("DescribeWorkflowExecution failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }

    @Override
    public DescribeTaskListResponse DescribeTaskList(DescribeTaskListRequest describeRequest) throws BadRequestError, InternalServiceError, EntityNotExistsError, TException {
        ThriftResponse<WorkflowService.DescribeTaskList_result> response = null;
        try {
            ThriftRequest<WorkflowService.DescribeTaskList_args> request = buildThriftRequest("DescribeTaskList", new WorkflowService.DescribeTaskList_args(describeRequest));
            response = doRemoteCall(request);
            WorkflowService.DescribeTaskList_result result = response.getBody(WorkflowService.DescribeTaskList_result.class);
            if (response.getResponseCode() == ResponseCode.OK) {
                return result.getSuccess();
            }
            if (result.isSetBadRequestError()) {
                throw result.getBadRequestError();
            }
            if (result.isSetInternalServiceError()) {
                throw result.getInternalServiceError();
            }
            if (result.isSetEntityNotExistError()) {
                throw result.getEntityNotExistError();
            }
            throw new TException("DescribeTaskList failed with unknown error:" + result);
        } finally {
            if (response != null) {
                response.release();
            }
        }
    }
}
