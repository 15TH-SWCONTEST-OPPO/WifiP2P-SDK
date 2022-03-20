package wifip2p.wifi.com.wifip2p.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import wifip2p.wifi.com.wifip2p.Constant;
import wifip2p.wifi.com.wifip2p.FileBean;
import wifip2p.wifi.com.wifip2p.R;
import wifip2p.wifi.com.wifip2p.TransBean;
import wifip2p.wifi.com.wifip2p.WifiState;
import wifip2p.wifi.com.wifip2p.socket.CameraSocket;
import wifip2p.wifi.com.wifip2p.socket.SendSocket;
import wifip2p.wifi.com.wifip2p.utils.FileUtils;
import wifip2p.wifi.com.wifip2p.utils.Md5Util;
import wifip2p.wifi.com.wifip2p.utils.WifiException;
import wifip2p.wifi.com.wifip2p.utils.WifiUtils;

/**
 * 发送文件界面
 * <p>
 * 1、搜索设备信息
 * 2、选择设备连接服务端组群信息
 * 3、选择要传输的文件路径
 * 4、把该文件通过socket发送到服务端
 */
public class SendCameraActivity extends BaseActivity implements View.OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "SendFileActivity";

    private ListView mTvDevice;
    private ArrayList<String> mListDeviceName = new ArrayList();
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private AlertDialog mDialog;
    private String type;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private int mWidth;
    private int mHeight;
    private EditText mInput;
    private boolean isBluetoothConnnect;
    public Camera.Size size;
    private boolean mark = true;
    private int count;
    private WifiUtils wifiUtils;

    private Integer sendType = 0;

    private Boolean isConnected = false;
    private MediaRecorder mediaRecorder;

    private boolean isRecording = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private Button captureButton;

    private SurfaceView mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_camera);
        Button mBtnChoseFile = (Button) findViewById(R.id.btn_chosefile);
        Button mBtnConnectServer = (Button) findViewById(R.id.btn_connectserver);
        Button mBtnSendText = (Button) findViewById(R.id.btn_sendtext);
        Button mBtnSendPhoto = (Button) findViewById(R.id.btn_sendphoto);
        Button mBtnSendCamera = (Button) findViewById(R.id.btn_sendcamera);
        Button mBtnCancelConnect = (Button) findViewById(R.id.btn_cancelconnect);
        mTvDevice = (ListView) findViewById(R.id.lv_device);
        mInput = findViewById(R.id.edit_text);

        mBtnSendPhoto.setOnClickListener(this);
        mBtnChoseFile.setOnClickListener(this);
        mBtnConnectServer.setOnClickListener(this);
        mBtnSendText.setOnClickListener(this);
        mBtnSendCamera.setOnClickListener(this);
        mBtnCancelConnect.setOnClickListener(this);

        // 创建wifi工具类
        wifiUtils = new WifiUtils(this);
        wifiUtils.setupService();
        //mInput = (EditText) findViewById(R.id.input);
        mPreview = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mPreview.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        //视频录制


        captureButton = (Button) findViewById(R.id.btn_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isRecording) {
                            startRecord();
                        } else {
                            stopRecord();
                        }
                    }
                }
        );
    }

    void startRecord() {
        if (mediaRecorder == null) {
            prepareVideoRecorder();
        }
        mediaRecorder.start();
        captureButton.setText("Stop");
        isRecording = true;
    }

    void stopRecord() {
        mediaRecorder.stop();
        releaseMediaRecorder();
        mCamera.lock();

        captureButton.setText("Capture");
        isRecording = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_chosefile:
                //chooseFile();
                wifiUtils.disconnect();
                break;
            case R.id.btn_connectserver:
                sendType = 0;
                wifiUtils.disconnect();
                cancelConnect();
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
                sendType = 2;
                sendPhoto();

                break;
            case R.id.btn_sendtext:
                if (mWifiP2pInfo == null) {
                    Toast.makeText(this, "当前未连接", Toast.LENGTH_SHORT).show();
                }
                sendType = 1;
                sendText();
                break;
            case R.id.btn_sendcamera:
                if (mWifiP2pInfo == null) {
                    Toast.makeText(this, "当前未连接", Toast.LENGTH_SHORT).show();
                }
                sendType = 3;

                //startRecord();
                sendVideo();

                break;
            case R.id.btn_cancelconnect:
                sendType = 0;
                wifiUtils.disconnect();
                cancelConnect();
            default:

                break;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Toast.makeText(SendCameraActivity.this, "连接出错", Toast.LENGTH_SHORT).show();
                isConnected = false;
            }
            if (msg.what == 2) {
                Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                isConnected = true;
            }
        }
    };


    private void cancelConnect() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "取消成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "取消失败");
            }
        });
    }

    //发送文本信息
    public void sendText() {
        if (!isConnected) {
            Toast.makeText(this, "请连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        // 从EditText中得到数据
        String input = mInput.getText().toString().trim();
        if (input != null && !input.isEmpty()) {
            // 发送数据
            wifiUtils.send(input.getBytes(), "text");
        } else {
            // 校验数据格式
            Toast.makeText(this, "输入信息不能为空", Toast.LENGTH_SHORT).show();
        }
    }

    //发送图片
    public void sendPhoto() {
        if (!isConnected) {
            Toast.makeText(this, "请连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        mark = false;
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                wifiUtils.send(bytes, "photo");
                mCamera.startPreview();
            }
        });

    }

    //发送视频 其实也是发送一张一张的图片
    public void sendVideo() {
        if (!isConnected) {
            Toast.makeText(this, "请连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        mark = true;

        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                count++;
                Camera.Size size = camera.getParameters().getPreviewSize();
                final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, stream);
                byte[] imageBytes = stream.toByteArray();
                if (count % 2 == 0 && mark) {
                    Log.d(TAG, "发送数据");
                    wifiUtils.send(imageBytes, "video");
                }
            }
        });
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
                    if (mWifiP2pInfo == null || mWifiP2pInfo.groupOwnerAddress == null) {
                        Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    } else {
                        //Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        wifiUtils.connect(mWifiP2pInfo, mHandler);
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    isConnected = false;
                    Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });

            mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                    Log.d(TAG, "获得连接对象");
                    mWifiP2pInfo = wifiP2pInfo;
                }
            });
        }
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
        //prepareVideoRecorder();
        try {
            mCamera = Camera.open();
            Camera.Parameters mPara = mCamera.getParameters();
            List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
            List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();

            int previewSizeIndex = -1;
            Camera.Size psize;
            int height_sm = 999999;
            int width_sm = 999999;
            //获取设备最小分辨率图片，图片越清晰，传输越卡
            for (int i = 0; i < previewSizes.size(); i++) {
                psize = previewSizes.get(i);
                if (psize.height <= height_sm && psize.width <= width_sm) {
                    previewSizeIndex = i;
                    height_sm = psize.height;
                    width_sm = psize.width;
                }
            }

            if (previewSizeIndex != -1) {
                mWidth = previewSizes.get(previewSizeIndex).width;
                mHeight = previewSizes.get(previewSizeIndex).height;
                mPara.setPreviewSize(mWidth, mHeight);
            }
            mCamera.setParameters(mPara);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

            size = mCamera.getParameters().getPreviewSize();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                connect(wifiP2pDevice);
            }
        });
    }

    private boolean prepareVideoRecorder() {
        //getCameraInstance();
        //mCamera = Camera.open();
        //Camera open = Camera.open();

        // Step 1: Unlock and set camera to MediaRecorder
        //open.unlock();
        //mCamera.stopPreview();
        releaseCamera();
        mCamera = Camera.open();
        mCamera.unlock();
        mediaRecorder = new MediaRecorder();

        //mediaRecorder.setCamera(mCamera);

        mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.i(TAG, "Error");
            }
        });
        //设置音频源，视频源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        //mediaRecorder.setAudioSamplingRate(8000);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        //mediaRecorder.setVideoSize(1920, 1080);
        // Step 4: Set output file
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        //Camera.Parameters parameters = mCamera.getParameters();
        //parameters.setPreviewSize(mWidth, mHeight);
        //mCamera.setParameters(parameters);
        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

//        // Step 3: Set output format and encoding (for versions prior to API Level 8)
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
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
