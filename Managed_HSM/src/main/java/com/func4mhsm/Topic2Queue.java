package com.func4mhsm;

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
    private final Function<String, ManagedHsmCryptography> cryptographyFactory;

    public Topic2Queue() {
        this(System::getenv, AzureManagedHsmCryptography::new);
    }

    Topic2Queue(
        Function<String, String> environment,
        Function<String, ManagedHsmCryptography> cryptographyFactory) {
        this.environment = Objects.requireNonNull(environment);
        this.cryptographyFactory = Objects.requireNonNull(cryptographyFactory);
    }

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

        Optional<String> keyId = Optional.ofNullable(environment.apply(KEY_ID_SETTING));
        if(keyId.isEmpty()) return;

        byte[] plainText = cryptographyFactory.apply(keyId.get()).decrypt(payload.getCipherText(), payload.getIv());
        String decryptedString = new String(plainText, StandardCharsets.UTF_8);
        output.setValue(decryptedString);
    }
}
