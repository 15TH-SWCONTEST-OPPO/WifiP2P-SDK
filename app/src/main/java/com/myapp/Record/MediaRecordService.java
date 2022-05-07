package com.myapp.Record;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


import com.myapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaRecordService extends Service {


    private MediaRecorder mediaRecorder;
    private File recordFile;
    private int width;
    private int height;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private MediaCodec videoEncoder;
    private Surface inputSurface;
    private MediaMuxer mediaMuxer;
    private int videoTrackIndex;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isRecord = false;
    private NotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {

        public void paused() {
            // 置为null时,表示暂停
            if(virtualDisplay!=null){
                virtualDisplay.setSurface(null);
            }
        }

        public void stop() {
            isRecord = false;

            virtualDisplay.setSurface(null);
            virtualDisplay.release();
            if(videoEncoder!=null){
                videoEncoder.stop();
                videoEncoder.release();
            }

            if(mediaMuxer!=null){
                mediaMuxer.stop();
                mediaMuxer.release();
            }


            notificationManager.cancel(123123);
        }

        public void resume() {
            if(virtualDisplay!=null){
                virtualDisplay.setSurface(surface);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this, "123123")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("录屏")
                .setContentText(getString(R.string.app_name) + "录屏中")
                .build();

        if (Build.VERSION.SDK_INT >= 26) {
            // 推送通道
            NotificationChannel channel = new NotificationChannel("123123", "通道说明", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        // 展示前台服务
        startForeground(123123, notification);


        int resultCode = intent.getIntExtra("resultCode", -1);
        width = intent.getIntExtra("width", -1);
        height = intent.getIntExtra("height", -1);
        Intent data = intent.getParcelableExtra("data");
        surface = intent.getParcelableExtra("surface");

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection != null) {
            /**
             * 创建投影
             * name 本次虚拟显示的名称
             * width 录制后视频的宽
             * height 录制后视频的高
             * dpi 显示屏像素
             * flags VIRTUAL_DISPLAY_FLAG_PUBLIC 通用显示屏
             * Surface 输出位置
             */
            virtualDisplay = mediaProjection.createVirtualDisplay("record-video", width, height, 6000000,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void readEncoderData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRecord) {
                    // 获取已经解码的缓冲区索引
                    int index = videoEncoder.dequeueOutputBuffer(bufferInfo, 10000);
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // 输出格式已改变
                        resetOutputFormat();
                    } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // 超时

                    } else if (index >= 0) {
                        // 获取数据
                        ByteBuffer outputBuffer = videoEncoder.getOutputBuffer(index);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0;
                        }
                        if (bufferInfo.size == 0) {
                            outputBuffer = null;
                        } else {
                            if (outputBuffer != null) {
                                // 将 ByteBuffer 转换为 byte[]
//                                byte[] bytes = bytebuffer2ByteArray(outputBuffer);

                                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo);
                            }
                        }
                        videoEncoder.releaseOutputBuffer(index, false);
                    }
                }
            }
        }).start();
    }

    /**
     * byteBuffer 转 byte数组
     *
     * @param buffer
     * @return
     */
    public static byte[] bytebuffer2ByteArray(ByteBuffer buffer) {
        //获取buffer中有效大小
        int len = buffer.limit() - buffer.position();
        byte[] bytes = new byte[len];
        buffer.get(bytes);
        return bytes;
    }

    private void initMediaCodec() {
        String MIME_TYPE = "video/avc"; // H.264 类型
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // 颜色格式
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 比特率
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        // 帧速率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
        // I帧的帧率
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        try {
            // 创建指定类型的编码器
            videoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            // 设置编码器属性
            videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 创建作为输入的 Surface
            inputSurface = videoEncoder.createInputSurface();
            videoEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetOutputFormat() {
        MediaFormat newFormat = videoEncoder.getOutputFormat();
        videoTrackIndex = mediaMuxer.addTrack(newFormat);
        mediaMuxer.start();
    }

    private void initMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        // 设置音频来源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置视频来源
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        // 设置输出格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置输出文件
        String absolutePath = new File(recordFile + "/record.mp4").getAbsolutePath();
        mediaRecorder.setOutputFile(absolutePath);
        // 设置视频宽高
        mediaRecorder.setVideoSize(width, height);
        // 设置视频帧率
        mediaRecorder.setVideoFrameRate(60);
        // 设置视频编码比特率
        mediaRecorder.setVideoEncodingBitRate(6000000);
        // 设置音频编码
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // 设置视频编码
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
