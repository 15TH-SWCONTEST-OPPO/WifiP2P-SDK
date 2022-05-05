package com.myapp.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Administrator on 2019/7/26
 * <p>
 * desc:
 */
public class NFCUtils {
	public static NfcAdapter mNfcAdapter;
    public static IntentFilter[] mIntentFilter = null;
    public static PendingIntent mPendingIntent = null;
    public static String[][] mTechList = null;
 
 
	/**
     * 构造函数，用于初始化nfc
     */
    public NFCUtils(Activity activity) {
        if(!isNfcEnable(activity)){
            return;
        }
        NfcInit(activity);
    }
    /**
     * 判断手机是否具备NFC功能
     *
     * @param context {@link Context}
     * @return {@code true}: 具备 {@code false}: 不具备
     */
    public static boolean isNfcExits(Context context) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        return nfcAdapter != null;
    }
 
    /**
     * 判断手机NFC是否开启
     * <p>
     *     OPPO A37m 发现必须同时开启NFC以及Android Beam才可以使用
     *     20180108 发现OPPO单独打开NFC即可读取标签，不清楚是否是系统更新
     * </p>
     *
     * @param context {@link Context}
     * @return {@code true}: 已开启 {@code false}: 未开启
     */
    public static boolean isNfcEnable(Context context) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        mNfcAdapter = nfcAdapter;
//        if (Build.MANUFACTURER.toUpperCase().contains("OPPO")) {
//            return nfcAdapter.isEnabled() && isAndroidBeamEnable(context);
//        }
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }
 
    /**
     * 判断手机NFC的Android Beam是否开启，在API 16之后才有
     *
     * @param context {@link Context}
     * @return {@code true}: 已开启 {@code false}: 未开启
     */
    public static boolean isAndroidBeamEnable(Context context) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && nfcAdapter != null && nfcAdapter.isNdefPushEnabled();
    }
 
    /**
     * 判断手机是否具备Android Beam
     *
     * @param context {@link Context}
     * @return {@code true}:具备 {@code false}:不具备
     */
    public static boolean isAndroidBeamExits(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && isNfcExits(context);
    }
 
    /**
     * 跳转至系统NFC设置界面.
     *
     * @param context {@link Context}
     * @return {@code true} 跳转成功 <br> {@code false} 跳转失败
     */
    public static boolean intentToNfcSetting(Context context) {
        if (isNfcExits(context)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                return toIntent(context, Settings.ACTION_NFC_SETTINGS);
            }
        }
        return false;
 
    }
 
    /**
     * 跳转至系统NFC Android Beam设置界面，同页面基本都有NFC开关.
     *
     * @param context {@link Context}
     * @return {@code true} 跳转成功 <br> {@code false} 跳转失败
     */
    public static boolean intentToNfcShare(Context context) {
        if (isAndroidBeamExits(context)) {
            return toIntent(context, Settings.ACTION_NFCSHARING_SETTINGS);
        }
        return false;
    }
 
    /**
     * 跳转方法.
     * @param context {@link Context}
     * @param action 意图
     * @return 是否跳转成功 {@code true } 成功<br>{@code false}失败
     */
    private static boolean toIntent(Context context, String action) {
        try {
            Intent intent = new Intent(action);
            context.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
 
    /**
     * 初始化nfc设置
     */
    public static void NfcInit(Activity activity) {
		//方法一：
        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        //做一个IntentFilter过滤你想要的action 这里过滤的是ndef
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        //如果你对action的定义有更高的要求，比如data的要求，你可以使用如下的代码来定义intentFilter
        //        IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        //        try {
        //            filter.addDataType("*/*");
        //        } catch (IntentFilter.MalformedMimeTypeException e) {
        //            e.printStackTrace();
        //        }
        //        mIntentFilter = new IntentFilter[]{filter, filter2};
        //        mTechList = null;
        try {
            filter.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        mTechList = new String[][]{{MifareClassic.class.getName()},
                {NfcA.class.getName()}};
        //生成intentFilter
        mIntentFilter = new IntentFilter[]{filter};
//		//方法二：
//		mPendingIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
//        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
//        IntentFilter filter2 = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
//        try {
//            filter.addDataType("*/*");
//        } catch (IntentFilter.MalformedMimeTypeException e) {
//            e.printStackTrace();
//        }
//        mIntentFilter = new IntentFilter[]{filter, filter2};
//        mTechList = null;
    }
 
 
    /**
     * 读取NFC的数据
     */
    public static String readNFCFromTag(Intent intent) throws UnsupportedEncodingException {
        Parcelable[] rawArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawArray != null) {
            NdefMessage mNdefMsg = (NdefMessage) rawArray[0];
            NdefRecord mNdefRecord = mNdefMsg.getRecords()[0];
            if (mNdefRecord != null) {
                String readResult = new String(mNdefRecord.getPayload(), "UTF-8");
                return readResult;
            }
        }
        return "";
    }
 
 
    /**
     * 往nfc写入数据
     */
    public static void writeNFCToTag(String data, Intent intent) throws IOException, FormatException {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        NdefRecord ndefRecord = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ndefRecord = NdefRecord.createTextRecord(null, data);
        }
        NdefRecord[] records = {ndefRecord};
        NdefMessage ndefMessage = new NdefMessage(records);
        ndef.writeNdefMessage(ndefMessage);
    }
 
    /**
     * 读取nfcID
     */
    public static String readNFCId(Intent intent) throws UnsupportedEncodingException {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String id = ByteArrayToHexString(tag.getId());
        return id;
    }
 
    /**
     * 将字节数组转换为字符串
     */
    private static String ByteArrayToHexString(byte[] inarray) {
        int i, j, in;
        String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String out = "";
 
        for (j = 0; j < inarray.length; ++j) {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }
}