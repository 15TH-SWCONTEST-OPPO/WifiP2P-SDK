package com.myapp.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.myapp.Constant;
import com.myapp.WifiState;
import com.myapp.activity.netty.NettyClient;
import com.myapp.activity.netty.NettyServer;

public class NettyUtils {

    private NettyServer nettyServer;
    private NettyClient nettyClient;

    // 事件监听器
    private OnNettyDataReceivedListener mDataReceivedListener = null;
    // Netty连接事件监听器
    private NettyConnectionListener mNettyConnectionListener = null;


    public void setUpService() {
        nettyServer = new NettyServer(Constant.NETTYPORT, mHandler);
    }

    public void createClient() {
        nettyClient = new NettyClient(clientHandler);
    }

    public void connect2Server(String host,String deviceName) throws InterruptedException {
        nettyClient.run(host, Constant.NETTYPORT,deviceName);
    }

    public void startService(Handler nettyHandler) {
        Message msg = new Message();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    nettyServer.run(nettyHandler);
//                    msg.what = 0;
//                    nettyHandler.sendMessage(msg);
                }catch (Exception e){
                    msg.what = 1;
                    nettyHandler.sendMessage(msg);
                }

            }
        }).start();
    }

    public void stopServer() {
        if(nettyServer!=null){
            nettyServer.stop();
        }
    }

    public void sendData(byte[] bytes, String type) {
        nettyClient.send(bytes, type);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sendCommand(String selectedName, byte[] bytes, String text) {
        nettyServer.push(selectedName, bytes, text);
    }

    // 自定义数据接收监听器
    public interface OnNettyDataReceivedListener {
        // 当接收到数据的时候
        public void onDataReceived(String name, byte[] readBuf, String str);
    }

    // Netty连接监听器
    public interface NettyConnectionListener {
        public void onDeviceConnected(String name);

        public void onDeviceDisconnected(String name);

        public void onDeviceConnectionFailed();
    }

    public void setOnDataReceivedListener(NettyUtils.OnNettyDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }

    public void setNettyConnectionListener(NettyUtils.NettyConnectionListener listener) {
        mNettyConnectionListener = listener;
    }

    public void releaseClient() {
        if (nettyClient != null) {
            nettyClient.close();
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle data;
            String name;
            switch (msg.what) {
                case NettyState.MESSAGE_WRITE:
                    break;
                case NettyState.DEVICE_CONNECTED:
                    data = msg.getData();
                    name = data.getString("name");
                    mNettyConnectionListener.onDeviceConnected(name);
                    break;
                case NettyState.DEVICE_DISCONNECTED:
                    data = msg.getData();
                    name = data.getString("name");
                    mNettyConnectionListener.onDeviceDisconnected(name);
                case NettyState.MESSAGE_READ:
                    String str = null;
                    int arg1 = msg.arg1;
                    if (arg1 == 0) {
                        str = "text";
                    } else if (arg1 == 1) {
                        str = "photo";
                    } else if (arg1 == 2) {
                        str = "video";
                    }else if(arg1 ==3){
                        str = "address";
                    }
                    // 将数据放到一个Byte数组中
                    byte[] readBuf = (byte[]) msg.obj;
                    data = msg.getData();
                    name = data.getString("name");

                    if (readBuf != null && readBuf.length > 0) {
                        if (mDataReceivedListener != null) {
                            // 为Listener设置值
                            Log.d("NettyUtils", str);
                            mDataReceivedListener.onDataReceived(name, readBuf, str);
                        }
                    }
                    break;

                case WifiState.MESSAGE_DEVICE_NAME:

                case WifiState.MESSAGE_TOAST:

                    break;
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler clientHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NettyState.MESSAGE_WRITE:
                    break;

                case NettyState.MESSAGE_READ:
                    String str = null;
                    int arg1 = msg.arg1;
                    if (arg1 == 0) {
                        str = "text";
                    } else if (arg1 == 1) {
                        str = "photo";
                    } else if (arg1 == 2) {
                        str = "video";
                    }else if(arg1==3){
                        str = "address";
                    }
                    // 将数据放到一个Byte数组中
                    byte[] readBuf = (byte[]) msg.obj;


                    if (readBuf != null && readBuf.length > 0) {
                        if (mDataReceivedListener != null) {
                            // 为Listener设置值
                            Log.d("NettyUtils", str);
                            mDataReceivedListener.onDataReceived("Server", readBuf, str);
                        }
                    }
                    break;

                case WifiState.MESSAGE_DEVICE_NAME:

                case WifiState.MESSAGE_TOAST:

                    break;
            }
        }
    };
}
