package com.myapp.socket;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class CameraSocket {

    public static final String TAG = "CameraSocket";
    private static int headInfoLength = 14;

    //private String mAddress;
    private OutputStream outputStream;
    private Socket socket;
    public static final int PORT = 10000;
    public String mAddress;
    public InetSocketAddress inetSocketAddress;
    public void init(String address){
        mAddress = address;
        // 创建Socket对象
        socket = new Socket();
        inetSocketAddress = new InetSocketAddress(mAddress, PORT);
        // 连接对方
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.connect(inetSocketAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void send(byte[] bytes){
        try {
            // 获得socket对象的输出流
            outputStream = socket.getOutputStream();
            //创建一个对象输出流
            //ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            //将transBean写入其中
            //TransBean transBean = new TransBean(Constant.CAMERA,"",new FileBean());
            //objectOutputStream.writeObject(transBean);


            outputStream.write(bytes);
            outputStream.flush();
            Log.e(TAG, "liu发送成功");
//            objectOutputStream.close();
//            outputStream.close();
//            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
