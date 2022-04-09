package com.myapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.myapp.Constant;
import com.myapp.FileBean;
import com.myapp.R;
import com.myapp.utils.FileUtils;
import com.myapp.utils.Md5Util;
import com.myapp.utils.NettyUtils;

/**
 * 发送文件界面
 * <p>
 * 1、搜索设备信息
 * 2、选择设备连接服务端组群信息
 * 3、选择要传输的文件路径
 * 4、把该文件通过socket发送到服务端
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SendCameraActivity extends BaseActivity implements View.OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "SendFileActivity";

    private ListView mTvDevice;
    private ArrayList<String> mListDeviceName = new ArrayList();
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private AlertDialog mDialog;

    private SurfaceHolder mSurfaceHolder;

    private EditText mInput;

    //记录是否正在传视频
    private boolean mark = false;
    private int count;

    private int connectTime = 0;
    private Integer sendType = 0;

    private Boolean isConnected = false;
    private MediaRecorder mediaRecorder;

    //camera的相关字段
    private boolean isOpened = false;
    private ImageReader imageReader;
    private Handler mainHandler;
    private Handler childHandler;
    private CameraDevice mCameraDevice;

    //动态设置图片的质量
    private int quality = 10;

    private CameraCaptureSession mCameraCaptureSession;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private SurfaceView mPreview;

    private Surface mRecorderSurface;
    private boolean isPicture = false;

    private boolean isRecording = false;

    private NettyUtils nettyUtils;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_camera);

        Button mBtnSearchServer = (Button) findViewById(R.id.btn_searchserver);
        Button mBtnSendText = (Button) findViewById(R.id.btn_sendtext);
        Button mBtnSendPhoto = (Button) findViewById(R.id.btn_sendphoto);
        Button mBtnSendCamera = (Button) findViewById(R.id.btn_sendcamera);
        Button mBtnCancelConnect = (Button) findViewById(R.id.btn_cancelconnect);
        Button mBtnStartRecord = (Button) findViewById(R.id.start_record);
        Button mBtnStopRecord = (Button) findViewById(R.id.stop_record);
        Button mBtnStartNetty = findViewById(R.id.start_netty);
        mTvDevice = (ListView) findViewById(R.id.lv_device);
        mInput = findViewById(R.id.edit_text);

        mBtnSendPhoto.setOnClickListener(this);
        mBtnSearchServer.setOnClickListener(this);
        mBtnSendText.setOnClickListener(this);
        mBtnSendCamera.setOnClickListener(this);
        mBtnCancelConnect.setOnClickListener(this);
        mBtnStartRecord.setOnClickListener(this);
        mBtnStopRecord.setOnClickListener(this);
        mBtnStartNetty.setOnClickListener(this);
        nettyUtils = new NettyUtils();
        nettyUtils.createClient();

        initWifi();

        mPreview = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mPreview.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

        //获取到可持久化的surface用于视频的录制
        mRecorderSurface = MediaCodec.createPersistentInputSurface();
    }

    //设置开始录制和结束录制的方法
    void startRecord() {
        if (isRecording) {
            return;
        }
        isRecording = true;
        mediaRecorder.start();
        Toast.makeText(SendCameraActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void stopRecord() {
        if (!isRecording) {
            return;
        }
        releaseMediaRecorder();

        setUpMediaRecorder();
        isRecording = false;
        Toast.makeText(SendCameraActivity.this, "停止录制", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_searchserver:
                sendType = 0;
                cancelConnect(false);
                mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
                mDialog.show();
                mDialog.setCancelable(false);
                mDialog.setContentView(R.layout.loading_progressba);
                //搜索设备
                connectServer();
                break;
            case R.id.btn_sendphoto:
                if (mWifiP2pInfo == null) {
                    Toast.makeText(this, "当前未连接", Toast.LENGTH_SHORT).show();
                }
                sendPhoto();
                break;
            case R.id.btn_sendtext:
                if (mWifiP2pInfo == null) {
                    Toast.makeText(this, "当前未连接", Toast.LENGTH_SHORT).show();
                }
                sendText();
                break;
            case R.id.btn_sendcamera:
                if (mWifiP2pInfo == null) {
                    Toast.makeText(this, "当前未连接", Toast.LENGTH_SHORT).show();
                }
                mark = true;
                break;

            case R.id.btn_cancelconnect:
                cancelConnect(true);
                break;

            case R.id.start_record:
                startRecord();
                break;

            case R.id.stop_record:
                stopRecord();
                break;
            case R.id.start_netty:
                startNetty();
                break;
            default:
                break;
        }
    }

    private void startNetty() {
        try {
            if(mWifiP2pInfo!=null){
                nettyUtils.connect2Server(mWifiP2pInfo.groupOwnerAddress.getHostAddress());
                isConnected = true;
            }else{
                isConnected = false;
                System.out.println("连接为空");
            }

        } catch (Exception e) {
            isConnected = false;
            System.err.println(e);
        }
    }

    private void sendPhoto() {
        takePicture();
    }

    private void initWifi() {
        nettyUtils.setOnDataReceivedListener(new NettyUtils.OnNettyDataReceivedListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataReceived(String name, byte[] data, String message) {
                if (message.equals("text") && data.length != 0) {
                    String text = new String(data);
                    Toast.makeText(SendCameraActivity.this, text, Toast.LENGTH_SHORT).show();
                    if (text.equals(Constant.SENDIMAGE)){
                        sendPhoto();
                    } else if (text.equals(Constant.STARTRECORD)){
                        startRecord();
                    }else if(text.equals(Constant.STOPRECORD)){
                        stopRecord();
                    }else if(text.equals(Constant.SENDCAMERA)){
                        mark = true;
                    }else if(text.equals(Constant.DISCONNECT)){
                        cancelConnect(false);
                    }

                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {

                if (mDialog == null) {
                    mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                }
                mDialog.dismiss();

                Toast.makeText(SendCameraActivity.this, "连接出错", Toast.LENGTH_SHORT).show();
                isConnected = false;
            }
            if (msg.what == 2) {

                if (mDialog == null) {
                    mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                }

                mDialog.dismiss();
                Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                isConnected = true;
            }
        }
    };


    private void cancelConnect(boolean isShow) {
        mark = false;
        isConnected = false;
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if(isShow){
                    Toast.makeText(SendCameraActivity.this, "取消成功", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "取消成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                //Log.e(TAG, "取消失败");
            }
        });
    }

    //发送文本信息
    public void sendText() {
        nettyUtils.sendData("你好".getBytes(),"text");
//        if (!isConnected) {
//            Toast.makeText(this, "请连接设备", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        // 从EditText中得到数据
//        String input = mInput.getText().toString().trim();
//        if (input != null && !input.isEmpty()) {
//            // 发送数据
//            wifiUtils.send(input.getBytes(), "text");
//        } else {
//            // 校验数据格式
//            Toast.makeText(this, "输入信息不能为空", Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     * 搜索设备
     */
    @SuppressLint("MissingPermission")
    private void connectServer() {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "搜索设备成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "搜索设备失败");
            }
        });
    }

    /**
     * 连接设备
     */
    @SuppressLint("MissingPermission")
    private void connect(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig config = new WifiP2pConfig();
        if (wifiP2pDevice != null) {
            config.deviceAddress = wifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    SystemClock.sleep(1000);

                    if (mWifiP2pInfo == null || mWifiP2pInfo.groupOwnerAddress == null) {
                        if (connectTime >= 2) {
                            connectTime = 0;
                            mDialog.dismiss();
                            Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        } else {
                            connectTime++;
                            connect(wifiP2pDevice);
                        }
                    } else {
                        Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    isConnected = false;
                    Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });


        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());

        if (isOpened) {
            return;
        }

        isOpened = true;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];//这个可能会有很多个，但是通常都是两个，第一个是后置，第二个是前置；
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Log.i(TAG, "onOpened");
                    mCameraDevice = camera;
                    createCameraPreview(camera);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Log.i(TAG, "onDisconnected");
                    camera.close();
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.i(TAG, "onError -> " + error);
                    camera.close();
                }
            }, mainHandler);//这个指定其后台运行，如果直接UI线程也可以，直接填null；
            Log.i(TAG, "open Camera " + cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void createCameraPreview(final CameraDevice cameraDevice) {
        try {
            if (null == cameraDevice) {
                Log.i(TAG, "updatePreview error, return");
                return;
            }
            setUpImageReader();
            setUpMediaRecorder();
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            Surface imageSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(mSurfaceHolder.getSurface());
            captureRequestBuilder.addTarget(mRecorderSurface);
            captureRequestBuilder.addTarget(imageSurface);
            //captureRequestBuilder.addTarget(mShowHolder.getSurface());
            List<Surface> surfaceList = Arrays.asList(mSurfaceHolder.getSurface(), /*mShowHolder.getSurface(),*/ imageSurface, mRecorderSurface);
            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                //配置要接受图像的surface
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, childHandler);//成功配置后，便开始进行相机图像的监听
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(SendCameraActivity.this, "配置出错", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUpMediaRecorder() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setProfile(getCamcorderProfile());
        //mediaRecorder.setInputSurface(mSurfaceHolder.getSurface());
        //mediaRecorder.setOutputFile(new File(getExternalCacheDir(), System.currentTimeMillis() + ".mp4").getAbsolutePath());
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO));
        //mediaRecorder.setPreviewDisplay(mShowHolder.getSurface());
        mediaRecorder.setInputSurface(mRecorderSurface);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (mCameraDevice == null) return;
        // 创建拍照需要的CaptureRequest.Builder
        //mark = false;
        isPicture = true;
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CamcorderProfile getCamcorderProfile() {
        return CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    }

    private void setUpImageReader() {
        imageReader = ImageReader.newInstance(800, 600, ImageFormat.JPEG, 10);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();

                if (image != null && isPicture) {
                    System.out.println("发送图片");
                    isPicture = false;
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    //wifiUtils.send(compressImage(bitmapImage, quality), "photo");

                    bitmapImage.recycle();
                    bitmapImage = null;
                    image.close();
                    return;
                }

                if (image != null && mark) {
                    //System.out.println("有图像");
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                    //wifiUtils.send(compressImage(bitmapImage, quality), "video");
                    nettyUtils.sendData(compressImage(bitmapImage, quality), "video");
                }
                if (image != null) {
                    image.close();
                }
                //Log.i(TAG, "onImageAvailable");
            }
        }, childHandler);
    }

    public static byte[] GetByteImage(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return bytes;
        //Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    public byte[] compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        //int options = 100;
        /*while (outputStream.toByteArray().length / 1024 > imageSize) {
            outputStream.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, outputStream);
            options -= 10;
        }*/
        return outputStream.toByteArray();
//        ByteArrayInputStream isBm = new ByteArrayInputStream(outputStream.toByteArray());
//        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
//        return bitmap;
    }


    /**
     * 客户端进行选择文件
     */
    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 10);
    }

    /**
     * 客户端选择文件回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtils.getAbsolutePath(this, uri);
                    if (path != null) {
                        final File file = new File(path);
                        if (!file.exists()) {
                            Toast.makeText(SendCameraActivity.this, "文件路径找不到", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mWifiP2pInfo == null) {
                            Toast.makeText(SendCameraActivity.this, "连接不存在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String md5 = Md5Util.getMd5(file);
                        FileBean fileBean = new FileBean(file.getPath(), file.length(), md5);
                        String hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                        new SendTask(SendCameraActivity.this, fileBean).execute(hostAddress);
                    }
                }
            }
        }
    }

    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);

        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (!mListDeviceName.contains(device.deviceName) && !mListDevice.contains(device)) {
                mListDeviceName.add("设备：" + device.deviceName + "----" + device.deviceAddress);
                mListDevice.add(device);
            }
        }

        //进度条消失
        if (mDialog == null) {
            mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
        }
        mDialog.dismiss();
        showDeviceInfo();
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    /**
     * 展示设备信息
     */
    private void showDeviceInfo() {
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mListDeviceName);
        mTvDevice.setAdapter(adapter);
        mTvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiP2pDevice wifiP2pDevice = mListDevice.get(i);
                mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                mDialog.show();
                mDialog.setCancelable(false);
                mDialog.setContentView(R.layout.loading_connect);
                connect(wifiP2pDevice);
            }
        });
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
