/*
 * Copyright (C) 2014 Akexorcist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myapp.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import com.myapp.WifiState;

@SuppressLint("NewApi")
public class WifiUtils {
    // Wifi状态监听器
    private WifiStateListener mWifiStateListener = null;
    // 事件监听器
    private WifiUtils.OnDataReceivedListener mDataReceivedListener = null;
    // wifi连接事件监听器
    private WifiUtils.WifiConnectionListener mWifiConnectionListener = null;

    private Context mContext;

    // Member object for the chat services
    // Wifi服务
    private WifiService mChatService = null;

    // Name and Address of the connected device
    private String mDeviceName = null;
    private String mDeviceAddress = null;
    private boolean isAutoConnectionEnabled = false;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private boolean isServiceRunning = false;

    private boolean isAndroid = WifiState.DEVICE_ANDROID;

    private WifiConnectionListener bcl;

    private int c = 0;

    private WifiManager mWifiManager;

    private WifiInfo mWifiInfo;

    private WifiP2pDevice mWifiP2pDevice;

    private static int headInfoLength = 14;

    public WifiUtils(Context context, WifiManager wifiManager, WifiInfo wifiInfo, WifiP2pDevice wifiP2pDevice) {
        // 传入上下文
        mContext = context;
        mWifiManager = wifiManager;
        mWifiInfo = wifiInfo;
        mWifiP2pDevice = wifiP2pDevice;
    }

    public WifiUtils(Context context) {
        mContext = context;
    }


    // 自定义Wifi状态监听器接口
    public interface WifiStateListener {
        public void onServiceStateChanged(int state);
    }

    // 自定义数据接收监听器
    public interface OnDataReceivedListener {
        // 当接收到数据的时候
        public void onDataReceived(byte[] readBuf, String str);
    }

    // Wifi连接监听器
    public interface WifiConnectionListener {
        public void onDeviceConnected(String name, String address);

        public void onDeviceDisconnected();

        public void onDeviceConnectionFailed();
    }

    // 查看Service是否可用
    public boolean isServiceAvailable() {
        return mChatService != null;
    }

    // 创建Service
    public void setupService() {
        mChatService = new WifiService(mContext, mHandler, mWifiInfo);
    }

    // 获得Service的状态
    public int getServiceState() {
        if (mChatService != null)
            return mChatService.getState();
        else
            return -1;
    }

    // 启动服务
    public void startService(boolean isAndroid) {
        if (mChatService != null) {
            if (mChatService.getState() == WifiState.STATE_NONE) {
                isServiceRunning = true;
                mChatService.start(isAndroid);
                WifiUtils.this.isAndroid = isAndroid;
            }
        }
    }

    public void stopService() {
        if (mChatService != null) {
            isServiceRunning = false;
            mChatService.stop();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (mChatService != null) {
                    isServiceRunning = false;
                    mChatService.stop();
                }
            }
        }, 500);
    }

    //
    public void setDeviceTarget(boolean isAndroid) {
        stopService();
        startService(isAndroid);
        WifiUtils.this.isAndroid = isAndroid;
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WifiState.MESSAGE_WRITE:
                    break;
                case WifiState.MESSAGE_READ:
                    String str = null;
                    int arg1 = msg.arg1;
                    if (arg1 == 0) {
                        str = "text";
                    } else if (arg1 == 1) {
                        str = "photo";
                    } else if (arg1 == 2) {
                        str = "video";
                    }
                    // 将数据放到一个Byte数组中
                    byte[] readBuf = (byte[]) msg.obj;
//                String readMessage = new String(readBuf);

                    if (readBuf != null && readBuf.length > 0) {
                        if (mDataReceivedListener != null)
                            // 为Listener设置值
                            Log.d("WifiUtil",str);
                            mDataReceivedListener.onDataReceived(readBuf, str);
                    }
                    break;

                case WifiState.MESSAGE_DEVICE_NAME:
                    mDeviceName = msg.getData().getString(WifiState.DEVICE_NAME);
                    mDeviceAddress = msg.getData().getString(WifiState.DEVICE_ADDRESS);
                    if(mWifiConnectionListener != null)
                        mWifiConnectionListener.onDeviceConnected(mDeviceName, mDeviceAddress);
                    isConnected = true;
                    break;


                case WifiState.MESSAGE_TOAST:
                    Toast.makeText(mContext, msg.getData().getString(WifiState.TOAST)
                            , Toast.LENGTH_SHORT).show();
                    break;
                case WifiState.MESSAGE_STATE_CHANGE:
                    if (mWifiStateListener != null)
                        mWifiStateListener.onServiceStateChanged(msg.arg1);
                    if (isConnected && msg.arg1 != WifiState.STATE_CONNECTED) {
                        if (mWifiConnectionListener != null)
                            mWifiConnectionListener.onDeviceDisconnected();
                        if (isAutoConnectionEnabled) {
                            isAutoConnectionEnabled = false;
                        }
                        isConnected = false;
                        mDeviceName = null;
                        mDeviceAddress = null;
                    }

                    if (!isConnecting && msg.arg1 == WifiState.STATE_CONNECTING) {
                        isConnecting = true;
                    } else if (isConnecting) {
                        if (msg.arg1 != WifiState.STATE_CONNECTED) {
                            if (mWifiConnectionListener != null)
                                mWifiConnectionListener.onDeviceConnectionFailed();
                        }
                        isConnecting = false;
                    }
                    break;
            }
        }
    };

    public void connect(WifiP2pDevice wifiP2pDevice,WifiP2pInfo wifiP2pInfo,Handler mHandler) {
        // 与wifi设备连接
        mChatService.connect(wifiP2pDevice,wifiP2pInfo,mHandler);
    }

    public static byte[] intToByteArray(int i) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(buf);
        dos.writeInt(i);
        byte[] b = buf.toByteArray();
        dos.close();
        buf.close();
        return b;
    }

    public void disconnect() {
        if (mChatService != null) {
            isServiceRunning = false;
            mChatService.stop();
        }
    }

    public void setOnDataReceivedListener(OnDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }

    public void setWifiConnectionListener(WifiConnectionListener listener) {
        mWifiConnectionListener = listener;
    }

    //添加头发送数据
    public void send(byte[] data, String str) {
        int length = data.length;
        byte[] length_b = null;
        try {
            length_b = intToByteArray(length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (length_b == null) return;

        byte[] headerInfo = new byte[headInfoLength];
        for (int i = 0; i < headInfoLength - 8; i++) {
            headerInfo[i] = (byte) i;
        }

        for (int i = 0; i < 4; i++) {
            headerInfo[6 + i] = length_b[i];
        }

        if (str.equals("text")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 0;
            }
        } else if (str.equals("photo")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 1;
            }
        } else if (str.equals("video")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 2;
            }
        }

        byte[] sendMsg = new byte[length + headInfoLength];
        for (int i = 0; i < sendMsg.length; i++) {
            if (i < headInfoLength) {
                sendMsg[i] = headerInfo[i];
            } else {
                sendMsg[i] = data[i - headInfoLength];
            }
        }
        Log.d("Server", "发发发");
        mChatService.write(sendMsg);
    }

    public String getConnectedDeviceName() {
        return mDeviceName;
    }

    public String getConnectedDeviceAddress() {
        return mDeviceAddress;
    }
}
