package com.myapp.activity;

import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.util.Collection;

import com.myapp.Wifip2pReceiver;
import com.myapp.Wifip2pActionListener;

public class BaseActivity extends AppCompatActivity implements Wifip2pActionListener {

    private static final String TAG = "BaseActivity";

    public WifiP2pManager mWifiP2pManager;
    public WifiP2pManager.Channel mChannel;
    public Wifip2pReceiver mWifip2pReceiver;
    public WifiP2pInfo mWifiP2pInfo;

    public WifiP2pDevice mWifiP2pDevice;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册WifiP2pManager
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        mChannel = mWifiP2pManager.initialize(BaseActivity.this, getMainLooper(), BaseActivity.this);

        //注册广播
        mWifip2pReceiver = new Wifip2pReceiver(mWifiP2pManager, mChannel, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mWifip2pReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播
        unregisterReceiver(mWifip2pReceiver);
        mWifip2pReceiver = null;
    }

    @Override
    public void wifiP2pEnabled(boolean enabled) {
        Log.e(TAG, "传输通道是否可用：" + enabled);
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            mWifiP2pInfo = wifiP2pInfo;
            //Log.d("测试", "在BaseActivity中");
            Log.e(TAG, "啦啦啦啦啦啦啦啦啦啦WifiP2pInfo:" + wifiP2pInfo);
        }
    }

    @Override
    public void onDisconnection() {
        Log.e(TAG, "连接断开");
    }

    @Override
    public void onDeviceInfo(WifiP2pDevice wifiP2pDevice) {
        Log.e(TAG, "当前的的设备名称" + wifiP2pDevice.deviceName);
        Log.e(TAG, "当前的的设备地址" + wifiP2pDevice.deviceAddress);
//        WifiManager mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//
//        final String[] macAddresses = mWifiManager.getFactoryMacAddresses();
//        String macAddress = null;
//        if (macAddresses != null && macAddresses.length > 0) {
//            macAddress = macAddresses[0];
//        }
//        Log.e(TAG, "当前的的设备地址" + info.getMacAddress());
        mWifiP2pDevice = wifiP2pDevice;
    }

    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        for (WifiP2pDevice device : wifiP2pDeviceList) {
            Log.e(TAG, "可用的设备信息：" + device.deviceName + "--------" + device.deviceAddress);
        }
    }

    @Override
    public void onChannelDisconnected() {

    }
}
