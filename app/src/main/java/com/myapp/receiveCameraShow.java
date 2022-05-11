package com.myapp;

import android.util.Log;
import android.widget.ImageView;

public class receiveCameraShow {
    private static final String TAG = "";
    private ImageView video;
    private String deviceName;
    private int status;

    public receiveCameraShow() {
        this.status=0;
    }

    public receiveCameraShow(ImageView v, String name, int s) {
        this.video = v;
        this.deviceName = name;
        this.status = s;
    }

    public void setVideo(ImageView v) {
        this.video = v;
    }

    public ImageView getVideo() {
        return video;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int s){
        this.status=s;
    }

    public void setDeviceName(String name){this.deviceName=name;}

    public String getDeviceName() {
        return deviceName;
    }
}
