package com.zzq.core.test;

import com.zzq.core.test.bean.RpcRequest;
import com.zzq.core.test.bean.RpcRequestParams;
import com.zzq.core.test.bean.User;
import com.zzq.core.test.handler.RpcProxyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Zhou Zhong Qing
 * @Title: ${file_name}
 * @Package ${package_name}
 * @Description: ${todo}
 * @date 2019/8/614:56
 */
public class NettyClient {

    public static void main(String[] args) {


        RpcRequestParams request = new RpcRequestParams();
        request.setName("zhangsan");
        User user = new User();
        user.setId(1);
        user.setName("user zhangsan");
        request.setUser(user);

        request.setParams(new HashMap<String, String>() {{
            put("param1", "test");
        }});
        request.setUserList(new ArrayList<User>() {{
            add(user);
        }});

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setParams(new Object[]{request});
        rpcRequest.setTypes(new Class[]{request.getClass()});
        rpcRequest.setClassName(NettyClient.class.getName());
        rpcRequest.setMethodName("method");
        //socket netty连
        final RpcProxyHandler rpcProxyHandler = new RpcProxyHandler();
        String[] address = "127.0.0.1:8080".split(":");
        String host = address[0];
        int port = Integer.parseInt(address[1]);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            //pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                            //        Integer.MAX_VALUE, 0, 4, 0, 4
                            //));
                            //pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE
                                    , ClassResolvers.cacheDisabled(null)
                            ));

                            pipeline.addLast(rpcProxyHandler);
                        }
                    });
            //System.out.println("host " + host + " prot " + port);
            ChannelFuture future = b.connect(host, port).sync();
            future.channel().writeAndFlush(rpcRequest);
            long startTime = System.currentTimeMillis();
            future.channel().closeFuture().sync();
            long endTime = System.currentTimeMillis();

            System.out.println("本次消耗 [ " + ((endTime - startTime) / 1000) + " ]秒");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }

    }
}
