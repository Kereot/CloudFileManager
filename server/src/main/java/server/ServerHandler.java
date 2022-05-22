package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.io.FileUtils;
import reqs.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;


public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    private static final Path PATH = Paths.get("storage");
    private Path rootPath;
    private Path targetPath;
    private Path fileBeingReceived = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Got smth");
        if (msg instanceof HashMap<?,?> message) {
            String type = (String) message.get("type");
            switch (type) {
                case "auth" -> authentication(ctx, message);
                case "dir" -> updateList(ctx, message);
            }
        }
        if (msg instanceof FileTransferRequest message) {
            sendFile(ctx, message);
        }
        if (msg instanceof FilePushRequest message) {
            checkBeforeFileAcquisition(ctx, message);
        }
        if (msg instanceof FilePlacementOnServer message) {
            fileBeingReceived = Paths.get(message.string());
        }
        if (msg instanceof byte[] message) {
            copySmallFileFromClient(ctx, message);
        }
        if (msg instanceof FileFirstChunk message) {
            copyLargeFileFromClientStart(message);
        }
        if (msg instanceof FileChunk message) {
            copyLargeFileFromClientMiddle(message);
        }
        if (msg instanceof FileLastChunk message) {
            copyLargeFileFromClientEnd(ctx, message);
        }
        if (msg instanceof CreateFolderRequest message) {
            createFolder(ctx, message);
        }
        if (msg instanceof DeleteObjectRequest message) {
            String type = message.type();
            switch (type) {
                case "file" -> deleteServerFile(ctx, message);
                case "dir" -> deleteServerDir(ctx, message);
            }
        }

        LOGGER.warning("This request received: " + msg.getClass());

