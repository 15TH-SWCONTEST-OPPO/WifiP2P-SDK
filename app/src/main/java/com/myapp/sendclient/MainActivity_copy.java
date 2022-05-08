package com.myapp.sendclient;//package com.rejointech.sendclient;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.bluetooth.BluetoothAdapter;
//import android.content.ComponentName;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.graphics.ImageFormat;
//import android.graphics.PixelFormat;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
//import android.hardware.Camera;
//import android.media.Image;
//import android.media.ImageReader;
//import android.media.projection.MediaProjectionManager;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.HandlerThread;
//import android.os.IBinder;
//import android.support.v7.app.AppCompatActivity;
//import android.view.Surface;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.io.ByteArrayOutputStream;
//
//public class MainActivity_copy extends AppCompatActivity implements SurfaceHolder.Callback {
//    private static final String TAG = MainActivity_copy.class.getName();
//    private static final int REQUEST_BLUETOOTH_ENABLE = 100;
//    private BluetoothUtil mBt;
//    private Camera mCamera;
//    private SurfaceHolder mSurfaceHolder;
//    private int mWidth;
//    private int mHeight;
//    private EditText mInput;
//    private boolean isBluetoothConnnect;
//    public Camera.Size size;
//    private boolean mark = true;
//    private int count;
//    private MediaProjectionManager mediaProjectionManager;
//    private long recordDpi = 6000000;
//    private TextView tvTime;
//    private Surface surface;
//    private MediaRecordService.MyBinder binder;
//    private Handler childHandler;
//
//    private ServiceConnection connection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            if (service instanceof MediaRecordService.MyBinder) {
//                binder = (MediaRecordService.MyBinder) service;
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//
//        }
//    };
//    private ImageReader imageReader;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        // 创建蓝牙工具类
//        mBt = new BluetoothUtil(this);
//        mInput = (EditText) findViewById(R.id.input);
//        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
//
//        mSurfaceHolder = surfaceView.getHolder();
//        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        mSurfaceHolder.addCallback(this);
//        initBlue();
//        HandlerThread handlerThread = new HandlerThread("Recorder");
//        handlerThread.start();
//        childHandler = new Handler(handlerThread.getLooper());
//        setUpImageReader();
//    }
//
//    @SuppressLint("WrongConstant")
//    private void setUpImageReader() {
//        imageReader = ImageReader.newInstance(WindowUtils.getWindowWidth(this), WindowUtils.getWindowHeight(this), PixelFormat.RGBA_8888, 10);
//        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
//
//            @Override
//            public void onImageAvailable(ImageReader reader) {
//                Image image = null;
//                try {
//                    image = reader.acquireLatestImage();
//                    if(image!=null){
//                    }
//                } catch (Exception e) {
//
//                }
//            }
//        }, childHandler);
//    }
//
//    private void initBlue() {
//        /**
//         * reveice data
//         */
//        mBt.setOnDataReceivedListener(new BluetoothUtil.OnDataReceivedListener() {
//            public void onDataReceived(byte[] data, String message) {
//
//            }
//        });
//
//        mBt.setBluetoothConnectionListener(new BluetoothUtil.BluetoothConnectionListener() {
//            public void onDeviceConnected(String name, String address) {
//                isBluetoothConnnect = true;
//                Toast.makeText(getApplicationContext(), "连接到 " + name + "\n" + address, Toast.LENGTH_SHORT).show();
//            }
//
//            public void onDeviceDisconnected() {
//                isBluetoothConnnect = false;
//                //断开蓝牙连接
//                Toast.makeText(getApplicationContext(), "蓝牙断开", Toast.LENGTH_SHORT).show();
//
//            }
//
//            public void onDeviceConnectionFailed() {
//                Toast.makeText(getApplicationContext(), "无法连接", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    // 此方法为从蓝牙线程返回获取数据的方法
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // 与设备进行连接
//        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
//            if (resultCode == Activity.RESULT_OK)
//                mBt.connect(data);
//        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
//            if (resultCode == Activity.RESULT_OK) {
//                // 开启Service
//                mBt.setupService();
//                mBt.startService(BluetoothState.DEVICE_ANDROID);
//            } else {
//                finish();
//            }
//        }
//    }
//
//    public void onStart() {
//        super.onStart();
//        if (!mBt.isBluetoothEnabled()) {
//            //打开蓝牙
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
//        } else {
//            if (!mBt.isServiceAvailable()) {
//                //开启监听
//                mBt.setupService();
//                mBt.startService(BluetoothState.DEVICE_ANDROID);
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mBt.stopService();
//        releaseCamera();
//    }
//
//    private void releaseCamera() {
//        if (mCamera != null) {
//            mCamera.setPreviewCallback(null);
//            mCamera.setPreviewCallbackWithBuffer(null);
//            mCamera.stopPreview();// 停掉原来摄像头的预览
//            mCamera.release();
//            mCamera = null;
//        }
//    }
//
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//
////        try {
////            mCamera = Camera.open();
////            Camera.Parameters mPara = mCamera.getParameters();
////            List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
////            List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
////
////            int previewSizeIndex = -1;
////            Camera.Size psize;
////            int height_sm = 999999;
////            int width_sm = 999999;
////            //获取设备最小分辨率图片，图片越清晰，传输越卡
////            for (int i = 0; i < previewSizes.size(); i++) {
////                psize = previewSizes.get(i);
////                if (psize.height <= height_sm && psize.width <= width_sm) {
////                    previewSizeIndex = i;
////                    height_sm = psize.height;
////                    width_sm = psize.width;
////                }
////            }
////
////            if (previewSizeIndex != -1) {
////                mWidth = previewSizes.get(previewSizeIndex).width;
////                mHeight = previewSizes.get(previewSizeIndex).height;
////                mPara.setPreviewSize(mWidth, mHeight);
////            }
////            mCamera.setParameters(mPara);
////            mCamera.setPreviewDisplay(mSurfaceHolder);
////            mCamera.startPreview();
////
////            size = mCamera.getParameters().getPreviewSize();
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//
//    }
//
//    //蓝牙搜索配对
//    public void bluetooth(View view) {
//        if (mBt.getServiceState() == BluetoothState.STATE_CONNECTED) {
//            mBt.disconnect();
//        } else {
//            Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
//            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
//        }
//    }
//
//    //发送文本信息
//    public void sendText(View view) {
//        if (!isBluetoothConnnect) {
//            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        // 从EditText中得到数据
//        String input = mInput.getText().toString().trim();
//        if (input != null && !input.isEmpty()) {
//            // 发送数据
//            mBt.send(input.getBytes(), "TEXT");
//        } else {
//            // 校验数据格式
//            Toast.makeText(MainActivity_copy.this, "输入信息不能为空", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    //发送图片
//    public void sendPhoto(View view) {
//        if (!isBluetoothConnnect) {
//            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        mark = false;//关闭视频发送
//        mCamera.takePicture(null, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] bytes, Camera camera) {
//                mBt.send(bytes, "photo");
//                mCamera.startPreview();
//            }
//        });
//    }
//
//    //发送视频 其实也是发送一张一张的图片
//    public void sendVideo(View view) {
//        if (!isBluetoothConnnect) {
//            Toast.makeText(this, "请连接蓝牙", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        mark = true;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
//                    @Override
//                    public void onPreviewFrame(byte[] data, Camera camera) {
//                        count++;
//                        Camera.Size size = camera.getParameters().getPreviewSize();
//                        final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                        image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, stream);
//                        byte[] imageBytes = stream.toByteArray();
//                        mBt.send(imageBytes, "video");
//                        if (count % 2 == 0 && mark) {
//                            mBt.send(imageBytes, "video");
//                        }
//                    }
//                });
//            }
//        }).start();
//    }
//}
