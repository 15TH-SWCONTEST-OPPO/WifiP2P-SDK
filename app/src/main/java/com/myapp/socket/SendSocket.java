package com.myapp.socket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.myapp.Constant;
import com.myapp.FileBean;
import com.myapp.TransBean;

/**
 * date：2018/2/24 on 18:10
 * description: 客户端发送的socket
 */

public class SendSocket {

    public static final String TAG = "SendSocket";

    private FileBean mFileBean;
    private String mAddress;
    private Integer mPort;
    private File mFile;
    private String mText;
    private String mType;
    private TransBean mTransBean;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 10:
                    int progress = (int) msg.obj;
                    if (mlistener != null) {
                        mlistener.onProgressChanged(mFile, progress);
                    }
                    break;
                case 20:
                    if (mlistener != null) {
                        mlistener.onFinished(mFile);
                    }
                    break;
                case 30:
                    if (mlistener != null) {
                        mlistener.onFaliure(mFile);
                    }
                    break;
            }
        }
    };

    public SendSocket(TransBean transBean, String address, String port, ProgressSendListener listener) {
        mAddress = address;
        mlistener = listener;
        mType = transBean.type;
        mTransBean = transBean;
        mPort = Integer.parseInt(port);
        if(transBean.type.equals(Constant.FILE)){
            mFileBean = transBean.fileBean;
        }
    }

    public void createSendSocket() {
        switch (mType){
            case Constant.FILE:
                sendFile();
                break;
        }
    }

    public void sendFile(){
        try {
            // 创建Socket对象
            Socket socket = new Socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(mAddress, mPort);
            // 连接对方
            socket.connect(inetSocketAddress);
            // 获得socket对象的输出流
            OutputStream outputStream = socket.getOutputStream();
            //创建一个对象输出流
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            //将fileBean写入其中
            objectOutputStream.writeObject(mTransBean);
            //得到需要传输的文件
            mFile = new File(mFileBean.filePath);
            // 创建文件输入流
            FileInputStream inputStream = new FileInputStream(mFile);

            long size = mFileBean.fileLength;
            long total = 0;

            byte bytes[] = new byte[1024];
            int len;
            //
            while ((len = inputStream.read(bytes)) != -1) {
                // 将数据写入流中
                outputStream.write(bytes, 0, len);
                total += len;
                int progress = (int) ((total * 100) / size);
                Log.e(TAG, "文件发送进度：" + progress);
                Message message = Message.obtain();
                message.what = 10;
                message.obj = progress;
                mHandler.sendMessage(message);
            }
            outputStream.close();
            objectOutputStream.close();
            inputStream.close();
            socket.close();
            mHandler.sendEmptyMessage(20);
            Log.e(TAG, "文件发送成功");
        } catch (Exception e) {
            mHandler.sendEmptyMessage(30);
            Log.e(TAG, "文件发送异常");
        }
    }
    /**
     * 监听发送进度
     */
    private ProgressSendListener mlistener;

    public interface ProgressSendListener {

        //当传输进度发生变化时
        void onProgressChanged(File file, int progress);

        //当传输结束时
        void onFinished(File file);

        //传输失败时
        void onFaliure(File file);
    }

    private void sendText() {
        try {
            // 创建Socket对象
            Socket socket = new Socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(mAddress, Constant.sendPort);
            // 连接对方
            socket.connect(inetSocketAddress);
            // 获得socket对象的输出流
            OutputStream outputStream = socket.getOutputStream();
            //创建一个对象输出流
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            //将fileBean写入其中
            objectOutputStream.writeObject(mTransBean);

            outputStream.close();
            objectOutputStream.close();
            socket.close();
            mHandler.sendEmptyMessage(20);
            Log.e(TAG, "文本发送成功");
        } catch (Exception e) {
            mHandler.sendEmptyMessage(30);
            Log.e(TAG, "文本发送异常");
        }
    }
}

