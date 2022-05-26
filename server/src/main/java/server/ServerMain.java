package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMain {

    private final int PORT = 45001;
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        new ServerMain().run();
    }

    private void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline inbound = socketChannel.pipeline();
                            inbound.addLast(
                                    new ObjectDecoder(20000000, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new ServerHandler()
                            );
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();
            LOGGER.info("Server started!");
            DBHandler.connect();
            LOGGER.info("DB connected!");
//            channelFuture.channel().closeFuture().sync(); // I'm not sure if it's needed.
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "EXCEPTION!", e);
            e.printStackTrace();
        }
//        finally { // I'm not sure when it should be closed if at all.
//            DBHandler.disconnect();
//            workerGroup.shutdownGracefully();
//            bossGroup.shutdownGracefully();
//        }
    }
}
