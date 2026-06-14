package com.func4kv;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class Topic2QueueTest {

    @Test
    void decryptsPayloadAndWritesQueueOutput() {
        byte[] cipherText = new byte[] {1, 2, 3};
        Payload payload = new Payload();
        payload.setCipherText(cipherText);
        AtomicReference<String> keyIdUsed = new AtomicReference<>();
        DecryptingCryptography cryptography = new DecryptingCryptography("decrypted".getBytes(StandardCharsets.UTF_8));
        Topic2Queue function = new Topic2Queue(setting -> "key-id", keyId -> {
            keyIdUsed.set(keyId);
            return cryptography;
        });
        TestOutputBinding<String> output = new TestOutputBinding<>();

        function.run(payload, output, new TestExecutionContext());

        assertEquals("key-id", keyIdUsed.get());
        assertArrayEquals(cipherText, cryptography.cipherTextReceived);
        assertEquals("decrypted", output.getValue());
    }

    @Test
    void skipsOutputWhenKeyIdIsMissing() {
        Payload payload = new Payload();
        payload.setCipherText(new byte[] {1, 2, 3});
        Topic2Queue function = new Topic2Queue(setting -> null, keyId -> {
            throw new AssertionError("Cryptography should not be created without KEY_ID.");
        });
        TestOutputBinding<String> output = new TestOutputBinding<>();

        function.run(payload, output, new TestExecutionContext());

        assertNull(output.getValue());
    }

    private static final class DecryptingCryptography implements KeyVaultCryptography {

        private final byte[] plainText;
        private byte[] cipherTextReceived;

        private DecryptingCryptography(byte[] plainText) {
            this.plainText = plainText;
        }

        @Override
        public byte[] encrypt(byte[] plainText) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] decrypt(byte[] cipherText) {
            this.cipherTextReceived = cipherText;
            return plainText;
        }
    }
}