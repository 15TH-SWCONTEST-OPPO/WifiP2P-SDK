package com.myapp.activity.netty;

import static com.myapp.utils.WifiUtils.intToByteArray;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class NettyServer {

    private static final String TAG = "NettyServer";
    private Integer port;
    //编写run方法，处理客户端的请求
    public ChannelFuture channelFuture;

    public Handler mHandler;

    private static int headInfoLength = 14;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    public NettyServer(Integer port, Handler mHandler) {
        this.port = port;
        this.mHandler = mHandler;
    }

    public void run(Handler nettyHandler) {
        //创建两个线程组
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(); //8个NioEventLoop

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //获取到pipeline
                            ChannelPipeline pipeline = ch.pipeline();
                            //向pipeline加入解码器
                            pipeline.addLast("decoder", new ByteArrayDecoder());
                            //向pipeline加入编码器
                            pipeline.addLast("encoder", new ByteArrayEncoder());
                            //加入自己的业务处理handler
                            pipeline.addLast(new GroupChatServerHandler(mHandler));
                        }
                    });

            System.out.println("netty 服务器启动");
            Message message = new Message();
            try {
                channelFuture = b.bind(new InetSocketAddress(port)).sync();
            } catch (InterruptedException interruptedException) {
                message = new Message();
                message.what = 1;
                nettyHandler.sendMessage(message);

                interruptedException.printStackTrace();
            }
            message.what = 0;
            nettyHandler.sendMessage(message);

            //监听关闭
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException interruptedException) {
                message = new Message();
                message.what = 1;
                nettyHandler.sendMessage(message);

                interruptedException.printStackTrace();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void push(String name, byte[] data, String type) {

        Map<String, Channel> channels = GroupChatServerHandler.channels;

        ChannelGroup channelGroup = GroupChatServerHandler.channelGroup;
        channelGroup.forEach(o -> {
            if (o == channels.get(name)) {
                o.writeAndFlush(encode(data, type));
                Log.d(TAG, "发送成功");
            }
        });

    }

    public byte[] encode(byte[] data, String str) {
        int length = data.length;
        byte[] length_b = null;
        try {
            length_b = intToByteArray(length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (length_b == null) return null;

        byte[] headerInfo = new byte[headInfoLength];
        for (int i = 0; i < headInfoLength - 8; i++) {
            headerInfo[i] = (byte) i;
        }

        for (int i = 0; i < 4; i++) {
            headerInfo[6 + i] = length_b[i];
        }

        if (str.equals("text")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 0;
            }
        } else if (str.equals("photo")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 1;
            }
        } else if (str.equals("video")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 2;
            }
        }

        byte[] sendMsg = new byte[length + headInfoLength];
        for (int i = 0; i < sendMsg.length; i++) {
            if (i < headInfoLength) {
                sendMsg[i] = headerInfo[i];
            } else {
                sendMsg[i] = data[i - headInfoLength];
            }
        }
        return sendMsg;
    }

}