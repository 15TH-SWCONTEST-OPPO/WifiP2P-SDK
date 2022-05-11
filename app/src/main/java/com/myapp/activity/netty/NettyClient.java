package com.myapp.activity.netty;

import static com.myapp.utils.WifiUtils.intToByteArray;

import android.os.Handler;
import android.util.Log;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;


public class NettyClient {
    public ChannelFuture channelFuture;

    public EventLoopGroup group;
    private static int headInfoLength = 14;
    private Handler mHandler;

    public NettyClient(Handler handler) {
        this.mHandler = handler;
    }

    public void run(String host, int port, String deviceName) throws InterruptedException {
        group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //得到pipeline
                            ChannelPipeline pipeline = ch.pipeline();
                            //加入相关handler
                            pipeline.addLast("decoder", new ByteArrayDecoder());
                            pipeline.addLast("encoder", new ByteArrayEncoder());
                            //加入自定义的handler

                            pipeline.addLast(new GroupChatClientHandler(mHandler));
                        }
                    });

            channelFuture = bootstrap.connect(host, port).sync();
            //得到channel
            Channel channel = channelFuture.channel();
            System.out.println("-------" + channel.localAddress() + "--------");
            channel.writeAndFlush("你好");
            send(deviceName.getBytes(), "address");
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    public void send(byte[] bytes, String type) {
        Channel channel = channelFuture.channel();
        byte[] encode = encode(bytes, type);

        if (encode != null) {
            Log.d("netty", "发送消息");
            channel.writeAndFlush(encode);
        }
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
        } else if (str.equals("address")) {
            for (int i = 0; i < 4; i++) {
                headerInfo[10 + i] = (byte) 3;
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

    public void close() {
        if (group == null) {
            return;
        }
        group.shutdownGracefully();
    }
}
