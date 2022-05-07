package com.myapp.Record;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


import com.myapp.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaRecordActivity extends BaseActivity implements View.OnClickListener {


    private MediaProjectionManager mediaProjectionManager;
    private long recordDpi = 6000000;
    private TextView tvTime;
    private Surface surface;
    private MediaRecordService.MyBinder binder;
    private Handler childHandler;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof MediaRecordService.MyBinder) {
                binder = (MediaRecordService.MyBinder) service;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private ImageReader imageReader;


    @Override
    public int setContentView() {
        return R.layout.activity_media_record;
    }

    @Override
    protected void initToolbar() {

    }

    @Override
    public void initView() {

        SurfaceView svRecord = findViewById(R.id.sv_record);
        SurfaceHolder holder = svRecord.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        tvTime = findViewById(R.id.tv_time);

        findViewById(R.id.bt_start).setOnClickListener(this);
        findViewById(R.id.bt_paused).setOnClickListener(this);
        findViewById(R.id.bt_resume).setOnClickListener(this);
        findViewById(R.id.bt_stop).setOnClickListener(this);

    }

    int time = 0;

    private void time() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tvTime.setText(String.valueOf(time));
                time++;
                time();
            }
        }, 1000);
    }

    @Override
    public void initData() {
        HandlerThread handlerThread = new HandlerThread("Recorder");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        setUpImageReader();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10012) {
            Intent intent = new Intent(this, MediaRecordService.class);
            intent.putExtra("data", data);
            intent.putExtra("resultCode", resultCode);
            intent.putExtra("width", WindowUtils.getWindowWidth(this));
            intent.putExtra("height", WindowUtils.getWindowHeight(this));
            intent.putExtra("surface", surface);
            startForegroundService(intent);
            bindService(intent, connection, BIND_AUTO_CREATE);
        }
    }

    @SuppressLint("WrongConstant")
    private void setUpImageReader() {
        imageReader = ImageReader.newInstance(WindowUtils.getWindowWidth(this), WindowUtils.getWindowHeight(this), PixelFormat.RGBA_8888, 10);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                        int a = 45;
                        //imageView.setImageBitmap(bitmapImage);
                    }
                } catch (Exception e) {

                }
            }
        }, childHandler);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start:
                time();
                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(screenCaptureIntent, 10012);
                break;
            case R.id.bt_paused:
                binder.paused();
                break;
            case R.id.bt_resume:
                binder.resume();
                break;
            case R.id.bt_stop:
                binder.stop();
                unbindService(connection);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binder != null) {
            binder.stop();
        }
    }
}
