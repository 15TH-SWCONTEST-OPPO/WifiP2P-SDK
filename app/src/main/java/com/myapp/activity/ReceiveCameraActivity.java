package com.myapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import com.google.android.material.navigation.NavigationView;
import com.myapp.Constant;
import com.myapp.ProgressDialog;
import com.myapp.R;
import com.myapp.Wifip2pCameraService;
import com.myapp.utils.FilePathUtils;
import com.myapp.utils.MeiTuAIUtil;
import com.myapp.utils.NettyState;
import com.myapp.utils.NettyUtils;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.vod.upload.VODUploadCallback;
import com.alibaba.sdk.android.vod.upload.VODUploadClient;
import com.alibaba.sdk.android.vod.upload.VODUploadClientImpl;
import com.alibaba.sdk.android.vod.upload.model.UploadFileInfo;
import com.alibaba.sdk.android.vod.upload.model.VodInfo;

import okhttp3.Response;

public class ReceiveCameraActivity extends BaseActivity {

    private static final String TAG = "ReceiveCameraActivity";
    private Wifip2pCameraService.MyBinder mBinder;
    private ProgressDialog mProgressDialog;


    private ImageView cameraView;
    private ImageView photoView;
    private boolean isConnected = false;
    private boolean isGroupFormed = false;
    private boolean isServerStarted = false;
    private Map<String, Integer> viewMap;
    private NettyUtils nettyUtils;
    private ViewGroup imageViews;

    private String selectedName = "None";

    private DrawerLayout drawer;

    private Toolbar toolbar;

    private String beautyPath = null;

    //@RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_camera_drawer);
        imageViews = findViewById(R.id.ll_group);

        nettyUtils = new NettyUtils();
        nettyUtils.setUpService();

        viewMap = new HashMap<>();

