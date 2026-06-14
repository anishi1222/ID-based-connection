package com.func4mhsm;

import java.security.SecureRandom;

import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptParameters;
import com.azure.security.keyvault.keys.cryptography.models.EncryptParameters;

interface ManagedHsmCryptography {
    ManagedHsmEncryptionResult encrypt(byte[] plainText);

    byte[] decrypt(byte[] cipherText, byte[] iv);
}

final class ManagedHsmEncryptionResult {

    private final byte[] cipherText;
    private final byte[] iv;

    ManagedHsmEncryptionResult(byte[] cipherText, byte[] iv) {
        this.cipherText = cipherText;
        this.iv = iv;
    }

    byte[] cipherText() {
        return cipherText;
    }

    byte[] iv() {
        return iv;
    }
}

final class AzureManagedHsmCryptography implements ManagedHsmCryptography {

    private final CryptographyClient cryptoClient;
    private final SecureRandom random;

    AzureManagedHsmCryptography(String keyId) {
        this(keyId, new SecureRandom());
    }

    AzureManagedHsmCryptography(String keyId, SecureRandom random) {
        this.cryptoClient = new CryptographyClientBuilder()
            .credential(new DefaultAzureCredentialBuilder().build())
            .keyIdentifier(keyId)
            .buildClient();
        this.random = random;
    }

    @Override
    public ManagedHsmEncryptionResult encrypt(byte[] plainText) {
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        EncryptParameters encryptParameters = EncryptParameters.createA256CbcParameters(plainText, iv);
        byte[] cipherText = cryptoClient.encrypt(encryptParameters, new Context("key1", "value1")).getCipherText();
        return new ManagedHsmEncryptionResult(cipherText, iv);
    }

    @Override
    public byte[] decrypt(byte[] cipherText, byte[] iv) {
        DecryptParameters decryptParameters = DecryptParameters.createA256CbcParameters(cipherText, iv);
        return cryptoClient.decrypt(decryptParameters, new Context("key1", "value1")).getPlainText();
    }
}