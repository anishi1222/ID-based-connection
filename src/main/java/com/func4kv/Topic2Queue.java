package com.func4kv;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Topic2Queue {
    @FunctionName("topic2Queue")
    public void run(
            @ServiceBusTopicTrigger(
                name = "req",
                topicName = "kvt1",
                subscriptionName = "s1",
                connection = "sbConnection") Payload payload,
            @ServiceBusQueueOutput(
                name = "res",
                queueName = "kvq1",
                connection = "sbConnection") OutputBinding<String> output,
            final ExecutionContext context) {
        context.getLogger().info("Java Service Bus trigger processed a request.");


        byte[] cipherText = payload.getCipherText();
        Optional<String> keyId = Optional.ofNullable(System.getenv("KEY_ID"));
        if(keyId.isEmpty()) return;

        CryptographyClient cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId.get())
            .buildClient();
        DecryptResult decryptResult = cryptoClient.decrypt(EncryptionAlgorithm.RSA_OAEP,cipherText);
        byte[] rawBytes = decryptResult.getPlainText();
        output.setValue(new String(rawBytes, StandardCharsets.UTF_8));
    }
}
