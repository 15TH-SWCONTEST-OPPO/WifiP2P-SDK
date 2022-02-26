package wifip2p.wifi.com.wifip2p.activity;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import wifip2p.wifi.com.wifip2p.Constant;
import wifip2p.wifi.com.wifip2p.FileBean;
import wifip2p.wifi.com.wifip2p.R;
import wifip2p.wifi.com.wifip2p.TransBean;
import wifip2p.wifi.com.wifip2p.socket.CameraSocket;
import wifip2p.wifi.com.wifip2p.socket.ReceiveSocket;
import wifip2p.wifi.com.wifip2p.socket.SendSocket;
import wifip2p.wifi.com.wifip2p.utils.FileUtils;
import wifip2p.wifi.com.wifip2p.utils.Md5Util;

/**
 * 发送文件界面
 *
 * 1、搜索设备信息
 * 2、选择设备连接服务端组群信息
 * 3、选择要传输的文件路径
 * 4、把该文件通过socket发送到服务端
 */
public class SendFileActivity extends BaseActivity implements View.OnClickListener, SurfaceHolder.Callback {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        Button mBtnChoseFile = (Button) findViewById(R.id.btn_chosefile);
        Button mBtnConnectServer = (Button) findViewById(R.id.btn_connectserver);
        Button mBtnSendText = (Button) findViewById(R.id.btn_sendtext);
        Button mBtnSendCamera = (Button) findViewById(R.id.btn_sendcamera);
        mTvDevice = (ListView) findViewById(R.id.lv_device);

        mBtnChoseFile.setOnClickListener(this);
        mBtnConnectServer.setOnClickListener(this);
        mBtnSendText.setOnClickListener(this);
        mBtnSendCamera.setOnClickListener(this);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

    }

    @Override
    public void onClick(View v) {
        String hostAddress = "";
        String type = "";
        switch (v.getId()) {
            case R.id.btn_chosefile:
                chooseFile();
                break;
            case R.id.btn_connectserver:
                mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
                mDialog.show();
                mDialog.setCancelable(false);
                mDialog.setContentView(R.layout.loading_progressba);
                //搜索设备
                connectServer();
                break;
            case R.id.btn_sendtext:
                type = Constant.TEXT;
                hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                TransBean transBean = new TransBean(Constant.TEXT,"你浩",new FileBean());
                SendSocket sendSocket = new SendSocket(transBean,hostAddress,null);
                sendSocket.createSendSocket();
                break;
            case R.id.btn_sendcamera:
                type = Constant.CAMERA;
                if (mWifiP2pInfo == null){
                    Toast.makeText(this,"未连接设备",Toast.LENGTH_LONG).show();
                    break;
                }
                if(mWifiP2pInfo.groupOwnerAddress==null){
                    Toast.makeText(this,"未连接设备",Toast.LENGTH_LONG).show();
                    break;
                }
                hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                if(hostAddress==null||hostAddress.equals("")){
                    Toast.makeText(this,"未连接设备",Toast.LENGTH_LONG).show();
                }else{
                    sendVideo(hostAddress);
                }
                break;
            default:
                break;

        }
    }

    /**
     * 搜索设备
     */
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
    private void connect(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig config = new WifiP2pConfig();
        if (wifiP2pDevice != null) {
            config.deviceAddress = wifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "连接成功");
                    Toast.makeText(SendFileActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "连接失败");
                    Toast.makeText(SendFileActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
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
                        if (!file.exists() ) {
                            Toast.makeText(SendFileActivity.this,"文件路径找不到",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (mWifiP2pInfo == null){
                            Toast.makeText(SendFileActivity.this,"连接不存在",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String md5 = Md5Util.getMd5(file);
                        FileBean fileBean = new FileBean(file.getPath(), file.length(), md5);
                        String hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                        new SendTask(SendFileActivity.this, fileBean).execute(hostAddress);
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
        mDialog.dismiss();
        showDeviceInfo();
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
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

    //发送视频 其实也是发送一张一张的图片
    public void sendVideo(String host) {

        CameraSocket cameraSocket = new CameraSocket();
        cameraSocket.init(host);
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                            //mBt.send(imageBytes, "video");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    cameraSocket.send(imageBytes);
                                }
                            }).start();

                        }
                    }
                });
            }
        }).start();
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

}
