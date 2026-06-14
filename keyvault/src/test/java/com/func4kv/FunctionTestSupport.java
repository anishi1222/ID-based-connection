package com.func4kv;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.RetryContext;
import com.microsoft.azure.functions.TraceContext;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

final class TestHttpRequestMessage<T> implements HttpRequestMessage<T> {

    private final T body;

    TestHttpRequestMessage(T body) {
        this.body = body;
    }

    @Override
    public URI getUri() {
        return URI.create("http://localhost/api/http2sb");
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of();
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return Map.of();
    }

    @Override
    public T getBody() {
        return body;
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
        return new TestHttpResponseMessage.Builder(status);
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType status) {
        return new TestHttpResponseMessage.Builder(status);
    }
}

final class TestHttpResponseMessage implements HttpResponseMessage {

    private final HttpStatusType status;
    private final Map<String, String> headers;
    private final Object body;

    private TestHttpResponseMessage(HttpStatusType status, Map<String, String> headers, Object body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public HttpStatusType getStatus() {
        return status;
    }

    @Override
    public int getStatusCode() {
        return status.value();
    }

    @Override
    public String getHeader(String key) {
        return headers.get(key);
    }

    @Override
    public Object getBody() {
        return body;
    }

    static final class Builder implements HttpResponseMessage.Builder {

        private HttpStatusType status;
        private final Map<String, String> headers = new HashMap<>();
        private Object body;

        Builder(HttpStatusType status) {
            this.status = status;
        }

        @Override
        public HttpResponseMessage.Builder status(HttpStatusType status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new TestHttpResponseMessage(status, Map.copyOf(headers), body);
        }
    }
}

final class TestOutputBinding<T> implements OutputBinding<T> {

    private T value;

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }
}

final class TestExecutionContext implements ExecutionContext {

    private static final Logger LOGGER = Logger.getLogger(TestExecutionContext.class.getName());

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getInvocationId() {
        return "test-invocation";
    }

    @Override
    public String getFunctionName() {
        return "test-function";
    }

    @Override
    public TraceContext getTraceContext() {
        return null;
    }

    @Override
    public RetryContext getRetryContext() {
        return null;
    }
}