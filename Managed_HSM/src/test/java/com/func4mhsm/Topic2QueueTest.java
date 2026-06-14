package com.func4mhsm;

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
        byte[] iv = new byte[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, -112, -23, 121, 98, -37};
        Payload payload = new Payload();
        payload.setCipherText(cipherText);
        payload.setIv(iv);
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
        assertArrayEquals(iv, cryptography.ivReceived);
        assertEquals("decrypted", output.getValue());
    }

    @Test
    void skipsOutputWhenKeyIdIsMissing() {
        Payload payload = new Payload();
        payload.setCipherText(new byte[] {1, 2, 3});
        payload.setIv(new byte[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, -112, -23, 121, 98, -37});
        Topic2Queue function = new Topic2Queue(setting -> null, keyId -> {
            throw new AssertionError("Cryptography should not be created without KEY_ID.");
        });
        TestOutputBinding<String> output = new TestOutputBinding<>();

        function.run(payload, output, new TestExecutionContext());

        assertNull(output.getValue());
    }

    private static final class DecryptingCryptography implements ManagedHsmCryptography {

        private final byte[] plainText;
        private byte[] cipherTextReceived;
        private byte[] ivReceived;

        private DecryptingCryptography(byte[] plainText) {
            this.plainText = plainText;
        }

        @Override
        public ManagedHsmEncryptionResult encrypt(byte[] plainText) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] decrypt(byte[] cipherText, byte[] iv) {
            this.cipherTextReceived = cipherText;
            this.ivReceived = iv;
            return plainText;
        }
    }
}