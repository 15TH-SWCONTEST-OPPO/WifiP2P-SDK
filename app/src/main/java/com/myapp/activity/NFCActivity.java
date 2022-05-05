package com.myapp.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.R;
import com.myapp.utils.NFCUtils;

public class NFCActivity extends AppCompatActivity {

    private NFCUtils nfcUtils;

    private AlertDialog mDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcUtils = new NFCUtils(NFCActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialog();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //processIntent(intent);
    }
    public void onResume(){
        super.onResume();
        nfcUtils.mNfcAdapter.enableForegroundDispatch(this, nfcUtils.mPendingIntent, nfcUtils.mIntentFilter, nfcUtils.mTechList);
    }

    public void onPause(){
        super.onPause();
        if (nfcUtils.isNfcEnable(this)){
            nfcUtils.mNfcAdapter.disableForegroundDispatch(this);
        }
    }


    private void showDialog(String title, String content, View.OnClickListener listener) {
        dismissDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.activity_nfc, null);
        TextView tvTitle = (TextView) view.findViewById(R.id.dialog_title);
        TextView tvContent = (TextView) view.findViewById(R.id.dialog_content);
        tvTitle.setText(title);
        tvContent.setText(content);
        Button btnCancel = (Button) view.findViewById(R.id.btn_cancel);
        Button btnOk = (Button) view.findViewById(R.id.btn_confirm);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });
        btnOk.setOnClickListener(listener);

        builder.setView(view);
        mDialog = builder.create();
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
    }

    private void dismissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
