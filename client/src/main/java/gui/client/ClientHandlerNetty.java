package gui.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import reqs.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

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
//        if (msg instanceof FileTransferCheckSize message) {
//            Platform.runLater(() -> {
//                if (mc.checkLocalFreeSpace(message.fileSize())) {
//                    ClientNetty.send(new FileTransferReply("OK"));
//                    System.out.println("Sent positive reply");
//                } else {
//                    ClientNetty.send(new FileTransferReply("NO"));
//                    System.out.println("Sent negative reply");
//                }
//            });
//        }
//        if (msg instanceof ChunkedInput message) {
//            Platform.runLater(() -> {
//                mc.copyFileFromServer(message);
//            });
//        }
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
        if (msg instanceof FileWrittenToServer) {
            Platform.runLater(() -> mc.updateServerListAfterFileAcquisition());
        }
//        ctx.close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

//    private void sendFile(ChannelHandlerContext ctx, Path pathToSend, Path pathToPlace) {
//        System.out.println("Tried to send a file");
//
//        try {
//            ctx.writeAndFlush(new FilePlacementOnServer(pathToPlace.toString()));
//            byte[] file = Files.readAllBytes(pathToSend);
//            final int CHUNK = 19000000;
//            if (file.length <= CHUNK) {
//                ctx.writeAndFlush(file);
//            } else {
//                final int PARTS = file.length / CHUNK;
//                final int LAST_PART = file.length - (CHUNK * PARTS);
//                int start = 0;
//                int end = CHUNK;
//                for (int i = 0; i < PARTS; i++) {
//                    if (i == 0) {
//                        ctx.writeAndFlush(new FileFirstChunk(Arrays.copyOfRange(file, start, end)));
//                        start += CHUNK;
//                        end = start + CHUNK;
//                    }
//                    if (i != PARTS - 1) {
//                        ctx.writeAndFlush(new FileChunk(Arrays.copyOfRange(file, start, end)));
//                        start += CHUNK;
//                        if (i == PARTS -2) {
//                            end = start + LAST_PART;
//                        } else {
//                            end = start + CHUNK;
//                        }
//                    } else {
//                        ctx.writeAndFlush(new FileLastChunk(Arrays.copyOfRange(file, start, end)));
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
