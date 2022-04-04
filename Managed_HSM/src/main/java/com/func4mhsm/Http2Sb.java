package com.func4mhsm;

import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptParameters;
import com.azure.security.keyvault.keys.cryptography.models.EncryptResult;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusTopicOutput;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
            topicName = "mhsmt1",
            connection = "sbConnection",
            subscriptionName = "s1"
        ) OutputBinding<Payload> message,
        final ExecutionContext context) {

        // Get HTTP body
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("No HTTP body").build();
        }
        final String body = request.getBody().get();

        Optional<String> keyId = Optional.ofNullable(System.getenv("KEY_ID"));
        if(keyId.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Key is not found").build();
        }

        CryptographyClient cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId.get())
            .buildClient();

        // Generate an initialized vector
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);

        // Encryption
        byte[] rawBytes = body.getBytes(StandardCharsets.UTF_8);
        EncryptParameters encryptParameters = EncryptParameters.createA256CbcParameters(rawBytes, iv);
        EncryptResult encryptResult = cryptoClient.encrypt(encryptParameters, new Context("key1", "value1"));

        Payload payload = new Payload();
        payload.setCipherText(encryptResult.getCipherText());
        payload.setIv(iv);

        // Send data to Service Bus
        message.setValue(payload);
        return request.createResponseBuilder(HttpStatus.ACCEPTED)
            .body("body message is encrypted and published to mhsmt1")
            .build();
    }
}
