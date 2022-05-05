//package com.myapp.oppoapi;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.media.Image;
//import android.os.Environment;
//import android.util.Base64;
//import android.util.Log;
//
//import com.aiunit.core.FrameData;
//import com.aiunit.vision.common.ConnectionCallback;
//import com.aiunit.vision.common.FrameInputSlot;
//import com.aiunit.vision.common.FrameOutputSlot;
//import com.coloros.ocs.ai.cv.CVUnit;
//import com.coloros.ocs.ai.cv.CVUnitClient;
//import com.coloros.ocs.base.common.ConnectionResult;
//import com.coloros.ocs.base.common.api.OnConnectionFailedListener;
//import com.coloros.ocs.base.common.api.OnConnectionSucceedListener;
//import com.myapp.utils.FilePathUtils;
//
//import java.io.File;
//import java.io.FileOutputStream;
//
//public class CVClientUtils {
//
//    private CVUnitClient mCVClient;
//    private Context context;
//
//    public CVClientUtils(Context context) {
//        this.context = context;
//        init(context);
//    }
//
//    private void init(Context context){
//        mCVClient = CVUnit.getVideoStyleTransferDetectorClient
//                (context).addOnConnectionSucceedListener(new OnConnectionSucceedListener() {
//            @Override
//            public void onConnectionSucceed() {
//                Log.i("TAG", " authorize connect: onConnectionSucceed");
//            }
//        }).addOnConnectionFailedListener(new OnConnectionFailedListener() {
//            @Override
//            public void onConnectionFailed(ConnectionResult connectionResult) {
//                Log.e("TAG", " authorize connect: onFailure: " + connectionResult.getErrorCode());
//            }
//        });
//    }
//
//    public void connect2AIUnitServer(){
//        Log.d("TAG","1");
//        mCVClient.initService(context, new ConnectionCallback() {
//            @Override
//            public void onServiceConnect() {
//                Log.i("TAG", "initService: onServiceConnect");
//                int startCode = mCVClient.start();
//                Log.d("TAG",String.valueOf(startCode));
//            }
//
//            @Override
//            public void onServiceDisconnect() {
//                Log.e("TAG", "initService: onServiceDisconnect: ");
//            }
//        });
//        Log.d("TAG","2");
//    }
//
//    public Bitmap runAIUnitServer(Bitmap bitmap) throws Exception {
//        FrameInputSlot inputSlot = (FrameInputSlot) mCVClient.createInputSlot();
//        inputSlot.setTargetBitmap(bitmap);
//        FrameOutputSlot outputSlot = (FrameOutputSlot) mCVClient.createOutputSlot();
//
//        int code = mCVClient.process(inputSlot, outputSlot);
//        Log.d("TAG",String.valueOf(code));
//        FrameData frameData = outputSlot.getOutFrameData();
//        if(frameData==null) {
//            throw new Exception();
//        }
//        byte[] outImageBuffer = frameData.getData();
//        try {
//            File file = FilePathUtils.getOutputMediaFile(FilePathUtils.MEDIA_TYPE_IMAGE);
//           // File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/facenew1.png");
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(outImageBuffer, 0, outImageBuffer.length);
//            fos.flush();
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Bitmap array = BitmapFactory.decodeByteArray(outImageBuffer, 0, outImageBuffer.length);
//        return array;
//        //return outImageBuffer;
//    }
//
//    public void releaseAIUnitServer(){
//        if (mCVClient != null) {
//            mCVClient.stop();
//            mCVClient.releaseService();
//            mCVClient = null;
//        }
//    }
//}
