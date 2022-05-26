package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.io.FileUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;
import reqs.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;



public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    private static final Path PATH = Paths.get("storage");
    private Path rootPath;
    private Path targetPath;
    private Path fileBeingReceived = null;

    private boolean isLogged;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info(ctx.channel().remoteAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOGGER.info("Got smth");
        if (msg instanceof HashMap<?,?> message) {
            String type = (String) message.get("type");
            switch (type) {
                case "auth" -> authentication(ctx, message);
                case "reg" -> registration(ctx, message);
                case "dir" -> updateList(ctx, message);
            }
        }
        if (isLogged && msg instanceof FileTransferRequest message) {
            sendFile(ctx, message);
        }
        if (isLogged && msg instanceof FilePushRequest message) {
            checkBeforeFileAcquisition(ctx, message);
        }
        if (isLogged && msg instanceof FilePlacementOnServer message) {
            fileBeingReceived = Paths.get(message.string());
        }
        if (isLogged && msg instanceof byte[] message) {
            copySmallFileFromClient(ctx, message);
        }
        if (isLogged && msg instanceof FileFirstChunk message) {
            copyLargeFileFromClientStart(message);
        }
        if (isLogged && msg instanceof FileChunk message) {
            copyLargeFileFromClientMiddle(message);
        }
        if (isLogged && msg instanceof FileLastChunk message) {
            copyLargeFileFromClientEnd(ctx, message);
        }
        if (isLogged && msg instanceof CreateFolderRequest message) {
            createFolder(ctx, message);
        }
        if (isLogged && msg instanceof DeleteObjectRequest message) {
            String type = message.type();
            switch (type) {
                case "file" -> deleteServerFile(ctx, message);
                case "dir" -> deleteServerDir(ctx, message);
            }
        }
        if (isLogged && msg instanceof RenameRequest message) {
            renameServer(ctx, message);
        }

        LOGGER.info("This request received: " + msg.getClass());
        if (!isLogged && !(msg instanceof HashMap<?,?>)) {
            LOGGER.warning("Unauthorised access attempt!");
        }

//        ctx.close();
    }

    private void authentication(ChannelHandlerContext ctx, HashMap<?,?> message) throws Exception {
        String login = (String) message.get("login");
//        System.out.println(BCrypt.hashpw((String) message.get("pass"), BCrypt.gensalt(12))); // or use https://bcrypt-generator.com/ for encryption

//        String password = getEncryptedPassword((String) message.get("pass")); // standard approach without BCrypt
//        PreparedStatement psUser = DBHandler.getConnection().prepareStatement("SELECT login FROM clients WHERE login = ? AND password = ?;");
//        psUser.setString(1, login);
//        psUser.setString(2, password);
        PreparedStatement psUser = DBHandler.getConnection().prepareStatement("SELECT password FROM clients WHERE login = ?;");
        psUser.setString(1, login);
        ResultSet rs = psUser.executeQuery();
        Channel currentChannel = ctx.channel();
        if (rs.next()) {
            if (BCrypt.checkpw((String) message.get("pass"), rs.getString(1))) {
                LOGGER.info("Client authenticated.");
                rootPath = PATH.resolve(login);
                if (Files.notExists(rootPath)) {
                    Files.createDirectory(rootPath);
                }
                isLogged = true;
                currentChannel.writeAndFlush(new AuthSuccess(Files.list(rootPath).map(Path::toFile).toList()));
                currentChannel.writeAndFlush(new PassServerPath(login));
                LOGGER.info("Files list sent to client.");
            } else {
                LOGGER.warning("Wrong password. Login: " + message.get("login"));
                currentChannel.writeAndFlush(new ServerRequestNegative("Wrong login or password!")); // we actually know it's password
            }
        } else {
            LOGGER.warning("Wrong login. Login: " + message.get("login"));
            currentChannel.writeAndFlush(new ServerRequestNegative("Wrong login or password!")); // we actually know it's login
        }
    }

//    private String getSalt() throws NoSuchAlgorithmException { // if registration is implemented AND BCrypt is not used, can be useful
//        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
//        byte[] salt = new byte[16];
//        sr.nextBytes(salt);
//        return Arrays.toString(salt);
//    }

