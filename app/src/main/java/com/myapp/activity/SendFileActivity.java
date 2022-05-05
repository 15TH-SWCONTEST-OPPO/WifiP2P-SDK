package com.myapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.NFC.activity.ReadingWritingActivity;
import com.myapp.Constant;
import com.myapp.FileBean;
import com.myapp.R;
import com.myapp.utils.FileUtils;
import com.myapp.utils.FormatUtils;
import com.myapp.utils.Md5Util;

/**
 * 发送文件界面
 * <p>
 * 1、搜索设备信息
 * 2、选择设备连接服务端组群信息
 * 3、选择要传输的文件路径
 * 4、把该文件通过socket发送到服务端
 */
public class SendFileActivity extends BaseActivity implements View.OnClickListener {

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

    //  NfcAdapter
    private NfcAdapter mNfcAdapter;

    private String ip = "";
    private String deviceName = "";
    private boolean needConnect = false;
    private Collection<WifiP2pDevice> mWifiP2pDeviceList;
    private int connectTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        Button mBtnChoseFile = (Button) findViewById(R.id.file_btn_chosefile);
        Button mBtnConnectServer = (Button) findViewById(R.id.file_btn_connectserver);
        Button mBtnCancelConnect = (Button) findViewById(R.id.file_btn_cancelconnect);
        Button btn_nfc = findViewById(R.id.file_btn_nfc);
        mTvDevice = (ListView) findViewById(R.id.lv_device);

        mBtnChoseFile.setOnClickListener(this);
        mBtnConnectServer.setOnClickListener(this);
        mBtnCancelConnect.setOnClickListener(this);
        btn_nfc.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        String hostAddress = "";
        String type = "";
        switch (v.getId()) {
            case R.id.file_btn_chosefile:
                chooseFile();
                break;
            case R.id.file_btn_connectserver:
                cancelConnect(false);
                mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
                mDialog.show();
                mDialog.setCancelable(false);
                mDialog.setContentView(R.layout.loading_progressba);
                //搜索设备
                connectServer();
                break;
            case R.id.file_btn_cancelconnect:
                cancelConnect(true);
                break;
            case R.id.file_btn_nfc:
                readIPFromNFC();

                break;
            default:
                break;

        }
    }

    /**
     * 搜索设备
     */
    private void connectServer() {
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
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "搜索设备成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "搜索设备失败");
                if (mDialog != null) {
                    mDialog.dismiss();
                }
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
                            Toast.makeText(SendFileActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        } else {
                            connectTime++;
                            connect(wifiP2pDevice);
                        }
                    } else {
                        Toast.makeText(SendFileActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    if (connectTime >= 2) {
                        connectTime = 0;
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        //Toast.makeText(SendCameraActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }

                    Toast.makeText(SendFileActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });


        }
    }


    private void cancelConnect(boolean isShow) {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                if(isShow) {
                    Toast.makeText(SendFileActivity.this, "移除成功", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "取消成功");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (isShow){
                    Toast.makeText(SendFileActivity.this, "移除失败", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "取消失败");
            }
        });
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

    private void readIPFromNFC() {
        Intent intent = new Intent(this, ReadingWritingActivity.class);
        startActivityForResult(intent, 20);
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
                            Toast.makeText(SendFileActivity.this, "文件路径找不到", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mWifiP2pInfo == null) {
                            Toast.makeText(SendFileActivity.this, "连接不存在", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String md5 = Md5Util.getMd5(file);
                        FileBean fileBean = new FileBean(file.getPath(), file.length(), md5);
                        String hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                        new SendTask(SendFileActivity.this, fileBean).execute(hostAddress, Constant.sendPort.toString());
                    }
                }
            }
        } else if (requestCode == 20) {
            if (resultCode == RESULT_OK) {
                deviceName = data.getStringExtra("NAME");
                needConnect = true;
                //showDevice = false;
                for (WifiP2pDevice device:mWifiP2pDeviceList){
                    if(device.deviceName.equals(deviceName)){
                        connect(device);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);
        mWifiP2pDeviceList = wifiP2pDeviceList;
        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (!mListDeviceName.contains(device.deviceName) && !mListDevice.contains(device)) {
                mListDeviceName.add("设备：" + device.deviceName + "----" + device.deviceAddress);
                mListDevice.add(device);
            }
        }

        //进度条消失
        if(mDialog!=null) {
            mDialog.dismiss();
        }
        showDeviceInfo();
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelConnect(false);
    }
}
