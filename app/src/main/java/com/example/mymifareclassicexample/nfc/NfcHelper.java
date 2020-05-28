package com.example.mymifareclassicexample.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.provider.Settings;
import android.widget.Toast;

import java.io.IOException;

/**
 * @author zhangx by 2019-02-22
 * @description NFC 工具类
 */
public class NfcHelper {
    private NfcAdapter mNfcAdapter;
    private IntentFilter[] mIntentFilter = null;
    private PendingIntent mPendingIntent = null;
    private String[][] mTechList = null;

    private INfcInitError iNfcInitError;
    private Activity activity;


    byte[] code = MifareClassic.KEY_DEFAULT;//读写标签中每个块的密码

    public NfcHelper(Activity activity, INfcInitError iNfcInitError) {
        this.iNfcInitError = iNfcInitError;
        this.activity = activity;
        checkNFCFunction();
        init();
    }

    private void init() {
        mPendingIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, activity
                .getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter2.addCategory(Intent.CATEGORY_DEFAULT);
        mIntentFilter = new IntentFilter[]{filter1 , filter2};
        mTechList = new String[][]{new String[]{android.nfc.tech.MifareClassic.class.getName()}, new String[]{android.nfc.tech.MifareUltralight.class.getName()}};
        enableForegroundDispatch();
        iNfcInitError.openSuccess();
    }

    private void checkNFCFunction() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        // check the NFC adapter first
        if (mNfcAdapter == null) {
            // mTextView.setText("NFC apdater is not available");
            iNfcInitError.notSupportNfc();
            return;
        }
        else {
            if (!mNfcAdapter.isEnabled()) {
                iNfcInitError.nfcNotOpen();
                return;
            }
        }
    }

    public void openNfc() {
        Intent setnfc = new Intent(
                Settings.ACTION_NFC_SETTINGS);
        activity.startActivity(setnfc);
        enableForegroundDispatch();
        iNfcInitError.openSuccess();
    }
    public  void restore(Tag tag){
        MifareClassic mfc = MifareClassic.get(tag);
        try {
            if (mfc != null) {
                mfc.connect();
            }
            else {
                Toast.makeText(activity, "写入失败", Toast.LENGTH_SHORT).show();
                return;
            }
            int blockCount = mfc.getBlockCount();
                for (int i = 0; i < blockCount; i++) {
                    mfc.restore(i);
                }
                mfc.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                mfc.close();
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    // 写数据
    public void writeTag(Tag tag, String str , INfcIOError iNfcIOError) {
        int len = str.length();
        MifareClassic mfc = MifareClassic.get(tag);
        try {
            if (mfc != null) {
                mfc.connect();
                iNfcIOError.connect();
            }
            else {
                iNfcIOError.fail("write fail!");
                return;
            }
            int sectorCount = mfc.getSectorCount();
            int size = mfc.getSize();
            byte[] b1 = str.getBytes("GBK");
            if (b1.length <= (size - 1024)) {
                int num = 0;
                for (int i = 1; i < sectorCount; i++) {
                    boolean isAuth = mfc.authenticateSectorWithKeyA(i , code);
                    if (isAuth){
                        //获取当前扇区的包含块的数量
                        int sectorBlockCount = mfc.getBlockCountInSector(i);
                        int bIndex = mfc.sectorToBlock(i);
                        for (int j = 0; j < sectorBlockCount -1; j++) {
                            byte[] b0 = new byte[16];
                            int srcPos = b0.length * num;
                            if (srcPos + b0.length <= b1.length) {
                                System.arraycopy(b1, srcPos, b0, 0, b0.length);
                            }
                            else if (srcPos <= b1.length && srcPos + b0.length > b1.length) {
                                System.arraycopy(b1, srcPos, b0, 0, b1.length - srcPos);
                            }
                            mfc.writeBlock(bIndex, b0);
                            bIndex++;
                            num++;
                        }
                    }else{
                        iNfcIOError.fail("Invalid IC card password!");
                        mfc.close();
                        return;
                    }
                }
                mfc.close();
                iNfcIOError.writeSuccess();
            }else{
                iNfcIOError.fail("IC card out of memory!");
            }
        }
        catch (IOException e) {
            iNfcIOError.fail(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                mfc.close();
            }
            catch (IOException e) {
                iNfcIOError.fail(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // //读取数据
    public void readTag(Tag tag , INfcIOError iNfcIOError) {
        MifareClassic mfc = MifareClassic.get(tag);
        // 读取TAG
        try {
            StringBuilder metaInfo = new StringBuilder();
            mfc.connect();
            iNfcIOError.connect();
            int sectorCount = mfc.getSectorCount();// 获取TAG中包含的扇区数
            for (int j = 1; j < sectorCount; j++) {
                // Authenticate a sector with key A.
                boolean auth = mfc.authenticateSectorWithKeyA(j,
                        code);// 逐个获取密码
                if (auth) {
                    // 读取扇区中的块
                    int bCount = mfc.getBlockCountInSector(j);
                    int bIndex = mfc.sectorToBlock(j);
                    for (int i = 0; i < bCount-1; i++) {
                        byte[] data = mfc.readBlock(bIndex);
                        String blockData = new String(data , "GBK");
                        blockData = blockData.replace(new String(new byte[]{0} , "GBK") , "");
                        metaInfo.append(blockData);
                        bIndex++;
                    }
                }
                else {
                    iNfcIOError.fail("Invalid IC card password!");
                    mfc.close();
                    return;
                }
            }
            iNfcIOError.readSuccess(metaInfo.toString());
        }
        catch (Exception e) {
            iNfcIOError.fail(e.getMessage());
            e.printStackTrace();
        } finally {
            if (mfc != null) {
                try {
                    mfc.close();
                }
                catch (IOException e) {
                    iNfcIOError.fail(e.getMessage());
                }
            }
        }
    }

    public void enableForegroundDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(activity, mPendingIntent,
                    mIntentFilter, mTechList);
        }
    }

    public void disableForegroundDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(activity);
        }
    }

}
