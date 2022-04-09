package com.myapp.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import com.myapp.Constant;
import com.myapp.ProgressDialog;
import com.myapp.R;
import com.myapp.Wifip2pCameraService;
import com.myapp.utils.NettyState;
import com.myapp.utils.NettyUtils;

public class ReceiveCameraActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "ReceiveCameraActivity";
    private Wifip2pCameraService.MyBinder mBinder;
    private ProgressDialog mProgressDialog;


    private ImageView cameraView;
    private ImageView photoView;
    private boolean isConnected = false;

    private Map<String, Integer> viewMap;
    private NettyUtils nettyUtils;
    private ViewGroup imageViews;

    private String selectedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_camera);
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        Button btnRemove = (Button) findViewById(R.id.btn_remove);
        Button sendCamera = findViewById(R.id.btn_rsend_camera);
        Button sendPhoto = findViewById(R.id.btn_rsend_image);
        Button disConnect = findViewById(R.id.btn_disconnect);
        Button startNetty = findViewById(R.id.rstart_netty);
        Button startRecord = findViewById(R.id.rstart_record);
        Button stopRecord = findViewById(R.id.rstop_record);
        imageViews = findViewById(R.id.ll_group);

        btnCreate.setOnClickListener(this);
        btnRemove.setOnClickListener(this);
        disConnect.setOnClickListener(this);
        sendCamera.setOnClickListener(this);
        sendPhoto.setOnClickListener(this);
        startRecord.setOnClickListener(this);
        stopRecord.setOnClickListener(this);
        startNetty.setOnClickListener(this);

        nettyUtils = new NettyUtils();
        nettyUtils.setUpService();

        viewMap = new HashMap<>();

        initWifi();
        initView();
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sendCommand(String command) {

        if(selectedName.equals(NettyState.DEVICE_NONE)){
            Toast.makeText(this,"当前无选中的设备",Toast.LENGTH_SHORT).show();
        }
        if (command.equals(Constant.SENDCAMERA)) {
            nettyUtils.sendCommand(selectedName,Constant.SENDCAMERA.getBytes(), "text");
        } else if (command.equals(Constant.SENDIMAGE)) {
            nettyUtils.sendCommand(selectedName,Constant.SENDIMAGE.getBytes(), "text");
        } else if (command.equals(Constant.DISCONNECT)) {
            nettyUtils.sendCommand(selectedName,Constant.DISCONNECT.getBytes(), "text");
            removeGroup();
            createGroup();
        } else if (command.equals(Constant.STARTRECORD)) {
            nettyUtils.sendCommand(selectedName,Constant.STARTRECORD.getBytes(), "text");
        } else if (command.equals(Constant.STOPRECORD)) {
            nettyUtils.sendCommand(selectedName,Constant.STOPRECORD.getBytes(), "text");
        } else {
            Toast.makeText(this, "无效指令", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        cameraView = (ImageView) findViewById(R.id.camera_video);
        photoView = (ImageView) findViewById(R.id.camera_photo);
    }

    private void initWifi() {
        nettyUtils.setNettyConnectionListener(new NettyUtils.NettyConnectionListener() {
            @Override
            public void onDeviceConnected(String name) {
                ImageView imageView = new ImageView(ReceiveCameraActivity.this);
                imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 200));
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
                            if(viewMap.get(str)==idx){
                                selectedName = str;
                                break;
                            }
                        }
                        Toast.makeText(ReceiveCameraActivity.this,"当前选中用户为"+selectedName,Toast.LENGTH_SHORT).show();
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
                    imageView.setImageBitmap(bitmap);
                } else if (message.equals("video") && data.length != 0) {
                    imageView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_create:
                createGroup();
                break;
            case R.id.btn_remove:
                removeGroup();
                break;
            case R.id.btn_rsend_camera:
                sendCommand(Constant.SENDCAMERA);
                break;
            case R.id.btn_rsend_image:
                sendCommand(Constant.SENDIMAGE);
                break;
            case R.id.btn_disconnect:
                sendCommand(Constant.DISCONNECT);
                break;
            case R.id.rstart_record:
                sendCommand(Constant.STARTRECORD);
                break;
            case R.id.rstop_record:
                sendCommand(Constant.STOPRECORD);
            case R.id.rstart_netty:
                startNetty();
                break;
        }
    }

    private void startNetty() {
        try {
            nettyUtils.startService();
        } catch (InterruptedException e) {
            Toast.makeText(ReceiveCameraActivity.this, "开启服务失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
                Log.e(TAG, "创建群组成功");
                Toast.makeText(ReceiveCameraActivity.this, "创建群组成功", Toast.LENGTH_SHORT).show();
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
    public void removeGroup() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "移除组群成功");
                Toast.makeText(ReceiveCameraActivity.this, "移除组群成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "移除组群失败");
                Toast.makeText(ReceiveCameraActivity.this, "移除组群失败,请创建组群重试", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
