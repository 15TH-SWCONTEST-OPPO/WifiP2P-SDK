package com.myapp.NFC.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;


import com.myapp.NFC.utils.NFCUtils;
import com.myapp.R;

import java.util.List;

public class ReadingWritingActivity extends Activity {
    private NfcAdapter _nfcAdapter;
    private PendingIntent _pendingIntent;
    private IntentFilter[] _readIntentFilters;
    private IntentFilter[] _writeIntentFilters;
    private Intent _intent;
    //
    private final String _MIME_TYPE = "text/plain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nfc);
        // 开始初始化
        _init();
    }

    private void _init() {
        // 获得NFCAdapter
        _nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // 查看nfc是否可用
        if (_nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC.", Toast.LENGTH_LONG).show();
            return;
        }

        // 查看nfc是否启用
        if (_nfcAdapter.isEnabled()) {
            _pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndefDetected.addDataType(_MIME_TYPE);
            } catch (MalformedMimeTypeException e) {
                Log.e(this.toString(), e.getMessage());
            }

            _readIntentFilters = new IntentFilter[]{ndefDetected};
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        _enableNdefExchangeMode();
        _enableTagWriteMode();
    }

    private void _enableNdefExchangeMode() {
        _nfcAdapter.setNdefPushMessage(_getNdefMessage(), this);
        _nfcAdapter.enableForegroundDispatch(this, _pendingIntent, _readIntentFilters, null);
    }

    private void _enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        _writeIntentFilters = new IntentFilter[]{tagDetected};
        _nfcAdapter.enableForegroundDispatch(this, _pendingIntent, _writeIntentFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        _intent = intent;

        _readMessage();
    }

    private NdefMessage _getNdefMessage() {
        NdefMessage message = NFCUtils.getNewMessage(_MIME_TYPE, "你好".getBytes());

        return message;
    }

    private void _writeMessage() {
        Tag detectedTag = _intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (NFCUtils.writeMessageToTag(_getNdefMessage(), detectedTag)) {
            Toast.makeText(this, "Successfully wrote message to NFC tag", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Write failed", Toast.LENGTH_LONG).show();
        }
    }

    private void _readMessage() {
        List<String> msgs = NFCUtils.getStringsFromNfcIntent(_intent);
        if (msgs == null || msgs.size() < 1) {
            return;
        }
        String res = msgs.get(0);
        Toast.makeText(this, "Message : " + res, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.putExtra("NAME", res);
        setResult(RESULT_OK, intent);
        finish();
    }
}
