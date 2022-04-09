/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.myapp.Constant;
import com.myapp.WifiState;

@SuppressLint("NewApi")
public class WifiService {
    private static final String TAG = "Wifi Service";
    private static final String NAME_SECURE = "Wifi Secure";

    private final Handler mHandler;

    //Accept线程
    private AcceptThread mSecureAcceptThread;
    //连接线程
    private ConnectThread mConnectThread;
    //连接后传输的线程
    private ConnectedThread mConnectedThread;

    private int mState;

    private boolean isAndroid = WifiState.DEVICE_ANDROID;

    private WifiInfo mWifiInfo;

    private WifiP2pDevice mWifiP2pDevice;

    public WifiService(Context context, Handler handler) {
        mState = WifiState.STATE_NONE;
        mHandler = handler;
    }

    // 传入Handler，方便监听事件，传入WifiInfo和WiFiP2pDevice，方便连接
    public WifiService(Context mContext, Handler handler, WifiInfo wifiInfo) {
        mState = WifiState.STATE_NONE;
        mHandler = handler;
        mWifiInfo = wifiInfo;
    }

    //加锁的更新状态的方法
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(WifiState.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start(boolean isAndroid) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(WifiState.STATE_LISTEN);
        if (mSecureAcceptThread == null) {
            //创建接收线程
            mSecureAcceptThread = new AcceptThread(isAndroid);
            mSecureAcceptThread.start();
            WifiService.this.isAndroid = isAndroid;
        }
    }