        initWifi();
        initView();




    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sendCommand(String command) {
        if (!isServerStarted) {
            Toast.makeText(this, "服务未开启", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedName.equals(NettyState.DEVICE_NONE)) {
            Toast.makeText(this, "当前无选中的设备", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(ReceiveCameraActivity.this, "发送指令", Toast.LENGTH_SHORT).show();
        if (command.equals(Constant.SENDCAMERA)) {
            nettyUtils.sendCommand(selectedName, Constant.SENDCAMERA.getBytes(), "text");
        } else if (command.equals(Constant.SENDIMAGE)) {
            nettyUtils.sendCommand(selectedName, Constant.SENDIMAGE.getBytes(), "text");
        } else if (command.equals(Constant.DISCONNECT)) {
            nettyUtils.sendCommand(selectedName, Constant.DISCONNECT.getBytes(), "text");
        } else if (command.equals(Constant.STARTRECORD)) {
            nettyUtils.sendCommand(selectedName, Constant.STARTRECORD.getBytes(), "text");
        } else if (command.equals(Constant.STOPRECORD)) {
            nettyUtils.sendCommand(selectedName, Constant.STOPRECORD.getBytes(), "text");
        } else if (command.equals(Constant.HIGHQUALITY)) {
            nettyUtils.sendCommand(selectedName, Constant.HIGHQUALITY.getBytes(), "text");
        } else if (command.equals(Constant.MEDIUMQUALITY)) {
            nettyUtils.sendCommand(selectedName, Constant.MEDIUMQUALITY.getBytes(), "text");
        } else if (command.equals(Constant.LOWQUALITY)) {
            nettyUtils.sendCommand(selectedName, Constant.LOWQUALITY.getBytes(), "text");
        } else {
            Toast.makeText(this, "无效指令", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        //cameraView = (ImageView) findViewById(R.id.camera_video);
        photoView = (ImageView) findViewById(R.id.camera_photo);
        toolbar = (Toolbar) findViewById(R.id.toolbar_receive);
        //处理抽屉
        NavigationView navigationview = (NavigationView) findViewById(R.id.navigation_view_receive);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout_receive);

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
            @RequiresApi(api = Build.VERSION_CODES.N)
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                //安卓
                //Toast.makeText(getApplicationContext(), menuItem.getTitle(), Toast.LENGTH_LONG).show();

                switch (menuItem.getItemId()) {
                    case R.id.create_group:
                        removeGroup(false);
                        createGroup();
                        break;
                    case R.id.remove_group:
                        removeGroup(true);
                        break;
                    case R.id.stop_server:
                        stopNetty();
                        break;
                    case R.id.rsend_camera:
                        sendCommand(Constant.SENDCAMERA);
                        break;
                    case R.id.rsend_photo:
                        sendCommand(Constant.SENDIMAGE);
                        break;
                    case R.id.receive_start_record:
                        sendCommand(Constant.STARTRECORD);
                        break;
                    case R.id.receive_stop_record:
                        sendCommand(Constant.STOPRECORD);
                        break;
                    case R.id.rdisconnect:
                        sendCommand(Constant.DISCONNECT);
                        break;
                    case R.id.beauty:
                        beauty();
                        break;
                    case R.id.rquality_high:
                        sendCommand(Constant.HIGHQUALITY);
                        break;
                    case R.id.rquality_mid:
                        sendCommand(Constant.MEDIUMQUALITY);
                        break;
                    case R.id.rquality_low:
                        sendCommand(Constant.LOWQUALITY);
                        break;
                }

                //设置哪个按钮被选中
//                menuItem.setChecked(true);
                //关闭侧边栏
//                drawer.closeDrawers();
                return false;
            }
        });
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

    @SuppressLint("HandlerLeak")
    private void beauty() {
        if (beautyPath == null) {
            Toast.makeText(ReceiveCameraActivity.this, "当前选中图片为空", Toast.LENGTH_SHORT).show();
        }
        Handler handler = new Handler() {

            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    Toast.makeText(ReceiveCameraActivity.this, "美颜出现异常", Toast.LENGTH_SHORT).show();
                } else if (msg.what == 1) {
//                    Response response = (Response) msg.obj;
//                    System.out.println(response);
                    Bitmap bitmap = (Bitmap) msg.obj;
                    photoView.setImageBitmap(bitmap);
                    try {
                        FilePathUtils.saveBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (msg.what == 2) {
                    Toast.makeText(ReceiveCameraActivity.this, "请检查图片中是否有人脸", Toast.LENGTH_SHORT).show();
                }
            }
        };
        try {
            MeiTuAIUtil.doPost(beautyPath, 70, "jpg", handler);
        } catch (Exception e) {
            Toast.makeText(ReceiveCameraActivity.this, "美颜出现异常", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void stopNetty() {
//        if (!isServerStarted) {
//            Toast.makeText(ReceiveCameraActivity.this, "服务未开启", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if(!isServerStarted){
            Toast.makeText(ReceiveCameraActivity.this, "服务未开启", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(ReceiveCameraActivity.this, "服务已关闭", Toast.LENGTH_SHORT).show();
        }
        isServerStarted = false;
        nettyUtils.stopServer();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initWifi() {
        nettyUtils.setNettyConnectionListener(new NettyUtils.NettyConnectionListener() {
            @Override
            public void onDeviceConnected(String name) {
                ImageView imageView = new ImageView(ReceiveCameraActivity.this);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int idx = -1;
                        for (int i = 0; i < imageViews.getChildCount(); i++) {
                            if (imageView == imageViews.getChildAt(i)) {
                                idx = i;
                                break;
                            }
                        }

                        for (String str : viewMap.keySet()) {
                            if (viewMap.get(str) == idx) {
                                selectedName = str;
                                break;
                            }
                        }
                        Toast.makeText(ReceiveCameraActivity.this, "当前选中用户为" + selectedName, Toast.LENGTH_SHORT).show();
                    }
                });
                imageViews.addView(imageView);
                viewMap.put(name, imageViews.getChildCount() - 1);
            }

            @Override
            public void onDeviceDisconnected(String name) {
                if (viewMap.containsKey(name)) {
                    imageViews.removeViewAt(viewMap.get(name));
                }
            }

            @Override
            public void onDeviceConnectionFailed() {

            }
        });

        nettyUtils.setOnDataReceivedListener(new NettyUtils.OnNettyDataReceivedListener() {
            @Override
            public void onDataReceived(String name, byte[] data, String message) {

                if (!viewMap.containsKey(name)) return;
                int idx = viewMap.get(name);
                ImageView imageView = (ImageView) imageViews.getChildAt(idx);

                if (imageView == null) return;
                if (message.equals("text") && data.length != 0) {
                    String text = new String(data);
                    Toast.makeText(ReceiveCameraActivity.this, text, Toast.LENGTH_SHORT).show();
                } else if (message.equals("photo") && data.length != 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    try {
                        beautyPath = FilePathUtils.saveBitmap(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    photoView.setImageBitmap(bitmap);
                    //imageView.setImageBitmap(bitmap);
                } else if (message.equals("video") && data.length != 0) {
                    Matrix matrix = new Matrix();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    matrix.postScale(2.0F, 2.0F);
                    //matrix.setRotate(90, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                    Bitmap newBitmap = bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

//                    newBitmap.setWidth(400);
//                    newBitmap.setHeight(300);
                    imageView.setLayoutParams(new LinearLayout.LayoutParams(800, 600));
                    imageView.setImageBitmap(newBitmap);
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    final Handler nettyHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0:
                    isServerStarted = true;
                    Toast.makeText(ReceiveCameraActivity.this, "服务开启成功", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    isServerStarted = false;
                    Toast.makeText(ReceiveCameraActivity.this, "服务异常", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void startNetty() {
        if (!isGroupFormed) {
            Toast.makeText(ReceiveCameraActivity.this, "未创建群组", Toast.LENGTH_SHORT).show();
            return;
        }
        nettyUtils.startService(nettyHandler);
    }

    /**
     * 创建组群，等待连接
     */
    public void createGroup() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ReceiveCameraActivity.this, "请赋予权限", Toast.LENGTH_SHORT).show();
            return;
        }
        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isGroupFormed = true;
                Log.e(TAG, "创建群组成功");
                ///Toast.makeText(ReceiveCameraActivity.this, "创建群组成功", Toast.LENGTH_SHORT).show();
                startNetty();

            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "创建群组失败: " + reason);
                Toast.makeText(ReceiveCameraActivity.this, "创建群组失败,请移除已有的组群或者连接同一WIFI重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 移除组群
     */
    public void removeGroup(boolean isShow) {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isGroupFormed = false;
                Log.e(TAG, "移除组群成功");
                if (isShow) {
                    nettyUtils.stopServer();
                    isServerStarted = false;
                    nettyUtils.releaseClient();
                    Toast.makeText(ReceiveCameraActivity.this, "移除组群成功", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "移除组群失败");
                if (isShow) {
                    Toast.makeText(ReceiveCameraActivity.this, "移除组群失败,请创建组群重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServerStarted) {
            nettyUtils.stopServer();
        }

        removeGroup(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //onDestroy();
        isServerStarted = false;
        nettyUtils.stopServer();
        nettyUtils.releaseClient();
        removeGroup(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        nettyUtils = new NettyUtils();
        nettyUtils.setUpService();

        viewMap = new HashMap<>();

        initWifi();
        initView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeGroup(false);
    }
}