//    private static String getEncryptedPassword(String passwordToHash) { // standard approach without BCrypt
//        String generatedPassword = null;
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
////            md.update(salt.getBytes());
//            byte[] bytes = md.digest(passwordToHash.getBytes());
//            StringBuilder sb = new StringBuilder();
//            for (byte aByte : bytes) {
//                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
//            }
//            generatedPassword = sb.toString();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        return generatedPassword;
//    }

    private void registration(ChannelHandlerContext ctx, HashMap<?,?> message) {
        Channel currentChannel = ctx.channel();
        try {
            String login = (String) message.get("login");
            PreparedStatement psUser = DBHandler.getConnection().prepareStatement("SELECT login FROM clients WHERE login = ?;");
            psUser.setString(1, login);
            ResultSet rs = psUser.executeQuery();
            if (rs.next()) {
                LOGGER.info("That username already exists: " + login);
                currentChannel.writeAndFlush(new ServerRequestNegative("This login is taken, please choose another."));
            } else {
                String password = BCrypt.hashpw((String) message.get("pass"), BCrypt.gensalt(12));
                PreparedStatement psReg = DBHandler.getConnection().prepareStatement("INSERT INTO clients (login, password) VALUES ( ? , ? );");
                psReg.setString(1, login);
                psReg.setString(2, password);
                psReg.executeUpdate();
                currentChannel.writeAndFlush(new ServerFinishedTask());
            }
        } catch (SQLException e) {
            LOGGER.severe("Some database error: " + e);
            currentChannel.writeAndFlush(new ServerRequestNegative("Sorry, some internal error with registration."));
            e.printStackTrace();
        } finally {
            currentChannel.close();
            ctx.close();
        }
    }

    private void updateList(ChannelHandlerContext ctx, HashMap<?,?> message) throws IOException {
        LOGGER.info("Tried to update list");
        targetPath = PATH.resolve(Paths.get((String) message.get("path")));
        if (Files.isDirectory(targetPath)) {
            Channel currentChannel = ctx.channel();
            currentChannel.writeAndFlush(new DirInfo(Files.list(targetPath).map(Path::toFile).toList()));
            currentChannel.writeAndFlush(new PassServerPath((String) message.get("path")));
            LOGGER.info("Files list sent to client.");
        } else {
            LOGGER.fine("A file was double clicked.");
        }
    }

    private void sendFile(ChannelHandlerContext ctx, FileTransferRequest message) {
        LOGGER.info("Tried to send a file");
        Path path = Paths.get(PATH.resolve(message.path()).resolve(message.fileName()).toString());

        try {
            byte[] file = Files.readAllBytes(path);
            LOGGER.info(path.toString());
            LOGGER.info(file.getClass() + " " + file.length);
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
            LOGGER.severe("Smth went wrong with file sending. " + e);
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
        try {
            Files.write(fileBeingReceived, message);
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Something went wrong when copying!"));
            LOGGER.severe("Was unable to copy " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    public void copyLargeFileFromClientStart(FileFirstChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk());
        } catch (IOException e) {
            LOGGER.severe("Was unable to copy " + e);
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromClientMiddle(FileChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.severe("Was unable to copy " + e);
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromClientEnd(ChannelHandlerContext ctx, FileLastChunk message) {
        try {
            Files.write(fileBeingReceived, message.chunk(), StandardOpenOption.APPEND);
            fileBeingReceived = null;
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Something went wrong when copying!"));
            LOGGER.severe("Was unable to copy " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    private void createFolder(ChannelHandlerContext ctx, CreateFolderRequest message) {
        Path folderOnServerSide = PATH.toAbsolutePath().resolve(Paths.get(message.path()));
        Path targetFolder = folderOnServerSide.resolve(Paths.get(message.name()));
        if (message.name().equals(rootPath.getFileName().toString())) {
            ctx.writeAndFlush(new ServerRequestNegative("Sorry, you can't create a folder with the same name as your login!"));
            return;
        }
        if (Files.exists(targetFolder)) {
            ctx.writeAndFlush(new ServerRequestNegative("A folder with the same name already exists!"));
            return;
        }
        int i = 0;
        Path checkPath = folderOnServerSide;
        final int SUBDIR_LIMIT = 3;
        while (!checkPath.equals(rootPath.toAbsolutePath())) {
            checkPath = folderOnServerSide.getParent();
            i += 1;
            if (i == SUBDIR_LIMIT) {
                ctx.writeAndFlush(new ServerRequestNegative("Sorry, you can't create another layer of subfolders!"));
                return;
            }
        }
        try {
            Files.createDirectory(targetFolder);
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Creation failed for some reason!"));
            LOGGER.severe("Was unable to create " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    private void deleteServerFile(ChannelHandlerContext ctx, DeleteObjectRequest message) {
        Path targetFile = PATH.toAbsolutePath().resolve(Paths.get(message.path(), message.name()));
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Deleting failed for some reason!"));
            LOGGER.severe("Was unable to delete " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    private void deleteServerDir(ChannelHandlerContext ctx, DeleteObjectRequest message) {
        Path targetFile = PATH.toAbsolutePath().resolve(Paths.get(message.path(), message.name()));
        File dir = new File(String.valueOf(targetFile));
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Deleting failed for some reason!"));
            LOGGER.severe("Was unable to delete " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }

    private void renameServer(ChannelHandlerContext ctx, RenameRequest message) {
        Path oldPath = PATH.toAbsolutePath().resolve(Paths.get(message.path(), message.oldName()));
        try {
            Files.move(oldPath, oldPath.resolveSibling(message.newName()));
        } catch (FileAlreadyExistsException e) {
            ctx.writeAndFlush(new ServerRequestNegative("An object with the same name already exists!"));
        } catch (IOException e) {
            ctx.writeAndFlush(new ServerRequestNegative("Renaming failed for some reason!"));
            LOGGER.severe("Was unable to rename " + e);
            e.printStackTrace();
        } finally {
            ctx.writeAndFlush(new ServerFinishedTask());
        }
    }
}

