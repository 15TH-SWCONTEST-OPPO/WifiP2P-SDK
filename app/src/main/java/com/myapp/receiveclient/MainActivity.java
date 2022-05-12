package com.myapp.receiveclient;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.myapp.R;

public class MainActivity extends AppCompatActivity {
    private BluetoothUtil mBt;
    private ImageView mPhoto;
    private ImageView mVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBt = new BluetoothUtil(this);
        initBlue();
        initView();


    }

    private void initView() {
        //mPhoto = (ImageView) findViewById(R.id.photo);
        mVideo = (ImageView) findViewById(R.id.video);
    }

    private void initBlue() {
        if (!mBt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBt.setOnDataReceivedListener(new BluetoothUtil.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                if (message.equals("text") && data.length != 0) {
                    String text = new String(data);
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                } else if (message.equals("photo") && data.length != 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    mPhoto.setImageBitmap(bitmap);
                } else if (message.equals("video") && data.length != 0) {
                    mVideo.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                }
            }
        });

        mBt.setBluetoothConnectionListener(new BluetoothUtil.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(MainActivity.this, "蓝牙已连接", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                Toast.makeText(MainActivity.this, "蓝牙已断开", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() {

            }
        });

    }

    public void Click(View view) {
        if (mBt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            mBt.disconnect();
        } else {
            Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                mBt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                mBt.setupService();
                mBt.startService(BluetoothState.DEVICE_ANDROID);
            } else {
                finish();
            }
        }
    }

    public void onStart() {
        super.onStart();
        if (!mBt.isBluetoothEnabled()) {
            //打开蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!mBt.isServiceAvailable()) {
                //开启监听
                mBt.setupService();
                mBt.startService(BluetoothState.DEVICE_ANDROID);
            }
        }
    }
}
