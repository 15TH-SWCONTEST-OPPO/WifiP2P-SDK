package com.myapp.socket;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.myapp.Constant;
import com.myapp.FileBean;
import com.myapp.R;
import com.myapp.TransBean;
import com.myapp.utils.FileUtils;
import com.myapp.utils.Md5Util;

/**
 * date：2018/2/24 on 16:59
 * description:服务端监听的socket
 */

public class ReceiveSocket extends Activity {

    public static final String TAG = "ReceiveSocket";
    public static final int PORT = 10000;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private InputStream mInputStream;
    private ObjectInputStream mObjectInputStream;
    private FileOutputStream mFileOutputStream;
    private File mFile;
    private Context context;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 40:
                    if (mListener != null) {
                        mListener.onSatrt();
                    }
                    break;
                case 50:
                    int progress = (int) msg.obj;
                    if (mListener != null) {
                        mListener.onProgressChanged(mFile, progress);
                    }
                    break;
                case 60:
                    if (mListener != null) {
                        mListener.onFinished(mFile);
                    }
                    break;
                case 70:
                    if (mListener != null) {
                        mListener.onFaliure(mFile);
                    }
                    break;
            }
        }
    };



    public void createServerSocket(Context context) {

        try {
            //创建socket对象
            mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            //绑定端口
            mServerSocket.bind(new InetSocketAddress(PORT));
            //开始接收
            mSocket = mServerSocket.accept();
            Log.e(TAG, "客户端IP地址 : " + mSocket.getRemoteSocketAddress());
            //获得流对象
            mInputStream = mSocket.getInputStream();
            //根据传入的流创建对象输入流
            mObjectInputStream = new ObjectInputStream(mInputStream);
            //获得对应的TransBean
            TransBean transBean = (TransBean) mObjectInputStream.readObject();

            if (transBean.type.equals(Constant.FILE)) {
                receiveFile((FileBean) transBean.fileBean);
            }else if(transBean.type.equals(Constant.TEXT)){
                Toast.makeText(context,transBean.text,Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            mHandler.sendEmptyMessage(70);
            Log.e(TAG, "文件接收异常");
        }
    }

    private void readVideo() {
        try{
            byte bytes[] = new byte[1024];
            int len;
            long total = 0;
            int progress;
            while ((len = mInputStream.read(bytes)) != -1) {
                mFileOutputStream.write(bytes, 0, len);
                total += len;
            }
            ImageView viewById = (ImageView) findViewById(R.id.camera_video);
            viewById.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        }catch (Exception e){
            Log.e(TAG,"出现异常");
        }
    }

    private void receiveFile(FileBean fileBean) {
        String name = new File(fileBean.filePath).getName();
        Log.e(TAG, "客户端传递的文件名称 : " + name);
        Log.e(TAG, "客户端传递的MD5 : " + fileBean.md5);
        try {
            mFile = new File(FileUtils.SdCardPath(name));
            //获得文件输出流
            mFileOutputStream = new FileOutputStream(mFile);
            //开始接收文件,向Handler对象传递值
            mHandler.sendEmptyMessage(40);
            byte bytes[] = new byte[1024];
            int len;
            long total = 0;
            int progress;
            while ((len = mInputStream.read(bytes)) != -1) {
                mFileOutputStream.write(bytes, 0, len);
                total += len;
                progress = (int) ((total * 100) / fileBean.fileLength);
                Log.e(TAG, "文件接收进度: " + progress);
                Message message = Message.obtain();
                message.what = 50;
                message.obj = progress;
                mHandler.sendMessage(message);
            }
            //新写入文件的MD5
            String md5New = Md5Util.getMd5(mFile);
            //发送过来的MD5
            String md5Old = fileBean.md5;
            if (md5New != null || md5Old != null) {
                if (md5New.equals(md5Old)) {
                    mHandler.sendEmptyMessage(60);
                    Log.e(TAG, "文件接收成功");
                }
            } else {
                mHandler.sendEmptyMessage(70);
            }

            mServerSocket.close();
            mInputStream.close();
            mObjectInputStream.close();
            mFileOutputStream.close();
        }catch (Exception e){
            mHandler.sendEmptyMessage(70);
            Log.e(TAG, "文件接收异常");
        }

    }

    /**
     * 监听接收进度
     */
    private ProgressReceiveListener mListener;

    public void setOnProgressReceiveListener(ProgressReceiveListener listener) {
        mListener = listener;
    }

    public interface ProgressReceiveListener {

        //开始传输
        void onSatrt();

        //当传输进度发生变化时
        void onProgressChanged(File file, int progress);

        //当传输结束时
        void onFinished(File file);

        //传输失败回调
        void onFaliure(File file);
    }

    /**
     * 服务断开：释放内存
     */
    public void clean() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mObjectInputStream != null) {
            try {
                mObjectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
