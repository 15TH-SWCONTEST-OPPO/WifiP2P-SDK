package wifip2p.wifi.com.wifip2p.socket;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
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

import wifip2p.wifi.com.wifip2p.Constant;
import wifip2p.wifi.com.wifip2p.FileBean;
import wifip2p.wifi.com.wifip2p.R;
import wifip2p.wifi.com.wifip2p.TransBean;

public class CameraReceiveSocket extends Activity {
    public static final String TAG = "ReceiveSocket";
    public static final int PORT = 10000;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private InputStream mInputStream;
    private ObjectInputStream mObjectInputStream;
    private FileOutputStream mFileOutputStream;
    private File mFile;
    private Context context;
    ImageView imageView;
    public void createServerSocket(Context context) {
        imageView = findViewById(R.id.camera_video);
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
            while (true){
                readVideo();
            }

        } catch (Exception e) {

            Log.e(TAG, "Socket接收异常");
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
            //imageView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
            Log.d(TAG,"第一次接收");
        }catch (Exception e){
            Log.e(TAG,"出现异常");
        }
    }

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
