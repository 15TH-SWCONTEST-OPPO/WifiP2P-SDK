package wifip2p.wifi.com.wifip2p.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import wifip2p.wifi.com.wifip2p.ProgressDialog;
import wifip2p.wifi.com.wifip2p.R;
import wifip2p.wifi.com.wifip2p.Wifip2pCameraService;
import wifip2p.wifi.com.wifip2p.Wifip2pService;
import wifip2p.wifi.com.wifip2p.utils.WifiUtils;

public class ReceiveCameraActivity extends BaseActivity implements View.OnClickListener{

    private static final String TAG = "ReceiveCameraActivity";
    private Wifip2pCameraService.MyBinder mBinder;
    private ProgressDialog mProgressDialog;
    private Intent mIntent;
    private WifiUtils wifiUtils;
    private ImageView imageView;


    // 创建一个服务连接，与写好的WifiService进行连接
//    private ServiceConnection serviceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            //调用服务里面的方法进行绑定,binder中含有ReceiveSocket
//            mBinder = (Wifip2pCameraService.MyBinder) service;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            //服务断开重新绑定
//            bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        Button btnRemove = (Button) findViewById(R.id.btn_remove);
        btnCreate.setOnClickListener(this);
        btnRemove.setOnClickListener(this);
        wifiUtils = new WifiUtils(this);

        wifiUtils.setupService();
        wifiUtils.startService(true);

        initBlue();
        initView();
    }



    private void initView() {
        imageView = (ImageView) findViewById(R.id.camera_video);
    }

    private void initBlue() {
        wifiUtils.setOnDataReceivedListener(new WifiUtils.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
//                Log.d(TAG,message+"0000000000000000000000000");
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                imageView.setImageBitmap(bitmap);
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

        wifiUtils.setBluetoothConnectionListener(new WifiUtils.WifiConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(ReceiveCameraActivity.this, "蓝牙已连接", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                Toast.makeText(ReceiveCameraActivity.this, "蓝牙已断开", Toast.LENGTH_SHORT).show();
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
        }
    }

    /**
     * 创建组群，等待连接
     */
    public void createGroup() {

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
    protected void onDestroy()  {
        super.onDestroy();
    }

}
