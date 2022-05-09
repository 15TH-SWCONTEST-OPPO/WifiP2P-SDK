package com.myapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.SystemClock;
import android.util.Log;
import android.util.Range;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
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

//import com.aiunit.core.FrameData;
//import com.aiunit.vision.common.ConnectionCallback;
//import com.aiunit.vision.common.FrameInputSlot;
//import com.aiunit.vision.common.FrameOutputSlot;
//import com.coloros.ocs.ai.cv.CVUnit;
//import com.coloros.ocs.ai.cv.CVUnitClient;
//import com.coloros.ocs.base.common.ConnectionResult;
//import com.coloros.ocs.base.common.api.OnConnectionFailedListener;
//import com.coloros.ocs.base.common.api.OnConnectionSucceedListener;
//import com.myapp.oppoapi.CVClientUtils;
import com.google.android.material.navigation.NavigationView;
import com.myapp.Constant;
import com.myapp.FileBean;
import com.myapp.R;

import com.myapp.utils.FilePathUtils;
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
public class SendCameraActivity extends BaseActivity implements SurfaceHolder.Callback {

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

    private Boolean isNettyConnected = false;
    private Boolean isWifiConnected = false;
    private MediaRecorder mediaRecorder;

    //camera的相关字段
    private boolean isOpened = false;
    private ImageReader imageReader;
    private Handler mainHandler;
    private Handler childHandler;
    private CameraDevice mCameraDevice;

    private ArrayAdapter<String> adapter;

    //动态设置图片的质量
    private int quality = 50;

    private int oriention = 0;

    private static Range<Integer>[] fpsRanges;

    private CameraCaptureSession mCameraCaptureSession;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private SurfaceView mPreview;

    private Surface mRecorderSurface;
    private boolean isPicture = false;

    private boolean isRecording = false;

    private NettyUtils nettyUtils;

    private DrawerLayout drawer;

    private Toolbar toolbar;

    private TextView newBtn;

    //private CVClientUtils cvClientUtils;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_camera_2);
        //处理UI部分
        mTvDevice = (ListView) findViewById(R.id.lv_device);
        mInput = findViewById(R.id.edit_text);
        newBtn = findViewById(R.id.new_btn_record);

        newBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

        // 处理连接部分
        nettyUtils = new NettyUtils();
        nettyUtils.createClient();

        initWifi();
        // 处理摄像头展示部分
        mPreview = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mPreview.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

        //获取到可持久化的surface用于视频的录制
        mRecorderSurface = MediaCodec.createPersistentInputSurface();

        initDrawer();

        //cvClientUtils = new CVClientUtils(SendCameraActivity.this);
        //cvClientUtils.connect2AIUnitServer();

