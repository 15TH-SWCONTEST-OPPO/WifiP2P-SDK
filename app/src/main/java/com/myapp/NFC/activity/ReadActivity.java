package com.myapp.NFC.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.myapp.NFC.utils.NFCUtils;
import com.myapp.R;

import java.util.List;

public class ReadActivity extends Activity {
    private NfcAdapter _nfcAdapter;
    private PendingIntent _pendingIntent;
    private IntentFilter[] _readIntentFilters;
    private IntentFilter[] _writeIntentFilters;
    private Intent _intent;
    //
    private final String _MIME_TYPE = "text/plain";
    private Button writeButton;

    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nfc);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        _enableNdefExchangeMode();
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
        _readMessage();
    }

    private NdefMessage _getNdefMessage() {
        EditText messageTextField = (EditText) findViewById(R.id.message_text_field);
        String stringMessage = " " + messageTextField.getText().toString();

        NdefMessage message = NFCUtils.getNewMessage(_MIME_TYPE, stringMessage.getBytes());

        return message;
    }


    private void _readMessage() {
        List<String> msgs = NFCUtils.getStringsFromNfcIntent(_intent);
        if (msgs != null && msgs.size() >= 1) {
            Toast.makeText(this, "Message : " + msgs.get(0), Toast.LENGTH_LONG).show();
        }
    }
}
