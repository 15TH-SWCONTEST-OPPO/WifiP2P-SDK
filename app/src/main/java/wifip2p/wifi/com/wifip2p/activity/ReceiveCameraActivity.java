package wifip2p.wifi.com.wifip2p.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;

import wifip2p.wifi.com.wifip2p.Constant;
import wifip2p.wifi.com.wifip2p.ProgressDialog;
import wifip2p.wifi.com.wifip2p.R;
import wifip2p.wifi.com.wifip2p.WifiState;
import wifip2p.wifi.com.wifip2p.Wifip2pCameraService;
import wifip2p.wifi.com.wifip2p.utils.WifiUtils;

public class ReceiveCameraActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "ReceiveCameraActivity";
    private Wifip2pCameraService.MyBinder mBinder;
    private ProgressDialog mProgressDialog;
    private Intent mIntent;

    private WifiUtils wifiUtils;
    private ImageView cameraView;
    private ImageView photoView;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_camera);
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        Button btnRemove = (Button) findViewById(R.id.btn_remove);
        Button sendCamera = findViewById(R.id.btn_rsend_camera);
        Button sendPhoto = findViewById(R.id.btn_rsend_image);
        Button disConnect = findViewById(R.id.btn_disconnect);

        Button startRecord = findViewById(R.id.rstart_record);
        Button stopRecord = findViewById(R.id.rstop_record);

        btnCreate.setOnClickListener(this);
        btnRemove.setOnClickListener(this);
        disConnect.setOnClickListener(this);
        sendCamera.setOnClickListener(this);
        sendPhoto.setOnClickListener(this);
        startRecord.setOnClickListener(this);
        stopRecord.setOnClickListener(this);

        wifiUtils = new WifiUtils(this);

        //wifiUtils.setupService();
        //wifiUtils.startService(true);

        initWifi();
        initView();
    }

    public void sendCommand(String command) {
        if (!isConnected) {
            Toast.makeText(this, "请连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        if (command.equals(Constant.SENDCAMERA)) {
            wifiUtils.send(Constant.SENDCAMERA.getBytes(), "text");
        } else if (command.equals(Constant.SENDIMAGE)) {
            wifiUtils.send(Constant.SENDIMAGE.getBytes(), "text");
        } else if (command.equals(Constant.DISCONNECT)) {
            wifiUtils.send(Constant.DISCONNECT.getBytes(), "text");
            removeGroup();
            createGroup();
        } else if (command.equals(Constant.STARTRECORD)) {
            wifiUtils.send(Constant.STARTRECORD.getBytes(), "text");
        } else if (command.equals(Constant.STOPRECORD)) {
            wifiUtils.send(Constant.STOPRECORD.getBytes(), "text");
        } else {
            Toast.makeText(this, "无效指令", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        cameraView = (ImageView) findViewById(R.id.camera_video);
        photoView = (ImageView) findViewById(R.id.camera_photo);
    }

    private void initWifi() {
        wifiUtils.setOnDataReceivedListener(new WifiUtils.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                if (message.equals("text") && data.length != 0) {
                    String text = new String(data);
                    Toast.makeText(ReceiveCameraActivity.this, text, Toast.LENGTH_SHORT).show();
                } else if (message.equals("photo") && data.length != 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    photoView.setImageBitmap(bitmap);
                } else if (message.equals("video") && data.length != 0) {
                    cameraView.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                }
            }
        });

        wifiUtils.setWifiConnectionListener(new WifiUtils.WifiConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                isConnected = true;
                Toast.makeText(ReceiveCameraActivity.this, "wifi已连接", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                isConnected = false;
                Toast.makeText(ReceiveCameraActivity.this, "wifi已断开", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() {

            }
        });

    }

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
                break;
        }
    }

    /**
     * 创建组群，等待连接
     */
    public void createGroup() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "创建群组成功");
                Toast.makeText(ReceiveCameraActivity.this, "创建群组成功", Toast.LENGTH_SHORT).show();
                wifiUtils.disconnect();
                wifiUtils.setupService();
                wifiUtils.startService(WifiState.DEVICE_ANDROID);
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
