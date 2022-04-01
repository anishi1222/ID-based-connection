package com.func4kv;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicOutput;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Http2Sb {

    @FunctionName("http2sb")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            @ServiceBusTopicOutput(
                name="res",
                topicName = "kvt1",
                connection = "sbConnection",
                subscriptionName = "s1"
            ) OutputBinding<Payload> message,
            final ExecutionContext context) {

        // Get HTTP body
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
        }
        final String body = request.getBody().get();

        byte[] rawBytes = body.getBytes(StandardCharsets.UTF_8);
        Optional<String> keyId = Optional.ofNullable(System.getenv("KEY_ID"));
        if(keyId.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        CryptographyClient cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId.get())
            .buildClient();
        EncryptResult encryptResult = cryptoClient.encrypt(EncryptionAlgorithm.RSA_OAEP, rawBytes);
        byte[] _cipherText = encryptResult.getCipherText();
        Payload payload = new Payload();
        payload.setCipherText(_cipherText);
        payload.setLength(_cipherText.length);

        // Send data to Service Bus
        message.setValue(payload);
        return request.createResponseBuilder(HttpStatus.ACCEPTED)
            .body("body message is encrypted and published to kvt1")
            .build();
    }
}