        /*
         * icon图标
         * */
        // 加载字体文件
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");
        // client
        newBtn.setTypeface(iconfont);
    }

    private void initDrawer() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //处理抽屉
        NavigationView navigationview = (NavigationView) findViewById(R.id.navigation_view);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.setDrawerListener(toggle);//初始化状态
        toggle.syncState();
        /*---------------------------添加头布局和尾布局-----------------------------*/
        //获取xml头布局view
        View headerView = navigationview.getHeaderView(0);

        navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        });
        ColorStateList csl = (ColorStateList) getResources().getColorStateList(R.color.nav_menu_text_color);
        //设置item的条目颜色
        navigationview.setItemTextColor(csl);
        //去掉默认颜色显示原来颜色  设置为null显示本来图片的颜色
        navigationview.setItemIconTintList(csl);


        //设置条目点击监听
        navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                //安卓
                //Toast.makeText(getApplicationContext(), menuItem.getTitle(), Toast.LENGTH_LONG).show();

                switch (menuItem.getItemId()) {
                    case R.id.search_device:

                        cancelConnect(false);
                        mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                        mDialog.show();
                        mDialog.setCancelable(false);
                        mDialog.setContentView(R.layout.loading_progressba);
                        //搜索设备
                        connectServer();
                        break;
                    case R.id.send_photo:
                        if (!isWifiConnected) {
                            Toast.makeText(SendCameraActivity.this, "当前未连接", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        sendPhoto();
                        break;

                    case R.id.send_camera:
                        if (!isWifiConnected) {
                            Toast.makeText(SendCameraActivity.this, "当前未连接", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        if (!isNettyConnected) {
                            Toast.makeText(SendCameraActivity.this, "未连接服务", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        mark = true;
                        break;

                    case R.id.remove_wifi:
                        releaseClient();
                        cancelConnect(true);
                        break;

                    case R.id.disconnect:
                        //startNetty();
                        releaseClient();
                        break;
                    case R.id.connect:
                        startNetty();
                        break;
                    case R.id.device_list:
                        initPopWindow((View) toolbar);
                        break;
                    case R.id.quality_high:
                        quality = 70;
                        Toast.makeText(SendCameraActivity.this, "切换为高画质", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.quality_mid:
                        quality = 45;
                        Toast.makeText(SendCameraActivity.this, "切换为高中等画质", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.quality_low:
                        quality = 25;
                        Toast.makeText(SendCameraActivity.this, "切换为低画质", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void releaseClient() {
//        if (!isNettyConnected) {
//            Toast.makeText(SendCameraActivity.this, "服务未连接", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (!isNettyConnected) {
            Toast.makeText(SendCameraActivity.this, "服务未连接", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SendCameraActivity.this, "服务已关闭", Toast.LENGTH_SHORT).show();
        }
        isNettyConnected = false;
        nettyUtils.releaseClient();
    }

    private void initPopWindow(View v) {
        drawer.closeDrawers();
        View view = LayoutInflater.from(SendCameraActivity.this).inflate(R.layout.popwin_layout, null, false);
        //1.构造一个PopupWindow，参数依次是加载的 View，宽高

        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popWindow.setAnimationStyle(R.anim.anim_pop);  //设置加载动画

        //这些为了点击非PopupWindow区域，PopupWindow会消失的，如果没有下面的
        //代码的话，你会发现，当你把PopupWindow显示出来了，无论你按多少次后退键
        //PopupWindow并不会关闭，而且退不出程序，加上下述代码可以解决这个问题

        popWindow.setTouchable(true);
        popWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));    //要为popWindow设置一个背景才有效

        //设置 popupWindow 显示的位置，参数依次是参照 View，x轴的偏移量，y轴的偏移量
        popWindow.showAsDropDown(v, 50, 0);

//        Button btn_web = (Button) view.findViewById(R.id.test1);
        mTvDevice = view.findViewById(R.id.lv_device);
        adapter = new ArrayAdapter(view.getContext(), android.R.layout.simple_list_item_1, mListDeviceName);
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
        //设置 popupWindow 里的按钮的事件
    }


    //设置开始录制和结束录制的方法
    void startRecord() {
        if (isRecording) {
            return;
        }
        if (mediaRecorder == null) {
            Toast.makeText(SendCameraActivity.this, "相机还未准备好", Toast.LENGTH_SHORT).show();
            return;
        }
        newBtn.setEnabled(false);
        mediaRecorder.start();
        SystemClock.sleep(300);
//        newBtn.setImageDrawable(getResources().getDrawable(R.mipmap.stop_record));
        newBtn.setText(getResources().getString(R.string.end));
        newBtn.setTextColor(0xffff0000);
        newBtn.setTextSize(40);


        isRecording = true;
        Toast.makeText(SendCameraActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
        SystemClock.sleep(300);
        newBtn.setEnabled(true);

//        try{
//
//        }catch (RuntimeException e){
//            newBtn.setImageDrawable(getResources().getDrawable(R.mipmap.start_record));
//            isRecording = false;
//            Toast.makeText(SendCameraActivity.this,"您点的太快了",Toast.LENGTH_SHORT).show();
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void stopRecord() {
        if (!isRecording) {
            return;
        }
        if (mediaRecorder == null) {
            Toast.makeText(SendCameraActivity.this, "相机还未为准备好", Toast.LENGTH_SHORT).show();
            return;
        }
        newBtn.setEnabled(false);

        mediaRecorder.stop();
        SystemClock.sleep(300);
        releaseMediaRecorder();
        setUpMediaRecorder();
        SystemClock.sleep(300);
        isRecording = false;
        Toast.makeText(SendCameraActivity.this, "停止录制", Toast.LENGTH_SHORT).show();
//        newBtn.setImageDrawable(getResources().getDrawable(R.mipmap.start_record));
        newBtn.setText(getResources().getString(R.string.start));
        newBtn.setTextSize(60);
        newBtn.setEnabled(true);
        newBtn.setTextColor(0xffffffff);
//        try {
//
//        }catch (RuntimeException e){
//            isRecording = true;
//            newBtn.setImageDrawable(getResources().getDrawable(R.mipmap.stop_record));
//            Toast.makeText(SendCameraActivity.this,"您点的太快了",Toast.LENGTH_SHORT).show();
//        }

    }


    private void startNetty() {
        if (!isWifiConnected) {
            Toast.makeText(SendCameraActivity.this, "Wifi未连接", Toast.LENGTH_SHORT).show();
        }
        if (isNettyConnected) {
            Toast.makeText(SendCameraActivity.this, "客户端已开启", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (mWifiP2pInfo != null) {
                nettyUtils.connect2Server(mWifiP2pInfo.groupOwnerAddress.getHostAddress());
                isNettyConnected = true;
            } else {
                isNettyConnected = false;
                System.out.println("连接为空");
            }
        } catch (Exception e) {
            isNettyConnected = false;
            System.err.println(e);
        }
    }

    private void sendPhoto() {
        if (!isNettyConnected) {
            Toast.makeText(SendCameraActivity.this, "未连接服务", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(SendCameraActivity.this, "发送图片", Toast.LENGTH_SHORT).show();
        takePicture();
    }

    private void initWifi() {
        nettyUtils.setOnDataReceivedListener(new NettyUtils.OnNettyDataReceivedListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataReceived(String name, byte[] data, String message) {
                if (message.equals("text") && data.length != 0) {
                    String text = new String(data);
                    //Toast.makeText(SendCameraActivity.this, text, Toast.LENGTH_SHORT).show();
                    if (text.equals(Constant.SENDIMAGE)) {
                        sendPhoto();
                        Toast.makeText(SendCameraActivity.this, "收到指令", Toast.LENGTH_SHORT).show();
                    } else if (text.equals(Constant.STARTRECORD)) {
                        startRecord();
                    } else if (text.equals(Constant.STOPRECORD)) {
                        stopRecord();
                    } else if (text.equals(Constant.SENDCAMERA)) {
                        mark = true;
                    } else if (text.equals(Constant.DISCONNECT)) {
                        mark = false;
                        isPicture = false;
                        stopRecord();
                        releaseClient();
                    } else if (text.equals(Constant.HIGHQUALITY)) {
                        quality = 70;
                        Toast.makeText(SendCameraActivity.this, "切换为高画质", Toast.LENGTH_SHORT).show();
                    } else if (text.equals(Constant.MEDIUMQUALITY)) {
                        quality = 45;
                        Toast.makeText(SendCameraActivity.this, "切换为中等画质", Toast.LENGTH_SHORT).show();
                    } else if (text.equals(Constant.LOWQUALITY)) {
                        quality = 25;
                        Toast.makeText(SendCameraActivity.this, "切换为低画质", Toast.LENGTH_SHORT).show();
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
                isWifiConnected = false;
            }
            if (msg.what == 2) {

                if (mDialog == null) {
                    mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                }

                mDialog.dismiss();
                Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                isWifiConnected = true;
            }
        }
    };


    private void cancelConnect(boolean isShow) {
        mark = false;
        isWifiConnected = false;
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (isShow) {
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
        if (!isNettyConnected) {
            Toast.makeText(SendCameraActivity.this, "未连接服务", Toast.LENGTH_SHORT).show();
            return;
        }
        nettyUtils.sendData("你好".getBytes(), "text");
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
//                if (mDialog == null) {
//                    mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
//                }
//                mDialog.dismiss();
                //adapter.notifyDataSetChanged();
                //initPopWindow(toolbar);
            }

            @Override
            public void onFailure(int reasonCode) {
                //进度条消失
                if (mDialog == null) {
                    mDialog = new AlertDialog.Builder(SendCameraActivity.this, R.style.Transparent).create();
                }
                mDialog.dismiss();
                Toast.makeText(SendCameraActivity.this, "搜索设备失败，请检查手机wifi设置", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "搜索设备失败");
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        if(oriention==0){
//            mPreview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,500));
//            oriention = 1;
//        }else{
//            mPreview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,400));
//            oriention = 0;
//        }

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
                        isWifiConnected = true;
                        startNetty();
                        Toast.makeText(SendCameraActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    isWifiConnected = false;
                    if (connectTime >= 2) {
                        connectTime = 0;
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        //Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }

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
            // 该相机的FPS范围
            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d("FPS", "SYNC_MAX_LATENCY_PER_FRAME_CONTROL: " + Arrays.toString(fpsRanges));
            // 设置预览画面的帧率 视实际情况而定选择一个帧率范围

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Log.i(TAG, "onOpened");
                    try {
                        CaptureRequest.Builder builder=camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        builder.set(CaptureRequest.CONTROL_AE_MODE,1);
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        Log.d(TAG, "onOpened: builder changed");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
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

            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[0]);
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
        mediaRecorder.setOutputFile(FilePathUtils.getOutputMediaFile(FilePathUtils.MEDIA_TYPE_VIDEO));
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
//        final CaptureRequest.Builder captureRequestBuilder;
//        try {
//            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            // 将imageReader的surface作为CaptureRequest.Builder的目标
//            captureRequestBuilder.addTarget(imageReader.getSurface());
//            // 自动对焦
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            // 自动曝光
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            // 获取手机方向
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            // 根据设备方向计算设置照片的方向
//            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//            //拍照
//            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
//            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
    }

    private CamcorderProfile getCamcorderProfile() {
        return CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
    }

    @SuppressLint("HandlerLeak")
    private Handler photoHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            byte[] bytes = (byte[]) msg.obj;
            switch (msg.what) {
                case 0:
                    System.out.println("发送图片");
                    //ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//                    byte[] transfrom = runAIUnitServer(bitmapImage);
//                    Bitmap transfromed = BitmapFactory.decodeByteArray(transfrom, 0, bytes.length, null);

                    nettyUtils.sendData(compressImage(bitmapImage, quality), "photo");
                    //bitmapImage.recycle();
                    break;
                case 1:
                    //buffer = image.getPlanes()[0].getBuffer();

                    bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    nettyUtils.sendData(compressImage(bitmapImage, quality), "video");
                    break;
            }

        }
    };

    private void setUpImageReader() {
        imageReader = ImageReader.newInstance(400, 300, ImageFormat.JPEG, 10);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {


            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                } catch (Exception e) {

                }
                if (image != null && isPicture) {
                    isPicture = false;
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    Message msg = new Message();
                    msg.what = 0;
                    msg.obj = bytes;
                    photoHandler.sendMessage(msg);
                    image.close();
                    return;
                }

                if (image != null && mark) {
                    //System.out.println("有图像");
                    Message msg = new Message();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    msg.what = 1;
                    msg.obj = bytes;
                    photoHandler.sendMessage(msg);
//                    Bitmap transfromed;
//                    try {
//                        transfromed = cvClientUtils.runAIUnitServer(bitmapImage);
//                    } catch (Exception e) {
//                        transfromed = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//                        e.printStackTrace();
//                    }
//                    if(transfromed!=null){
//
//                    }
                }
                if (image != null) {
                    image.close();
                }
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
//        BitmapFactory.Options options = new BitmapFactory.Options();
//
//        Bitmap copy = image.copy(Bitmap.Config.RGB_565, true);
//        Matrix matrix = new Matrix();
//        matrix.setScale(0.5f, 0.5f);
//        copy = Bitmap.createBitmap(copy, 0, 0, copy.getWidth(),
//                copy.getHeight(), matrix, true);
//
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.out.println("压缩前" + (double) image.getByteCount() / 1024.0 / 1024.0);
        image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        //int options = 100;
        /*while (outputStream.toByteArray().length / 1024 > imageSize) {
            outputStream.reset();
            image.compress(Bitmap.CompressFormat.JPEG, options, outputStream);
            options -= 10;
        }*/
        byte[] bytes = outputStream.toByteArray();
        System.out.println("压缩后" + (double) bytes.length / 1024.0 / 1024.0);
        return bytes;
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
        //showDeviceInfo();
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
    protected void onStart() {
        super.onStart();
        //onCreate(new Bundle());
    }

    @Override
    protected void onStop() {
        super.onStop();
        //onDestroy();
        isNettyConnected = false;
        isWifiConnected = false;
        cancelConnect(false);
        nettyUtils.releaseClient();
        releaseMediaRecorder();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        openCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isNettyConnected = false;
        isWifiConnected = false;
        nettyUtils.releaseClient();
        cancelConnect(false);
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
    }


}
