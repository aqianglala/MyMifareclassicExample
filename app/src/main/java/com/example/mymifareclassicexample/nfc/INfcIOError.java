package com.example.mymifareclassicexample.nfc;

/**
 * @author zhangx by 2019-03-01
 * @description
 */
public interface INfcIOError {
    void readSuccess(String data);
    void writeSuccess();
    void connect();
    void fail(String msg);
}
