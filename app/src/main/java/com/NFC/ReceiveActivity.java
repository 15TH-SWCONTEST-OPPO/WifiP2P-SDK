package com.NFC;

import androidx.appcompat.app.AppCompatActivity;


import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;

import android.widget.TextView;
import android.widget.Toast;

import com.myapp.R;

import java.nio.charset.Charset;
import java.util.Arrays;

public class ReceiveActivity extends AppCompatActivity {
    private static final String TAG = "RedWolf";
    //  NfcAdapter
    private NfcAdapter mNfcAdapter;

    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_receive);
        checkNFCFunction();
        textView = (TextView) findViewById(R.id.tv);
    }
    //  * * * * * * * * * * * * * * * * * * * * * * * NFC start * * * * * * * * * * * * * * * * * * * * * * * ↓

    //  回调,当NFC消息过来的时候自动调用.109122
    @Override
    protected void onNewIntent(Intent intent) {
        //  接收到消息的第一步
        //setIntent(intent);
        resolveIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            return;
        }
        //  需要从Intent中读出信息
        //  消息判别
//        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
//            resolveIntent(getIntent());
//        }
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

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            NdefMessage[] messages = null;

            Parcelable[] rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsg != null) {
                messages = new NdefMessage[rawMsg.length];
                for (int i = 0; i < messages.length; i++) {
                    messages[i] = (NdefMessage) rawMsg[i];
                }
            } else {
                //  未知Action
                byte[] empty = new byte[]{};
                NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage ndefMessage = new NdefMessage(ndefRecord);
                messages = new NdefMessage[]{ndefMessage};
            }
            //  将Message中的Record解析出来
            progressNdefMessage(messages);
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

        } else {
            return;
        }
    }


    private void progressNdefMessage(NdefMessage[] messages) {
        if (messages == null || messages.length == 0) {
            return;
        }
        for (int i = 0; i < messages.length; i++) {
            NdefRecord records[] = messages[i].getRecords();
            for (NdefRecord record : records) {
                if (isTextUri(record)) {
                    parseTextUri(record);
                }
            }
        }
    }

    private boolean isTextUri(NdefRecord record) {
        if (NdefRecord.TNF_WELL_KNOWN == record.getTnf()) {
            if (Arrays.equals(NdefRecord.RTD_TEXT, record.getType())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void parseTextUri(NdefRecord record) {

        //  读出所有的PayLoad
        String payLoadStr = "";
        byte[] payloads = record.getPayload();
        byte statusByte = payloads[0];
        //  得到编码方式
        String textEncoding = ((statusByte & 0200) == 0) ? "UTF-8" : "UTF-16";
        //  获取语言码的长度
        int languageCodeLength = statusByte & 0077;
        //  真正的解析
        payLoadStr = new String(payloads, languageCodeLength + 1, payloads.length - languageCodeLength - 1, Charset.forName(textEncoding));
        //  解析完成- -NFC阶段结束
        //textView.setText("接受到的信息为_" + payLoadStr);
        Toast.makeText(this, payLoadStr, Toast.LENGTH_SHORT).show();
    }

    private void checkNFCFunction() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //  机器不支持Nfc功能
        if (mNfcAdapter == null) {
            return;
        } else {
            //  检查机器NFC是否开启
            if (!mNfcAdapter.isEnabled()) {
                //  机器Nfc未开启 提示用户开启 这里采用对话框的方式<PS:这个开启了  可以进行对NFC的信息读写>
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("警告").setMessage("本机NFC功能未开启,是否开启(不开启将无法继续)").setNegativeButton("开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent setNfc = new Intent(Settings.ACTION_NFC_SETTINGS);
                        startActivity(setNfc);
                    }
                }).setPositiveButton("不开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).create().show();
                return;
            } else {
                //  NFC 已开启  检查NFC_Beam是否开启  只有这个开启了  才能进行p2p的传输
                if (!mNfcAdapter.isNdefPushEnabled()) {
                    // NFC_Beam未开启  点击开启
                    new AlertDialog.Builder(this).setTitle("警告!").setMessage("NFC_Beam功能未开启,是否开启(不开启将无法继续)").setNegativeButton("开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent setNfc = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                            startActivity(setNfc);
                        }
                    }).setPositiveButton("不开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setCancelable(false).create().show();
                    return;
                }
            }
        }
    }


    //  * * * * * * * * * * * * * * * * * * * * * * * NFC end * * * * * * * * * * * * * * * * * * * * * * * ↑
}
