package com.func4kv;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.ServiceBusTopicTrigger;

public class Topic2Queue {

    private static final String KEY_ID_SETTING = "KEY_ID";

    private final Function<String, String> environment;
    private final Function<String, KeyVaultCryptography> cryptographyFactory;

    public Topic2Queue() {
        this(System::getenv, AzureKeyVaultCryptography::new);
    }

    Topic2Queue(
            Function<String, String> environment,
            Function<String, KeyVaultCryptography> cryptographyFactory) {
        this.environment = Objects.requireNonNull(environment);
        this.cryptographyFactory = Objects.requireNonNull(cryptographyFactory);
    }

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
        Optional<String> keyId = Optional.ofNullable(environment.apply(KEY_ID_SETTING));
        if(keyId.isEmpty()) return;

        byte[] rawBytes = cryptographyFactory.apply(keyId.get()).decrypt(cipherText);
        output.setValue(new String(rawBytes, StandardCharsets.UTF_8));
    }
}
