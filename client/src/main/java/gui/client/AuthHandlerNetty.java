package gui.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import reqs.ServerFinishedTask;
import reqs.ServerRequestNegative;

public class AuthHandlerNetty extends ChannelInboundHandlerAdapter {
    private Controller mc;

    public AuthHandlerNetty(Controller mc) {
        this.mc = mc;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ServerFinishedTask) {
            Platform.runLater(() -> mc.regSuccess());
        }
        if (msg instanceof ServerRequestNegative message) {
            Platform.runLater(() -> mc.regFailure(message.string()));
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
