package gui.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import reqs.Auth;
import reqs.DirInfo;
import reqs.PassServerPath;

public class ClientHandlerNetty extends ChannelInboundHandlerAdapter {

    private Controller mc;
    private ActionEvent actionEvent;

//    public ClientHandlerNetty(ActionEvent actionEvent) {
//        ClientHandlerNetty.actionEvent = actionEvent;
//    }

    public ClientHandlerNetty(ActionEvent actionEvent, Controller mc) {
        this.actionEvent = actionEvent;
        this.mc = mc;
    }

//    public ClientHandlerNetty(ActionEvent actionEvent, ServerController sc) {
//        this.actionEvent = actionEvent;
//        this.sc = sc;
//    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Auth message) {
                Platform.runLater(() -> {
                    mc.buildMainScene(actionEvent, message.authList());
//                    Controller.buildMainScene(actionEvent, message.authList());
                });
        }
        if (msg instanceof DirInfo message) {
            Platform.runLater(() -> {
               mc.updateList(message.dirInfoList());
            });
        }
        if (msg instanceof PassServerPath message) {
            Platform.runLater(() -> {
                mc.setServerPath(message.currentServerPath());
            });
        }
//        ctx.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
