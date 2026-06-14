package com.func4kv;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicOutput;

public class Http2Sb {

    private static final String KEY_ID_SETTING = "KEY_ID";
    private static final String SERVICE_BUS_TOPIC_NAME_EXPRESSION = "%SERVICE_BUS_TOPIC_NAME%";
    private static final String SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME_EXPRESSION = "%SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME%";

    private final Function<String, String> environment;
    private final Function<String, KeyVaultCryptography> cryptographyFactory;

    public Http2Sb() {
        this(System::getenv, AzureKeyVaultCryptography::new);
    }

    Http2Sb(
            Function<String, String> environment,
            Function<String, KeyVaultCryptography> cryptographyFactory) {
        this.environment = Objects.requireNonNull(environment);
        this.cryptographyFactory = Objects.requireNonNull(cryptographyFactory);
    }

    @FunctionName("http2sb")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            @ServiceBusTopicOutput(
                name="res",
                topicName = SERVICE_BUS_TOPIC_NAME_EXPRESSION,
                connection = "sbConnection",
                subscriptionName = SERVICE_BUS_TOPIC_SUBSCRIPTION_NAME_EXPRESSION
            ) OutputBinding<Payload> message,
            final ExecutionContext context) {

        // Get HTTP body
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
        }
        final String body = request.getBody().get();

        byte[] rawBytes = body.getBytes(StandardCharsets.UTF_8);
        Optional<String> keyId = Optional.ofNullable(environment.apply(KEY_ID_SETTING));
        if(keyId.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        byte[] _cipherText = cryptographyFactory.apply(keyId.get()).encrypt(rawBytes);
        Payload payload = new Payload();
        payload.setCipherText(_cipherText);
        payload.setLength(_cipherText.length);

        // Send data to Service Bus
        message.setValue(payload);
        return request.createResponseBuilder(HttpStatus.ACCEPTED)
            .body("body message is encrypted and published to the configured topic")
            .build();
    }
}
