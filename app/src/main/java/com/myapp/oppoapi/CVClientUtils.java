package com.myapp.oppoapi;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.aiunit.core.FrameData;
import com.aiunit.vision.common.ConnectionCallback;
import com.aiunit.vision.common.FrameInputSlot;
import com.aiunit.vision.common.FrameOutputSlot;
import com.coloros.ocs.ai.cv.CVUnit;
import com.coloros.ocs.ai.cv.CVUnitClient;
import com.coloros.ocs.base.common.ConnectionResult;
import com.coloros.ocs.base.common.api.OnConnectionFailedListener;
import com.coloros.ocs.base.common.api.OnConnectionSucceedListener;

public class CVClientUtils {

    private CVUnitClient mCVClient;
    private Context context;

    public CVClientUtils(Context context) {
        this.context = context;
        init(context);
        connect2AIUnitServer();
    }

    private void init(Context context){
        mCVClient = CVUnit.getVideoStyleTransferDetectorClient
                (context).addOnConnectionSucceedListener(new OnConnectionSucceedListener() {
            @Override
            public void onConnectionSucceed() {
                Log.i("TAG", " authorize connect: onConnectionSucceed");
            }
        }).addOnConnectionFailedListener(new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.e("TAG", " authorize connect: onFailure: " + connectionResult.getErrorCode());
            }
        });
    }

    public void connect2AIUnitServer(){
        Log.d("TAG","1");
        mCVClient.initService(context, new ConnectionCallback() {
            @Override
            public void onServiceConnect() {
                Log.i("TAG", "initService: onServiceConnect");
                int startCode = mCVClient.start();
                Log.d("TAG",String.valueOf(startCode));
            }

            @Override
            public void onServiceDisconnect() {
                Log.e("TAG", "initService: onServiceDisconnect: ");
            }
        });
        Log.d("TAG","2");
    }

    public byte[] runAIUnitServer(Bitmap bitmap){
        FrameInputSlot inputSlot = (FrameInputSlot) mCVClient.createInputSlot();
        inputSlot.setTargetBitmap(bitmap);
        FrameOutputSlot outputSlot = (FrameOutputSlot) mCVClient.createOutputSlot();

        int code = mCVClient.process(inputSlot, outputSlot);
        Log.d("TAG",String.valueOf(code));
        FrameData frameData = outputSlot.getOutFrameData();

        byte[] outImageBuffer = frameData.getData();
        return outImageBuffer;
    }

    public void releaseAIUnitServer(){
        if (mCVClient != null) {
            mCVClient.stop();
            mCVClient.releaseService();
            mCVClient = null;
        }
    }
}
