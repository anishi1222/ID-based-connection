package com.func4kv;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;

interface KeyVaultCryptography {
    byte[] encrypt(byte[] plainText);

    byte[] decrypt(byte[] cipherText);
}

final class AzureKeyVaultCryptography implements KeyVaultCryptography {

    private final CryptographyClient cryptoClient;

    AzureKeyVaultCryptography(String keyId) {
        this.cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId)
            .buildClient();
    }

    @Override
    public byte[] encrypt(byte[] plainText) {
        return cryptoClient.encrypt(EncryptionAlgorithm.RSA_OAEP, plainText).getCipherText();
    }

    @Override
    public byte[] decrypt(byte[] cipherText) {
        return cryptoClient.decrypt(EncryptionAlgorithm.RSA_OAEP, cipherText).getPlainText();
    }
}