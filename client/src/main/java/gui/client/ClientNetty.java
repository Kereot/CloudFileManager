package gui.client;

import gui.client.requests.AuthRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import javafx.event.ActionEvent;

import java.util.*;

public class ClientNetty {

    private static Channel channel;
    private static EventLoopGroup elg;

    public static boolean hasConnected() {
        return hasConnected;
    }

    private static boolean hasConnected;

    private static final int PORT = 45001;

//    public static ServerController sc = new ServerController();
//    public Controller mc;
//
//    public ClientNetty (ServerController sc, Controller mc) {
//        this.sc = sc;
//        this.mc = mc;
//    }

//        public ClientNetty (ServerController sc) {
//        ClientNetty.sc = sc;
//    }


    public void connect(AuthRequest authRequestBuilder, ActionEvent actionEvent, Controller controller) throws InterruptedException {
        new Thread(() -> {
            EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
            try {
                HashMap<String, String> credentials = new HashMap<>();
                credentials.put("type", "auth");
                credentials.put("login", authRequestBuilder.getLogin());
                credentials.put("pass", String.valueOf(authRequestBuilder.getPassword()));
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                socketChannel.pipeline().addLast(
                                        new ObjectDecoder(20000000, ClassResolvers.cacheDisabled(null)),
                                        new ObjectEncoder(),
                                        new ClientHandlerNetty(actionEvent, controller)
                                );
                            }
                        });
                ChannelFuture channelFuture = bootstrap.connect("localhost", PORT).sync();
                channel = channelFuture.channel();
                channel.writeAndFlush(credentials);
                elg = eventLoopGroup;
                hasConnected = true;


                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                eventLoopGroup.shutdownGracefully();
            }
        }).start();
    }

    public static void send(Object obj) {
        channel.writeAndFlush(obj);
        System.out.println("Tried to send smth");
        System.out.println(obj);
        System.out.println(obj.getClass());
    }

    public static void disconnect() {
        try {
            channel.close().sync();
            elg.shutdownGracefully();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
