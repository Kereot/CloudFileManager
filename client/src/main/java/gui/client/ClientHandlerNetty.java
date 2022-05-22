package gui.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import reqs.*;

import java.nio.file.Paths;

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
        System.out.println("Got that: " + msg.getClass());
        if (msg instanceof Auth message) {
            Platform.runLater(() -> mc.buildMainScene(actionEvent, message.authList()));
        }
        if (msg instanceof DirInfo message) {
            Platform.runLater(() -> mc.updateServerList(message.dirInfoList()));
        }
        if (msg instanceof PassServerPath message) {
            Platform.runLater(() -> mc.setServerPath(message.currentServerPath()));
        }
        if (msg instanceof byte[] message) {
            Platform.runLater(() -> mc.copySmallFileFromServer(message));
        }
        if (msg instanceof FileFirstChunk message) {
            Platform.runLater(() -> mc.copyLargeFileFromServerStart(message));
        }
        if (msg instanceof FileChunk message) {
            Platform.runLater(() -> mc.copyLargeFileFromServerMiddle(message));
        }
        if (msg instanceof FileLastChunk message) {
            Platform.runLater(() -> mc.copyLargeFileFromServerEnd(message));
        }
        if (msg instanceof FilePushReply message) {
            switch (message.string()) {
                case "OK" ->
                        Platform.runLater(() ->
                                mc.sendFileToServer(ctx, Paths.get(message.fileToSend()), Paths.get(message.fileToPlace())));
                case "EXISTS" ->
                        Platform.runLater(() ->
                                mc.sendFileToServerAfterConfirmation(ctx, Paths.get(message.fileToSend()), Paths.get(message.fileToPlace())));
            }
        }
        if (msg instanceof ServerFinishedTask) {
            Platform.runLater(() -> mc.updateServerListAfterSomeAction());
        }

        if (msg instanceof CreateFolderNegative message){
            Platform.runLater(() -> mc.failedToCreateServerFolder(message.string()));
        }
//        ctx.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
