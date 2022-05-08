package com.myapp.qrcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.myapp.qrcode.util.Constant;
import com.myapp.zxing.activity.CaptureActivity;
import com.myapp.FileBean;
import com.myapp.R;
import com.myapp.activity.SendTask;
import com.myapp.utils.FileUtils;
import com.myapp.utils.Md5Util;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private View btnQrCode; // 扫码
    private TextView tvResult; // 结果

    private String serverIP = null;

    private Integer serverPort = null;

//    private String serverIP = "192.168.43.236";
//
//    private Integer serverPort = 35674;

    private boolean isServerEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        /*
         * relative 布局
         * */
        RelativeLayout relativeLayout=(RelativeLayout)findViewById(R.id.relative);


        /*
         * icon图标
         * */
        // 加载字体文件
        Typeface iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");
        // output
        TextView output = (TextView) findViewById(R.id.output);
        output.setTypeface(iconfont);
        // qrcode
        TextView qrcode = (TextView) findViewById(R.id.qrcode);
        qrcode.setTypeface(iconfont);
        initView();
    }

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

    private void initView() {
        btnQrCode = (View) findViewById(R.id.btn_qrcode);
        View btn_choose_file = (View)findViewById(R.id.btn_choose_file);
        btn_choose_file.setOnClickListener(this);
        btnQrCode.setOnClickListener(this);
    }

    // 开始扫码
    private void startQrCode() {
        // 申请相机权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
            return;
        }
        // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQ_PERM_EXTERNAL_STORAGE);
            return;
        }
        // 二维码扫码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, Constant.REQ_QR_CODE);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_qrcode:
                startQrCode();
                break;
            case R.id.btn_choose_file:
                chooseFile();
                break;
        }
    }

    private void chooseFile() {
        // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQ_PERM_EXTERNAL_STORAGE);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
            if(scanResult==null||scanResult.length()<10){
                Toast.makeText(MainActivity.this, "二维码信息不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            //将扫描出的信息显示出来
            String[] pcData = scanResult.split(" ");
            if (pcData.length < 2) {
                Toast.makeText(MainActivity.this, "二维码信息不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean matches = pcData[0].matches("^([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
            int parseInt = Integer.parseInt(pcData[1]);
            boolean check = parseInt>=0&&parseInt<=65530;
            if(!(matches&&check)){
                Toast.makeText(MainActivity.this, "二维码信息不正确", Toast.LENGTH_SHORT).show();
                return;
            }
            serverIP =pcData[0];
            serverPort = parseInt;
            isServerEnable = true;
            Toast.makeText(MainActivity.this, "读取信息成功", Toast.LENGTH_SHORT).show();
            //tvResult.setText(scanResult);
        }
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtils.getAbsolutePath(this, uri);
                    if (path != null) {
                        final File file = new File(path);
                        if (!file.exists()) {
                            Toast.makeText(MainActivity.this, "文件路径找不到", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (serverIP == null || serverPort == null) {
                            Toast.makeText(MainActivity.this, "连接不存在", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(!isServerEnable){
                            Toast.makeText(MainActivity.this,"请检查是否获得信息",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String md5 = Md5Util.getMd5(file);
                        FileBean fileBean = new FileBean(file.getPath(), file.length(), md5);
                        //String hostAddress = mWifiP2pInfo.groupOwnerAddress.getHostAddress();
                        new SendTask(MainActivity.this, fileBean).execute(serverIP, serverPort.toString());
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
            case Constant.REQ_PERM_EXTERNAL_STORAGE:
                // 文件读写权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的文件读写权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


}