//        ctx.close();
    }

    private void authentication(ChannelHandlerContext ctx, HashMap<?,?> message) throws Exception {
        String login = (String) message.get("login");
        String password = (String) message.get("pass");
        PreparedStatement psUser = DBHandler.getConnection().prepareStatement("SELECT login FROM clients WHERE login = ? AND password = ?;");
        psUser.setString(1, login);
        psUser.setString(2, password);
        ResultSet rs = psUser.executeQuery();
        if (rs.next()) {
            Channel currentChannel = ctx.channel();
            LOGGER.info("Client authenticated.");
            rootPath = PATH.resolve(login);
            if (Files.notExists(rootPath)) {
                Files.createDirectory(rootPath);
            }
            currentChannel.writeAndFlush(serverList(rootPath));
            currentChannel.writeAndFlush(new PassServerPath(login));
            LOGGER.info("Files list sent to client.");
            System.out.println(serverList(rootPath));
            System.out.println(serverList(rootPath).getClass());
        } else {
            System.out.println("Wrong login or password");
            System.out.println(message.get("login") + " " + message.get("pass"));
        }
    }

    private Auth serverList(Path path) throws IOException {
        return new Auth(Files.list(path).map(Path::toFile).toList());
    }

    private void updateList(ChannelHandlerContext ctx, HashMap<?,?> message) throws IOException {
        System.out.println("Tried to update list");
        targetPath = PATH.resolve(Paths.get((String) message.get("path")));
        if (Files.isDirectory(targetPath)) {
            Channel currentChannel = ctx.channel();
            currentChannel.writeAndFlush(folderServerList(targetPath));
            currentChannel.writeAndFlush(new PassServerPath((String) message.get("path")));
            LOGGER.info("Files list sent to client.");
        } else {
            LOGGER.info("A file was double clicked.");
        }
    }

    private DirInfo folderServerList(Path path) throws IOException {
        return new DirInfo(Files.list(path).map(Path::toFile).toList());
    }

    private void sendFile(ChannelHandlerContext ctx, FileTransferRequest message) {
        System.out.println("Tried to send a file");
        Path path = Paths.get(PATH.resolve(message.path()).resolve(message.fileName()).toString());

        try {
            byte[] file = Files.readAllBytes(path);
            System.out.println(path);
            System.out.println(file.getClass() + " " + file.length);
            final int CHUNK = 19000000;
            if (file.length <= CHUNK) {
                ctx.writeAndFlush(file);
            } else {
            final int PARTS = file.length / CHUNK;
            final int LAST_PART = file.length - (CHUNK * PARTS);
            int start = 0;
            int end = CHUNK;
                for (int i = 0; i < PARTS; i++) {
                    if (i == 0) {
                        ctx.writeAndFlush(new FileFirstChunk(Arrays.copyOfRange(file, start, end)));
                        start += CHUNK;
                        end = start + CHUNK;
                    }
                    if (i != PARTS - 1) {
                        ctx.writeAndFlush(new FileChunk(Arrays.copyOfRange(file, start, end)));
                        start += CHUNK;
                        if (i == PARTS -2) {
                            end = start + LAST_PART;
                        } else {
                            end = start + CHUNK;
                        }
                    } else {
                        ctx.writeAndFlush(new FileLastChunk(Arrays.copyOfRange(file, start, end)));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkBeforeFileAcquisition(ChannelHandlerContext ctx, FilePushRequest message) {
        Path fileOnServerSide = PATH.toAbsolutePath().resolve(Paths.get(message.pathServer())).resolve(Paths.get(message.fileName()));
        System.out.println(fileOnServerSide);
        Path fileToSend = Paths.get(message.pathClient()).resolve(Paths.get(message.fileName()));
        System.out.println(fileToSend);
//        if () {} ToDo: Size/Quota check
        if (Files.exists(fileOnServerSide)) {
            ctx.writeAndFlush(new FilePushReply("EXISTS", fileToSend.toString(), fileOnServerSide.toString()));
            LOGGER.info("File EXISTS reply");
        } else {
            ctx.writeAndFlush(new FilePushReply("OK", fileToSend.toString(), fileOnServerSide.toString()));
            LOGGER.info("File OK reply");
        }
    }

    public void copySmallFileFromClient(ChannelHandlerContext ctx, byte[] message) {
        System.out.println(message.getClass());
        try {
            Files.write(fileBeingReceived, message);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }

    }

    public void copyLargeFileFromClientStart(FileFirstChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromClientMiddle(FileChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromClientEnd(ChannelHandlerContext ctx, FileLastChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk(), StandardOpenOption.APPEND);
            fileBeingReceived = null;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    private void createFolder(ChannelHandlerContext ctx, CreateFolderRequest message) {
        Path folderOnServerSide = PATH.toAbsolutePath().resolve(Paths.get(message.path()));
        Path targetFolder = folderOnServerSide.resolve(Paths.get(message.name()));
        if (message.name().equals(rootPath.getFileName().toString())) {
            ctx.writeAndFlush(new CreateFolderNegative("Sorry, you can't create a folder with the same name as your login!"));
            return;
        }
        if (Files.exists(targetFolder)) {
            ctx.writeAndFlush(new CreateFolderNegative("A folder with the same name already exists!"));
            return;
        }
        int i = 0;
        Path checkPath = folderOnServerSide;
        final int SUBDIR_LIMIT = 3;
        while (!checkPath.equals(rootPath.toAbsolutePath())) {
            checkPath = folderOnServerSide.getParent();
            i += 1;
            if (i == SUBDIR_LIMIT) {
                ctx.writeAndFlush(new CreateFolderNegative("Sorry, you can't create another layer of subfolders!"));
                return;
            }
        }
        try {
            Files.createDirectory(targetFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(new ServerFinishedTask());
    }

    private void deleteServerFile(ChannelHandlerContext ctx, DeleteObjectRequest message) {
        Path targetFile = PATH.toAbsolutePath().resolve(Paths.get(message.path(), message.name()));
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(new ServerFinishedTask());
    }

    private void deleteServerDir(ChannelHandlerContext ctx, DeleteObjectRequest message) {
        Path targetFile = PATH.toAbsolutePath().resolve(Paths.get(message.path(), message.name()));
        File dir = new File(String.valueOf(targetFile));
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(new ServerFinishedTask());
    }
}

