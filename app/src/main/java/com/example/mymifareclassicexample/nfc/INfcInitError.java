package com.example.mymifareclassicexample.nfc;

/**
 * @author zhangx by 2019-02-22
 * @description
 */
public interface INfcInitError {
    void notSupportNfc();
    void nfcNotOpen();
    void openSuccess();
}
