package com.myapp.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import com.myapp.R;

/**
 * Wifi P2P 技术并不会访问网络，但会使用到 Java socket 技术
 *
 * 总结：
 * 1、声明权限
 * 1、清单文件注册权限
 * 2、注册Wifi P2P相关广播
 * 3、创建客户端socket，把选择的文件解析成IO流，发送信息
 * 4、创建服务端server，在server内创建服务端socket，监听客户端socket端口，获取信息
 * 5、服务端创建连接的组群信息提供给客户端连接
 * 7、客户端连接信息组群和服务端建立WiFip2p连接
 * 8、客户端通过socket发送文件到服务端serversocket服务端监听到端口后就会获取信息，写入文件。
 */
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //申请文件读写权限
        requireSomePermission();
        /*
        * relative 布局
        * */
        RelativeLayout relativeLayout=(RelativeLayout)findViewById(R.id.relative);


        /*
         * icon图标
         * */
        // 加载字体文件
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");
        // client
        TextView client = (TextView) findViewById(R.id.client);
        client.setTypeface(iconfont);
        // server
        TextView server = (TextView) findViewById(R.id.server);
        server.setTypeface(iconfont);
        // input
        TextView input = (TextView) findViewById(R.id.input);
        input.setTypeface(iconfont);
        // output
        TextView output = (TextView) findViewById(R.id.output);
        output.setTypeface(iconfont);
        // qrcode
        TextView qrcode = (TextView) findViewById(R.id.qrcode);
        qrcode.setTypeface(iconfont);

        // record
        TextView record = (TextView) findViewById(R.id.record);
        record.setTypeface(iconfont);
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

    public void sendFile(View v) {
        startActivity(new Intent(this,SendFileActivity.class));
    }

    public void receiveFile(View v) {
        startActivity(new Intent(this,ReceiveFileActivity.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void sendCamera(View v){
        startActivity(new Intent(this,SendCameraActivity.class));
    }

    public void receiveCamera(View v){
        startActivity(new Intent(this,ReceiveCameraActivity.class));
    }

    public void scanCode(View v){
        startActivity(new Intent(this, com.myapp.qrcode.MainActivity.class));
    }

    public void record(View view) {
        startActivity(new Intent(this, com.myapp.sendclient.MainActivity.class));
    }

    //申请权限
    @AfterPermissionGranted(1000)
    private void requireSomePermission() {
        String[] perms = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            //有权限
        } else {
            //没权限
            EasyPermissions.requestPermissions(this, "需要文件读取权限",
                    1000, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 权限申成功
     * @param i
     * @param list
     */
    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {
        Log.e(TAG,"权限申成功");
    }

    /**
     * 权限申请失败
     * @param i
     * @param list
     */
    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        Log.e(TAG,"权限申请失败");
    }


}
