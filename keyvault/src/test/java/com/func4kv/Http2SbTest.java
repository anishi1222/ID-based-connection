package com.func4kv;

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
        Http2Sb function = new Http2Sb(setting -> "key-id", keyId -> new EncryptingCryptography(new byte[] {1}));

        HttpResponseMessage response = function.run(
            new TestHttpRequestMessage<>(Optional.empty()),
            output,
            new TestExecutionContext());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
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
        assertNull(output.getValue());
    }

    @Test
    void encryptsBodyAndPublishesPayload() {
        byte[] cipherText = new byte[] {10, 20, 30};
        AtomicReference<String> keyIdUsed = new AtomicReference<>();
        EncryptingCryptography cryptography = new EncryptingCryptography(cipherText);
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
        assertEquals(cipherText.length, output.getValue().getLength());
    }

    private static final class EncryptingCryptography implements KeyVaultCryptography {

        private final byte[] cipherText;
        private byte[] plainTextReceived;

        private EncryptingCryptography(byte[] cipherText) {
            this.cipherText = cipherText;
        }

        @Override
        public byte[] encrypt(byte[] plainText) {
            this.plainTextReceived = plainText;
            return cipherText;
        }

        @Override
        public byte[] decrypt(byte[] cipherText) {
            throw new UnsupportedOperationException();
        }
    }
}