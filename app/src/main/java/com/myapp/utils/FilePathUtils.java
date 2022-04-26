package com.myapp.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FilePathUtils {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    /**
     * Create a file Uri for saving an image or video
     */
    public static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static String saveBitmap(Bitmap bm) throws Exception {
        Log.d("Save Bitmap", "Ready to save picture");
        FileOutputStream saveImgOut = null;
        File outputMediaFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

        saveImgOut = new FileOutputStream(outputMediaFile);

        FileOutputStream finalSaveImgOut = saveImgOut;
        new Thread(new Runnable() {
            @Override
            public void run() {
                bm.compress(Bitmap.CompressFormat.JPEG, 80, finalSaveImgOut);
                //存储完成后需要清除相关的进程
                try {
                    finalSaveImgOut.flush();
                    finalSaveImgOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("Save Bitmap", "The picture is save to your phone!");
            }
        }).start();
        return outputMediaFile.getAbsolutePath();
    }

    static class FileUtils {
        /**
         * 判断指定目录的文件夹是否存在，如果不存在则需要创建新的文件夹
         * @param fileName 指定目录
         * @return 返回创建结果 TRUE or FALSE
         */
        static boolean fileIsExist(String fileName)
        {
            //传入指定的路径，然后判断路径是否存在
            File file=new File(fileName);
            if (file.exists())
                return true;
            else{
                //file.mkdirs() 创建文件夹的意思
                return file.mkdirs();
            }
        }
    }
}

