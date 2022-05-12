package com.myapp.sendclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.myapp.R;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_BLUETOOTH_ENABLE = 100;
    private BluetoothUtil mBt;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private int mWidth;
    private int mHeight;
//    private EditText mInput;
    private boolean isBluetoothConnnect;
    public Camera.Size size;
    private boolean mark = true;
    private int count;
    private MediaProjectionManager mediaProjectionManager;
    private long recordDpi = 6000000;
    private TextView tvTime;
    private Surface surface;
    private MediaRecordService.MyBinder binder;
    private Handler childHandler;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof MediaRecordService.MyBinder) {
                binder = (MediaRecordService.MyBinder) service;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private ImageReader imageReader;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_send);
        // 创建蓝牙工具类
        mBt = new BluetoothUtil(this);
//        mInput = (EditText) findViewById(R.id.input);
        imageView = (ImageView) findViewById(R.id.imageview);
        initBlue();
        /*
         * relative 布局
         * */
        RelativeLayout relativeLayout=(RelativeLayout)findViewById(R.id.relative);


        /*
         * icon图标
         * */
        // 加载字体文件
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");
        // client
        TextView bluetooth = (TextView) findViewById(R.id.blue_blueTooth);
        bluetooth.setTypeface(iconfont);
        // sendPhoto
        TextView sendPhoto = (TextView) findViewById(R.id.sendphoto);
        sendPhoto.setTypeface(iconfont);
        // video
        TextView sendvideo = (TextView) findViewById(R.id.sendvideo);
        sendvideo.setTypeface(iconfont);
        // cancleLink
        TextView cancleLink = (TextView) findViewById(R.id.cancleLink);
        cancleLink.setTypeface(iconfont);
        // closeScreen
        TextView closescreen = (TextView) findViewById(R.id.closescreen);
        closescreen.setTypeface(iconfont);

        HandlerThread handlerThread = new HandlerThread("Recorder");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        setUpImageReader();
    }

    /*
     * 隐藏navigateBar
     * */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        View decorView = getWindow().getDecorView();
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @SuppressLint("WrongConstant")
    private void setUpImageReader() {
        int width = WindowUtils.getWindowWidth(this);
        int height = WindowUtils.getWindowHeight(this);
        //int maxx = Math.max(width,height);
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int width = image.getWidth();
                        int height = image.getHeight();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowpadding = rowStride - pixelStride * width;
                        Bitmap bitmap = Bitmap.createBitmap(width + rowpadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
                        Log.e("bitmap", bitmap.getWidth() + ";" + bitmap.getHeight());

                        if (bitmap != null) {
                            // TODO 处理图片
                            imageView.setImageBitmap(bitmap);
                            mBt.send(compressImage(bitmap, 20), "video");
                            //bitmap.recycle();
                        }
                        image.close();
                    }
                } catch (Exception e) {

                }
            }
        }, childHandler);
    }

    public byte[] compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.out.println("压缩前" + (double) image.getByteCount() / 1024.0 / 1024.0);
        image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        byte[] bytes = outputStream.toByteArray();
        System.out.println("压缩后" + (double) bytes.length / 1024.0 / 1024.0);
        return bytes;
    }

    private void initBlue() {
        /**
         * reveice data
         */
        mBt.setOnDataReceivedListener(new BluetoothUtil.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {

            }
        });

        mBt.setBluetoothConnectionListener(new BluetoothUtil.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                isBluetoothConnnect = true;
                Toast.makeText(getApplicationContext(), "连接到 " + name + "\n" + address, Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                isBluetoothConnnect = false;
                //断开蓝牙连接
                Toast.makeText(getApplicationContext(), "蓝牙断开", Toast.LENGTH_SHORT).show();

            }

            public void onDeviceConnectionFailed() {
                Toast.makeText(getApplicationContext(), "无法连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 此方法为从蓝牙线程返回获取数据的方法
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 与设备进行连接
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                mBt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // 开启Service
                mBt.setupService();
                mBt.startService(BluetoothState.DEVICE_ANDROID);
            } else {
                finish();
            }
        }
        if (requestCode == 10012) {
            Intent intent = new Intent(this, MediaRecordService.class);
            intent.putExtra("data", data);
            intent.putExtra("resultCode", resultCode);
            intent.putExtra("width", WindowUtils.getWindowWidth(this));
            intent.putExtra("height", WindowUtils.getWindowHeight(this));
            intent.putExtra("surface", imageReader.getSurface());
            startForegroundService(intent);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }
    }

    public void onStart() {
        super.onStart();
        if (!mBt.isBluetoothEnabled()) {
            //打开蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!mBt.isServiceAvailable()) {
                //开启监听
                mBt.setupService();
                mBt.startService(BluetoothState.DEVICE_ANDROID);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mBt.stopService();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }


    //蓝牙搜索配对
    public void bluetooth(View view) {
        if (mBt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            mBt.disconnect();
        } else {
            Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    //发送文本信息
    public void sendText(View view) {
        if (!isBluetoothConnnect) {
            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        // 从EditText中得到数据
//        String input = mInput.getText().toString().trim();
//        if (input != null && !input.isEmpty()) {
//            // 发送数据
//            mBt.send(input.getBytes(), "TEXT");
//        } else {
//            // 校验数据格式
//            Toast.makeText(MainActivity.this, "输入信息不能为空", Toast.LENGTH_SHORT).show();
//        }
    }

    //发送图片
    public void sendPhoto(View view) {
        if (!isBluetoothConnnect) {
            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        mark = false;//关闭视频发送
    }

    public void sendVideo(View view) {
        if (!isBluetoothConnnect) {
            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        mark = true;
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(screenCaptureIntent, 10012);
    }

    public void disconnect(View view) {
        mBt.disconnect();
        mBt.stopService();
    }

    public void closeScreen(View view) {
        if (binder != null) {
            binder.stop();
        }
    }
}
