package com.NFC;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.R;
import com.myapp.utils.NFCUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NFC";
    private NfcAdapter mNfcAdapter; // 声明一个NFC适配器对象
    private TextView tv_nfc_result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc2);
        tv_nfc_result = findViewById(R.id.tv_nfc_result);
        initNfc();

    }

    // 1.初始化NFC适配器
    private void initNfc() {
        // 获取系统默认的NFC适配器
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            tv_nfc_result.setText("当前手机不支持NFC");
        } else if (!mNfcAdapter.isEnabled()) {
            tv_nfc_result.setText("请先在系统设置中启用NFC功能");
        } else {
            tv_nfc_result.setText("当前手机支持NFC");
        }
    }

    //2.
    @Override
    protected void onResume() {
        super.onResume();
        // 探测到NFC卡片后，必须以FLAG_ACTIVITY_SINGLE_TOP方式启动Activity，
        // 或者在AndroidManifest.xml中设置launchMode属性为singleTop或者singleTask，
        // 保证无论NFC标签靠近手机多少次，Activity实例都只有一个。
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // 声明一个NFC卡片探测事件的相应动作
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 读标签之前先确定标签类型。这里以大多数的NfcA为例
        String[][] techLists = new String[][]{new String[]{NfcA.class.getName()}, {IsoDep.class.getName()}};

        try {
            // 定义一个过滤器（检测到NFC卡片）
            IntentFilter[] filters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED, "*/*")};
            // 为本App启用NFC感应
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            return;
        }
        // 禁用本App的NFC感应
        mNfcAdapter.disableForegroundDispatch(this);
    }

    //3.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            String s = NFCUtils.readNFCId(intent);
            Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //resolveIntent(intent);
//        String action = intent.getAction(); // 获取到本次启动的action
//        if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED) // NDEF类型
//                || action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) // 其他类型
//                || action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) { // 未知类型
//            // 从intent中读取NFC卡片内容
//            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//            // 获取NFC卡片的序列号
//            byte[] ids = tag.getId();
//            String card_info = String.format("卡片的序列号为: %s",
//                    ByteArrayToHexString(ids));
//            String result = readGuardCard(tag);
//            card_info = String.format("%s\n详细信息如下：\n%s", card_info, result);
//            tv_nfc_result.setText(card_info);
//        }
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Tag tag = null;
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){
            NdefMessage[] messages = null;
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if(rawMsgs!=null){
                messages = new NdefMessage[rawMsgs.length];
                for(int i=0;i<rawMsgs.length;i++){
                    messages[i] = (NdefMessage) rawMsgs[i];
                }
            }else{
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,empty,empty,empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                messages = new NdefMessage[]{msg};
            }
        }

//        if (supportedTechs(tag.getTechList())) {
//            MifareClassic mfc = MifareClassic.get(tag);
//            if (mfc != null) {
//                try {
//                    mfc.connect();
//                    int nSecont = mfc.getSectorCount();
//                    boolean isAuth = false;
//                    for (int i=0;i<nSecont;i++){
//                        if(mfc.authenticateSectorWithKeyA(i,MifareClassic.KEY_DEFAULT)){
//                            isAuth = true;
//                        }else if(mfc.authenticateSectorWithKeyA(i,MifareClassic.KEY_DEFAULT)){
//                            isAuth = true;
//                        }else{
//                            isAuth = false;
//                        }
//                        if(isAuth){
//                            int nBlock = mfc.getBlockCountInSector(i);
//                            for (int j=0;j<nBlock;j++){
//                                byte[] data = mfc.readBlock(j);
//                                Log.i(TAG,data.toString());
//                            }
//                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                Log.i(TAG, "TAG类型不匹配");
//            }
//        }
    }

    private boolean supportedTechs(String[] techList) {
        boolean isSupport = false;
        for (String s : techList) {
            Log.i(TAG, "All SupportedTechs:" + s);
        }
        String[][] mTechList = NFCUtils.mTechList;

        for (String s : techList) {
            if (s.equals("android.nfc.tech.MifareClassic")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.Ndef")) {
                isSupport = true;
            } else if (s.equals("android.nfc.tech.NfcA")) {
                isSupport = true;
            } else {
                isSupport = false;
            }
        }
        return isSupport;
    }

    // 读取小区门禁卡信息
    public String readGuardCard(Tag tag) {
        MifareClassic classic = MifareClassic.get(tag);
        for (String tech : tag.getTechList()) {
            Log.d(TAG, "当前设备支持" + tech); //显示设备支持技术
        }
        String info;
        try {
            classic.connect(); // 连接卡片数据
            int type = classic.getType(); //获取TAG的类型
            String typeDesc;
            if (type == MifareClassic.TYPE_CLASSIC) {
                typeDesc = "传统类型";
            } else if (type == MifareClassic.TYPE_PLUS) {
                typeDesc = "增强类型";
            } else if (type == MifareClassic.TYPE_PRO) {
                typeDesc = "专业类型";
            } else {
                typeDesc = "未知类型";
            }
            info = String.format("\t卡片类型：%s\n\t扇区数量：%d\n\t分块个数：%d\n\t存储空间：%d字节",
                    typeDesc, classic.getSectorCount(), classic.getBlockCount(), classic.getSize());
        } catch (Exception e) {
            e.printStackTrace();
            info = e.getMessage();
        } finally { // 无论是否发生异常，都要释放资源
            try {
                classic.close(); // 释放卡片数据
            } catch (Exception e) {
                e.printStackTrace();
                info = e.getMessage();
            }
        }
        return info;
    }

    public static String ByteArrayToHexString(byte[] bytesId) {
        int i, j, in;
        String[] hex = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"
        };
        String output = "";

        for (j = 0; j < bytesId.length; ++j) {
            in = bytesId[j] & 0xff;
            i = (in >> 4) & 0x0f;
            output += hex[i];
            i = in & 0x0f;
            output += hex[i];
        }
        return output;
    }
}