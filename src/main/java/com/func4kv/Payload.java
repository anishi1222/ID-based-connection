package com.func4kv;

public class Payload {
    int length;
    byte[] cipherText;

    public int getLength() {
        return length;
    }

    public void setLength(int _length) {
        this.length = _length;
    }

    public byte[] getCipherText() {
        return cipherText;
    }

    public void setCipherText(byte[] _cipherText) {
        this.cipherText = _cipherText;
    }
}
