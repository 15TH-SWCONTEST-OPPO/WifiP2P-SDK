package com.myapp.activity;

import static com.myapp.utils.NFCUtils.mNfcAdapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import com.NFC.WriteActivity;
import com.NFC.utils.NFCUtils;
import com.google.gson.Gson;
import com.myapp.ProgressDialog;
import com.myapp.R;
import com.myapp.Wifip2pService;
import com.myapp.socket.ReceiveSocket;

/**
 * 接收文件界面
 * 1、创建组群信息
 * 2、移除组群信息
 * 3、启动服务，创建serversocket，监听客户端端口，把信息写入文件
 */
public class ReceiveFileActivity extends BaseActivity implements ReceiveSocket.ProgressReceiveListener, View.OnClickListener {

    private static final String TAG = "ReceiveFileActivity";
    private Wifip2pService.MyBinder mBinder;
    private ProgressDialog mProgressDialog;
    private Intent mIntent;
    //
    private final String _MIME_TYPE = "text/plain";
    private Button writeButton;

    private AlertDialog alertDialog;

    private Button nfcShare;
    // 创建一个服务连接，与写好的WifiService进行连接
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //调用服务里面的方法进行绑定,binder中含有ReceiveSocket
            mBinder = (Wifip2pService.MyBinder) service;
            mBinder.initListener(ReceiveFileActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //服务断开重新绑定
            bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        Button btnRemove = (Button) findViewById(R.id.btn_remove);
        nfcShare = findViewById(R.id.nfc_share);
        nfcShare.setEnabled(false);
        btnCreate.setOnClickListener(this);
        btnRemove.setOnClickListener(this);
        nfcShare.setOnClickListener(this);

        // 创建一个Intent，将该Activity与Service进行连接
        mIntent = new Intent(ReceiveFileActivity.this, Wifip2pService.class);
        // 开启服务
        startService(mIntent);
        // 绑定服务
        bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_create:
                createGroup();
                break;
            case R.id.btn_remove:
                removeGroup(true);
                break;
            case R.id.nfc_share:
                startNFC();
                break;
        }
    }

    // fuck android  android is a piece of shit
    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("p2p0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    private void startNFC() {
        Intent intent = new Intent(this, com.NFC.activity.WritingActivity.class);

        if (mWifiP2pDevice != null && mWifiP2pDevice.deviceAddress != null) {
            intent.putExtra("data", mWifiP2pDevice.deviceName);
            startActivityForResult(intent, 20);//启动Activity
        } else {
            Toast.makeText(this, "尚未得知群主信息", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 创建组群，等待连接
     */
    @SuppressLint("MissingPermission")
    public void createGroup() {

        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "创建群组成功");
                nfcShare.setEnabled(true);
                Toast.makeText(ReceiveFileActivity.this, "创建群组成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "创建群组失败: " + reason);
                Toast.makeText(ReceiveFileActivity.this, "创建群组失败,请移除已有的组群或者连接同一WIFI重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 移除组群
     */
    public void removeGroup(boolean isShow) {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "移除组群成功");
                if(isShow){
                    Toast.makeText(ReceiveFileActivity.this, "移除组群成功", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "移除组群失败");
                if(isShow){
                    Toast.makeText(ReceiveFileActivity.this, "移除组群失败,请创建组群重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSatrt() {
        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    public void onProgressChanged(File file, int progress) {
        Log.e(TAG, "接收进度：" + progress);
        mProgressDialog.setProgress(progress);
        mProgressDialog.setProgressText(progress + "%");
    }

    @Override
    public void onFinished(File file) {
        Log.e(TAG, "接收完成");
        mProgressDialog.dismiss();
        Toast.makeText(this, file.getName() + "接收完毕！", Toast.LENGTH_SHORT).show();
        //接收完毕后再次启动服务等待下载一次连接，不启动只能接收一次，第二次无效，原因待尚不清楚
        clear();
        startService(mIntent);
        bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onFaliure(File file) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        //Toast.makeText(this, "接收失败，请重试！", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        clear();
        removeGroup(false);
    }

    /**
     * 释放资源
     */
    private void clear() {
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
        if (mIntent != null) {
            stopService(mIntent);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}

