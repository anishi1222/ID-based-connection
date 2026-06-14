package com.func4mhsm;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

class Http2SbTest {

    @Test
    void returnsBadRequestWhenBodyIsMissing() {
        TestOutputBinding<Payload> output = new TestOutputBinding<>();
        Http2Sb function = new Http2Sb(
            setting -> "key-id",
            keyId -> new EncryptingCryptography(new ManagedHsmEncryptionResult(new byte[] {1}, new byte[] {2})));

        HttpResponseMessage response = function.run(
            new TestHttpRequestMessage<>(Optional.empty()),
            output,
            new TestExecutionContext());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertEquals("No HTTP body", response.getBody());
        assertNull(output.getValue());
    }

    @Test
    void returnsServerErrorWhenKeyIdIsMissing() {
        TestOutputBinding<Payload> output = new TestOutputBinding<>();
        Http2Sb function = new Http2Sb(setting -> null, keyId -> {
            throw new AssertionError("Cryptography should not be created without KEY_ID.");
        });

        HttpResponseMessage response = function.run(
            new TestHttpRequestMessage<>(Optional.of("hello")),
            output,
            new TestExecutionContext());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals("Key is not found", response.getBody());
        assertNull(output.getValue());
    }

    @Test
    void encryptsBodyAndPublishesPayload() {
        byte[] cipherText = new byte[] {10, 20, 30};
        byte[] iv = new byte[] {1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, -112, -23, 121, 98, -37};
        AtomicReference<String> keyIdUsed = new AtomicReference<>();
        EncryptingCryptography cryptography = new EncryptingCryptography(new ManagedHsmEncryptionResult(cipherText, iv));
        Http2Sb function = new Http2Sb(setting -> "key-id", keyId -> {
            keyIdUsed.set(keyId);
            return cryptography;
        });
        TestOutputBinding<Payload> output = new TestOutputBinding<>();

        HttpResponseMessage response = function.run(
            new TestHttpRequestMessage<>(Optional.of("hello")),
            output,
            new TestExecutionContext());

        assertEquals(HttpStatus.ACCEPTED, response.getStatus());
        assertEquals("key-id", keyIdUsed.get());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), cryptography.plainTextReceived);
        assertArrayEquals(cipherText, output.getValue().getCipherText());
        assertArrayEquals(iv, output.getValue().getIv());
    }

    private static final class EncryptingCryptography implements ManagedHsmCryptography {

        private final ManagedHsmEncryptionResult encryptionResult;
        private byte[] plainTextReceived;

        private EncryptingCryptography(ManagedHsmEncryptionResult encryptionResult) {
            this.encryptionResult = encryptionResult;
        }

        @Override
        public ManagedHsmEncryptionResult encrypt(byte[] plainText) {
            this.plainTextReceived = plainText;
            return encryptionResult;
        }

        @Override
        public byte[] decrypt(byte[] cipherText, byte[] iv) {
            throw new UnsupportedOperationException();
        }
    }
}