    public synchronized void disconnect() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    // 连接方法，此处统一语义为建立Socket连接，需要的参数仅有address,次数传入一个device
    // 连接方法为接收之后进行，在此方法之中会创建一个连接线程，并更新状态
    public synchronized void connect(WifiP2pDevice wifiP2pDevice, WifiP2pInfo wifiP2pInfo, Handler mHandler) {
        mWifiP2pDevice = wifiP2pDevice;
        if (mState == WifiState.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //根据传入的Device建立一个连接线程
        mConnectThread = new ConnectThread(wifiP2pInfo, mHandler);
        mConnectThread.start();
        setState(WifiState.STATE_CONNECTING);
    }

    //
    public synchronized void connected(Socket socket, String socketType) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        mConnectedThread = new ConnectedThread(socket, socketType);

        setState(WifiState.STATE_CONNECTED);
        if (socketType.equals("Receive")) {
            mConnectedThread.start();
        }

        Message msg = mHandler.obtainMessage(WifiState.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(WifiState.DEVICE_NAME, "");
        bundle.putString(WifiState.DEVICE_ADDRESS, "");
        if (mWifiP2pDevice != null) {
            bundle.putString(WifiState.DEVICE_NAME, mWifiP2pDevice.deviceName);
            bundle.putString(WifiState.DEVICE_ADDRESS, mWifiP2pDevice.deviceAddress);
        }
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    private void connected(Socket socket, String send, WifiP2pInfo mmWifiInfo, Handler mmHandler) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket, send);

        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(WifiState.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(WifiState.DEVICE_NAME, "");
        bundle.putString(WifiState.DEVICE_ADDRESS, "");
        msg.setData(bundle);
        msg.what = WifiState.STATE_CONNECTED;
        setState(WifiState.STATE_CONNECTED);
        Message m = new Message();
        m.what = 2;
        mmHandler.sendMessage(m);
        //mmHandler.sendMessage(m);
        mHandler.sendMessage(msg);
    }

    public synchronized void stop() {
        disconnect();
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread.kill();
            mSecureAcceptThread = null;
        }
        setState(WifiState.STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r = null;
        synchronized (this) {
            if (mState != WifiState.STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private void connectionFailed(Handler mHandler) {
        //WifiService.this.start(WifiService.this.isAndroid);
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        Message msg = new Message();
        msg.what = 1;
        mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        //WifiService.this.start(WifiService.this.isAndroid);
    }

    private class AcceptThread extends Thread {
        private ServerSocket mmServerSocket;
        private String mSocketType;
        boolean isRunning = true;

        public AcceptThread(boolean isAndroid) {
            try {
                mmServerSocket = new ServerSocket();
                mmServerSocket.setReuseAddress(true);
                mmServerSocket.bind(new InetSocketAddress(Constant.PORT));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "创建了连接线程");
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            Socket socket = null;

            while (mState != WifiState.STATE_CONNECTED && isRunning) {
                try {
                    socket = mmServerSocket.accept();
                } catch (Exception e) {

                }
                if (socket != null) {
                    synchronized (WifiService.this) {
                        switch (mState) {
                            case WifiState.STATE_LISTEN:

                            case WifiState.STATE_CONNECTING:
                                connected(socket,
                                        "Receive");
                                break;
                            case WifiState.STATE_NONE:
                            case WifiState.STATE_CONNECTED:
                                try {
                                    socket.close();
                                    Log.d(TAG, "关闭");
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
                mmServerSocket = null;
            } catch (IOException e) {

            }
        }

        public void kill() {
            isRunning = false;
        }
    }


    // 连接线程
    private class ConnectThread extends Thread {
        private Socket mmSocket;
        private WifiP2pDevice mmDevice;
        private WifiP2pInfo mmWifiInfo;
        private String mSocketType;
        private InetSocketAddress inetSocketAddress;
        private Handler mmHandler;

        public ConnectThread(WifiP2pInfo wifiInfo, Handler handler) {
            mmSocket = new Socket();
            mmWifiInfo = wifiInfo;
            mmHandler = handler;
            inetSocketAddress = new InetSocketAddress(mmWifiInfo.groupOwnerAddress.getHostAddress(), Constant.PORT);
        }

        public void run() {
            try {
                mmSocket.connect(inetSocketAddress);
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                Log.d(TAG, "客户端连接服务端失败");
                connectionFailed(mmHandler);
                return;
            }
            synchronized (WifiService.this) {
                mConnectThread = null;
            }
            // 调用连接之后传输数据的线程
            connected(mmSocket, "send", mmWifiInfo, mmHandler);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    public static int ByteArrayToInt(byte b[]) throws Exception {
        ByteArrayInputStream buf = new ByteArrayInputStream(b);

        DataInputStream dis = new DataInputStream(buf);
        return dis.readInt();

    }


    private class ConnectedThread extends Thread {
        private Socket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private WifiP2pDevice mmDevice;
        private Integer errorTime;

        public ConnectedThread(Socket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            errorTime = 0;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer;
            ArrayList<Integer> arr_byte = new ArrayList<Integer>();

            while (true) {
                try {
                    boolean valid = true;
                    for (int i = 0; i < 6; i++) {
                        int t = mmInStream.read();
                        if (t != i) {
                            valid = false;
                            break;
                        }
                    }

                    if (valid) {
                        errorTime = 0;
                        byte[] bufLength = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            bufLength[i] = ((Integer) mmInStream.read()).byteValue();
                        }

                        int textCount = 0;
                        int photoCount = 0;
                        int videoCount = 0;

                        for (int i = 0; i < 4; i++) {
                            int read = mmInStream.read();
                            if (read == 0) {
                                textCount++;
                            } else if (read == 1) {
                                photoCount++;
                            } else if (read == 2) {
                                videoCount++;
                            }
                        }
                        int length = ByteArrayToInt(bufLength);
                        buffer = new byte[length];
                        for (int i = 0; i < length; i++) {
                            buffer[i] = ((Integer) mmInStream.read()).byteValue();
                        }
                        Message msg = Message.obtain();
                        msg.what = WifiState.MESSAGE_READ;
                        msg.obj = buffer;

                        if (textCount == 4) {
                            msg.arg1 = 0;
                            Log.d(TAG, "text");
                            mHandler.sendMessage(msg);
                        } else if (photoCount == 4) {
                            msg.arg1 = 1;
                            Log.d(TAG, "photo");
                            mHandler.sendMessage(msg);
                        } else if (videoCount == 4) {
                            msg.arg1 = 2;
                            Log.d(TAG, "video");
                            mHandler.sendMessage(msg);
                        }
                    } else {
                        errorTime++;
                        if (errorTime >= 50) {
                            connectionLost();
                            //WifiService.this.start(WifiService.this.isAndroid);
                        }
                    }

                    /*if (valid) {
                        byte[] bufLength = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            bufLength[i] = ((Integer) mmInStream.read()).byteValue();
                        }
                        int length = ByteArrayToInt(bufLength);
                        buffer = new byte[length];
                        for (int i = 0; i < length; i++) {
                            buffer[i] = ((Integer) mmInStream.read()).byteValue();
                        }
                        Log.d(TAG,"接收到数据");
                        mHandler.obtainMessage(WifiState.MESSAGE_READ,
                                buffer).sendToTarget();

                    }*/

                } catch (IOException e) {
                    connectionLost();
                    //WifiService.this.start(WifiService.this.isAndroid);
                    break;
                } catch (Exception e) {
                    connectionLost();
                    //WifiService.this.start(WifiService.this.isAndroid);
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] buffer) {
            /*try {
                mmOutStream.write(buffer);
//                        Log.d(TAG,"传递成功");
            } catch (IOException e) {
            }*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mmOutStream.write(buffer);
//                        Log.d(TAG,"传递成功");
                    } catch (IOException e) {
                    }
                }
            }).start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}