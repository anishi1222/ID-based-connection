package com.func4mhsm;

public class Payload {

    byte[] cipherText;
    byte[] iv;

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] _cipherText) {
        this.cipherText = _cipherText;
    }
}

