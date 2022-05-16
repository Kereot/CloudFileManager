import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import reqs.Auth;
import reqs.DirInfo;
import reqs.PassServerPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.logging.Logger;


public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    private static final Path PATH = Paths.get("storage");
    private Path rootPath;
    private Path targetPath;

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
                case "auth":
                    authentication(ctx, message);
                    break;
                case "dir":
                    updateList(ctx, message);
                    break;
            }
        }

        else {
            LOGGER.warning("Smth wrong; message class: " + msg.getClass());
        }
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
}

