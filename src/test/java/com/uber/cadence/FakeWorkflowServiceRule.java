package com.uber.cadence;

import com.uber.cadence.serviceclient.ClientOptions;
import com.uber.cadence.serviceclient.IWorkflowService;
import com.uber.cadence.serviceclient.WorkflowServiceTChannel;
import com.uber.tchannel.api.ResponseCode;
import com.uber.tchannel.api.TChannel;
import com.uber.tchannel.api.handlers.ThriftRequestHandler;
import com.uber.tchannel.messages.ThriftRequest;
import com.uber.tchannel.messages.ThriftResponse;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.rules.ExternalResource;

/**
 * FakeWorkflowServiceRule a local TChannel service which can be stubbed with fixed responses and
 * captures the requests made to it. This allows testing throw the entire TChannel stack,
 * particularly with TChannel being difficult to mock.
 */
public class FakeWorkflowServiceRule extends ExternalResource {

  private static final Duration SPAN_TIMEOUT = Duration.ofSeconds(5);

  private final Map<String, StubbedEndpoint> stubbedEndpoints = new ConcurrentHashMap<>();
  private final MockTracer tracer = new MockTracer();
  private TChannel tChannel;
  private IWorkflowService clientConn;

  @Override
  protected void before() throws Throwable {
    tChannel = new TChannel.Builder("cadence-frontend").build();
    tChannel.setDefaultUserHandler(
        new ThriftRequestHandler<Object, Object>() {
          @Override
          public ThriftResponse<Object> handleImpl(ThriftRequest<Object> request) {
            StubbedEndpoint endpoint = stubbedEndpoints.get(request.getEndpoint());
            if (endpoint == null) {
              throw new IllegalStateException(
                  "Endpoint " + request.getEndpoint() + " was not stubbed");
            }
            @SuppressWarnings("rawtypes")
            StubbedResponse stub = endpoint.getNext();
            if (stub == null) {
              throw new IllegalStateException(
                  "Exhausted all invocations of  " + request.getEndpoint());
            }
            //noinspection unchecked
            stub.future.complete(request.getBody(stub.requestType));
            return new ThriftResponse.Builder<>(request)
                .setBody(stub.body)
                .setResponseCode(stub.code)
                .build();
          }
        });
    tChannel.listen();
    clientConn =
        new WorkflowServiceTChannel(
            ClientOptions.newBuilder()
                .setTracer(tracer)
                .setHost(tChannel.getListeningHost())
                .setPort(tChannel.getListeningPort())
                .build());
  }

  @Override
  protected void after() {
    stubbedEndpoints.clear();
    if (clientConn != null) {
      clientConn.close();
    }
    if (tChannel != null) {
      tChannel.shutdown();
      tChannel = null;
    }
  }

  public void resetStubs() {
    tracer.reset();
    stubbedEndpoints.clear();
  }

  public IWorkflowService getClient() {
    return clientConn;
  }

  public MockTracer getTracer() {
    return tracer;
  }

  /**
   * Returns the first finished span with the given operation name, waiting for it to appear.
   * TChannel finishes its client span from a response callback which may run after the calling
   * thread has been unblocked, so spans are not guaranteed to be visible once the call returns.
   */
  public MockSpan awaitSpan(String operationName) {
    long deadline = System.nanoTime() + SPAN_TIMEOUT.toNanos();
    while (true) {
      List<MockSpan> spans = tracer.finishedSpans();
      Optional<MockSpan> span =
          spans.stream().filter(s -> operationName.equals(s.operationName())).findFirst();
      if (span.isPresent()) {
        return span.get();
      }
      if (System.nanoTime() - deadline >= 0) {
        throw new AssertionError("No span found for " + operationName + ", got: " + spans);
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted waiting for span " + operationName, e);
      }
    }
  }

  public <V> CompletableFuture<V> stubSuccess(
      String endpoint, Class<V> requestType, Object response) {
    return stubEndpoint(endpoint, requestType, ResponseCode.OK, response);
  }

  public <V> CompletableFuture<V> stubError(
      String endpoint, Class<V> requestType, Object response) {
    return stubEndpoint(endpoint, requestType, ResponseCode.Error, response);
  }

  public <V> CompletableFuture<V> stubEndpoint(
      String endpoint, Class<V> requestType, ResponseCode code, Object response) {
    CompletableFuture<V> future = new CompletableFuture<>();
    StubbedEndpoint endpointStub =
        stubbedEndpoints.computeIfAbsent(endpoint, id -> new StubbedEndpoint());
    endpointStub.addStub(new StubbedResponse<>(response, code, future, requestType));
    return future;
  }

  private static class StubbedEndpoint {
    private final Queue<StubbedResponse<?>> responses = new ConcurrentLinkedQueue<>();

    public void addStub(StubbedResponse<?> response) {
      responses.add(response);
    }

    public StubbedResponse<?> getNext() {
      return responses.poll();
    }
  }

  private static class StubbedResponse<V> {
    private final Object body;
    private final ResponseCode code;
    private final CompletableFuture<V> future;
    private final Class<V> requestType;

    private StubbedResponse(
        Object body, ResponseCode code, CompletableFuture<V> future, Class<V> requestType) {
      this.body = body;
      this.code = code;
      this.future = future;
      this.requestType = requestType;
    }
  }
}
