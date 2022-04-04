package com.func4mhsm;

import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptParameters;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
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
            topicName = "mhsmt1",
            subscriptionName = "s1",
            connection = "sbConnection") Payload payload,
        @ServiceBusQueueOutput(
            name = "res",
            queueName = "mhsmq1",
            connection = "sbConnection") OutputBinding<String> output,
        final ExecutionContext context) {

        Optional<String> keyId = Optional.ofNullable(System.getenv("KEY_ID"));
        if(keyId.isEmpty()) return;

        CryptographyClient cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId.get())
            .buildClient();
        DecryptParameters decryptParameters = DecryptParameters.createA256CbcParameters(payload.getCipherText(), payload.getIv());
        DecryptResult decryptResult = cryptoClient.decrypt(decryptParameters, new Context("key1", "value1"));

        String decryptedString = new String(decryptResult.getPlainText(), StandardCharsets.UTF_8);
        output.setValue(decryptedString);
    }
}
