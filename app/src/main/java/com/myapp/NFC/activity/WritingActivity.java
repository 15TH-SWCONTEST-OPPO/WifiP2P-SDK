package com.myapp.NFC.activity;

import static com.myapp.utils.NFCUtils.mNfcAdapter;

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

public class WritingActivity extends Activity {
    private NfcAdapter _nfcAdapter;
    private PendingIntent _pendingIntent;
    private IntentFilter[] _readIntentFilters;
    private IntentFilter[] _writeIntentFilters;
    private Intent _intent;
    //
    private final String _MIME_TYPE = "text/plain";

    private String IP = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_write);


        Intent intent = getIntent();
        //getXxxExtra方法获取Intent传递过来的数据
        IP = intent.getStringExtra("data");

        _init();

    }

    private void _init() {
        _nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (_nfcAdapter == null) {
            Toast.makeText(this, "This device does not support NFC.", Toast.LENGTH_LONG).show();
            return;
        }

        if (_nfcAdapter.isEnabled()) {
            _pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);

            IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndefDetected.addDataType(_MIME_TYPE);
            } catch (MalformedMimeTypeException e) {
                Log.e(this.toString(), e.getMessage());
            }

            _readIntentFilters = new IntentFilter[]{ndefDetected};
        }
    }

    public void disableForegroudDispatch() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
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

        _writeMessage();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

        }
    }

    private NdefMessage _getNdefMessage() {
        //EditText messageTextField = (EditText) findViewById(R.id.message_text_field);
        //String stringMessage = " " + messageTextField.getText().toString();

        NdefMessage message = NFCUtils.getNewMessage(_MIME_TYPE, IP.getBytes());

        return message;
    }

    private void _writeMessage() {
        Tag detectedTag = _intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NFCUtils.writeMessageToTag(_getNdefMessage(), detectedTag);
        setResult(20);
        finish();
    }

    private void _readMessage() {
        List<String> msgs = NFCUtils.getStringsFromNfcIntent(_intent);
        if (msgs != null && msgs.size() >= 1) {
            Toast.makeText(this, "Message : " + msgs.get(0), Toast.LENGTH_LONG).show();
        }
    }
}
