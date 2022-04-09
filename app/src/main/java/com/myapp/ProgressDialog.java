package com.myapp;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.myapp.utils.ScreenUtils;

import com.myapp.R;

/**
 * date：2018/3/2 on 15:11
 * description: 自定义发送和接收的进度条
 */

public class ProgressDialog extends AlertDialog.Builder {

    private TextView mProgressText;
    private ProgressBar mProgress;

    private AlertDialog mDialog;
    private View mView;

    public ProgressDialog(@NonNull Context context) {
        super(context);
        init();
    }

    public ProgressDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {

        mDialog = this.create();
        WindowManager.LayoutParams params = mDialog.getWindow().getAttributes();
        params.height = ScreenUtils.dip2px(getContext(), 130);
        params.gravity = Gravity.CENTER;
        mDialog.getWindow().setAttributes(params);

        mView = LayoutInflater.from(getContext()).inflate(R.layout.progressbar, null);
        mProgressText = mView.findViewById(R.id.tv_text);
        mProgress = mView.findViewById(R.id.pb_process);
        mDialog.show();
        mDialog.setContentView(mView);
        mDialog.setCancelable(false);
    }

    public void setProgressText(String text){
        mProgressText.setText(text);
    }

    public void setProgress(int progress){
        mProgress.setProgress(progress);
    }

    public void dismiss(){
        mDialog.dismiss();
    }

}
