package com.myapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;


public class Wifip2pReceiver extends BroadcastReceiver {

    public static final String TAG = "Wifip2pReceiver";

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private Wifip2pActionListener mListener;


    public Wifip2pReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel,
                           Wifip2pActionListener listener) {
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "接收到广播： " + intent.getAction());
        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
        switch (intent.getAction()) {
            //WiFi P2P是否可用
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    mListener.wifiP2pEnabled(true);
                } else {
                    mListener.wifiP2pEnabled(false);
                }
                break;
            // peers列表发生变化
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        mListener.onPeersInfo(peers.getDeviceList());
                    }
                });
                break;

//            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
//                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
//                WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
//                if (networkInfo.isConnected()){
//                    mListener.onConnection(wifiP2pInfo);
//                }
//                //WifiP2pGroup wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
//                break;

            // WiFi P2P连接发生变化
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()){
                    mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            Log.d("测试","Receiver");
                            mListener.onConnection(info);
                        }
                    });
                }else {
                    mListener.onDisconnection();
                }
                break;

            // WiFi P2P设备信息发生变化
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mListener.onDeviceInfo(device);
                break;

            default:
                break;
        }
    }
}
