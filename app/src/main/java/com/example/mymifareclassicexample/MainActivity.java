package com.example.mymifareclassicexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

import com.example.mymifareclassicexample.nfc.INfcIOError;
import com.example.mymifareclassicexample.nfc.INfcInitError;
import com.example.mymifareclassicexample.nfc.NfcHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private NfcHelper mNfcHelper;
    protected ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcHelper = new NfcHelper(this, new INfcInitError() {
            @Override
            public void notSupportNfc() {
            }

            @Override
            public void nfcNotOpen() {
                mNfcHelper.openNfc();
            }

            @Override
            public void openSuccess() {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcHelper.disableForegroundDispatch();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            fixedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    mNfcHelper.readTag(tag, new INfcIOError() {
                        @Override
                        public void readSuccess(String data) {
                        }

                        @Override
                        public void writeSuccess() {

                        }

                        @Override
                        public void connect() {
                        }

                        @Override
                        public void fail(String msg) {
                        }
                    });
//                    mNfcHelper.writeTag(tag, "123", new INfcIOError() {
//                        @Override
//                        public void readSuccess(String data) {
//                        }
//
//                        @Override
//                        public void writeSuccess() {
//                        }
//
//                        @Override
//                        public void connect() {
//                        }
//
//                        @Override
//                        public void fail(String msg) {
//                        }
//                    });
                }
            });
        }
    }


}
