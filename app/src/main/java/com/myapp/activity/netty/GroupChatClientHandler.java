package com.myapp.activity.netty;

import static com.myapp.service.WifiService.ByteArrayToInt;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.myapp.utils.NettyState;

public class GroupChatClientHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final String TAG = "NettyClientHandler";
    private Handler mHandler;
    private byte[] buffer;

    public GroupChatClientHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        Log.d(TAG,"收到消息");
        // 解码当前的Byte数组，然后通过handler更新ui
        decode(channel.remoteAddress().toString(), (byte[]) msg);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        Channel channel = ctx.channel();
        Log.d(TAG,"收到消息");
        // 解码当前的Byte数组，然后通过handler更新ui
        decode(channel.remoteAddress().toString(), msg);
    }

    private void decode(String name, byte[] msg) {
        boolean valid = true;
        int len = msg.length;
        if (len < 10) {
            valid = false;
        }
        for (int i = 0; i < 6; i++) {
            int t = msg[i];
            if (t != i) {
                valid = false;
                break;
            }
        }
        try{
            if (valid) {
                byte[] bufLength = new byte[4];
                for (int i = 0; i < 4; i++) {
                    bufLength[i] = msg[6 + i];
                }

                int textCount = 0;
                int photoCount = 0;
                int videoCount = 0;

                for (int i = 0; i < 4; i++) {
                    int read = msg[10 + i];
                    if (read == 0) {
                        textCount++;
                    } else if (read == 1) {
                        photoCount++;
                    } else if (read == 2) {
                        videoCount++;
                    }
                }
                int length = ByteArrayToInt(bufLength);
                buffer = new byte[length];
                for (int i = 0; i < length; i++) {
                    buffer[i] = msg[14 + i];
                }
                Message message = Message.obtain();
                message.what = NettyState.MESSAGE_READ;
                message.obj = buffer;

                //设置arg1，来确定到底是哪一种数据
                if (textCount == 4) {
                    message.arg1 = 0;
                    Log.d(TAG, "text");
                    mHandler.sendMessage(message);
                } else if (photoCount == 4) {
                    message.arg1 = 1;
                    Log.d(TAG, "photo");
                    mHandler.sendMessage(message);
                } else if (videoCount == 4) {
                    message.arg1 = 2;
                    Log.d(TAG, "video");
                    mHandler.sendMessage(message);
                }
            }
        }catch (Exception e){
            System.out.println(e);
        }
    }
}
