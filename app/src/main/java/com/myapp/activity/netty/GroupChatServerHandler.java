package com.myapp.activity.netty;

import static com.myapp.service.WifiService.ByteArrayToInt;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import com.myapp.utils.NettyState;

import java.util.HashMap;
import java.util.Map;

public class GroupChatServerHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final String TAG = "NettyServerHandler";

    //public static List<Channel> channels = new ArrayList<Channel>();

    //使用一个hashmap 管理
    public static Map<String, Channel> channels = new HashMap<String, Channel>();
    //定义一个channle 组，管理所有的channel
    //GlobalEventExecutor.INSTANCE) 是全局的事件执行器，是一个单例
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private Handler mHandler;

    public GroupChatServerHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private byte[] buffer;

    //handlerAdded 表示连接建立，一旦连接，第一个被执行
    //将当前channel 加入到  channelGroup
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        //将该客户加入聊天的信息推送给其它在线的客户端
        /*
        该方法会将 channelGroup 中所有的channel 遍历，并发送 消息，
        我们不需要自己遍历
         */
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 加入聊天" + " \n");
        channelGroup.add(channel);
    }

    //断开连接, 将xx客户离开信息推送给当前在线的客户
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 离开了\n");
        System.out.println("channelGroup size" + channelGroup.size());
    }

    //表示channel 处于活动状态, 提示 xx上线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.put(ctx.channel().remoteAddress().toString(), ctx.channel());

        Message msg = new Message();
        msg.what = NettyState.DEVICE_CONNECTED;
        Bundle bundle = new Bundle();
        bundle.putString("name",ctx.channel().remoteAddress().toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        System.out.println(ctx.channel().remoteAddress() + " 上线了~");
    }

    //表示channel 处于不活动状态, 提示 xx离线了
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel().remoteAddress().toString());

        Message msg = new Message();
        msg.what = NettyState.DEVICE_DISCONNECTED;
        Bundle bundle = new Bundle();
        bundle.putString("name",ctx.channel().remoteAddress().toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        System.out.println(ctx.channel().remoteAddress() + " 离线了~");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //关闭通道
        ctx.close();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        // 获取到当前channel
        Channel channel = ctx.channel();
        Log.d(TAG,"收到消息");
        // 解码当前的Byte数组，然后通过handler更新ui
        decode(channel.remoteAddress().toString(),msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //super.channelRead(ctx, msg);
        Channel channel = ctx.channel();
        Log.d(TAG,"收到消息");
        // 解码当前的Byte数组，然后通过handler更新ui
        decode(channel.remoteAddress().toString(), (byte[]) msg);
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
                //传输姓名参数，来确定更新哪一个ui
                Bundle bundle = new Bundle();
                bundle.putString("name",name);
                message.setData(bundle);
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
