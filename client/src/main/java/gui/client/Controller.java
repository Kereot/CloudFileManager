package gui.client;

import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.io.FileUtils;
import reqs.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;


public class Controller {

    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    MenuBar barParent;

    @FXML
    public Button copyBtn;
    private static Button savingCopyBtn = new Button();

    public void btnExitAction() {
        if (ClientNetty.hasConnected()) {
            ClientNetty.disconnect();
        }
        Platform.exit();
    }

    private ClientNetty nt = new ClientNetty();

    private final EventHandler<InputEvent> filter = Event::consume;
    private final EventHandler<MouseEvent> filter2 = Event::consume;

    private static ServerController serverController;
    private static PanelController panelController;
    public static void rememberServerController(ServerController sc) {
        serverController = sc;
    }
    public static void rememberPanelController(PanelController pc) {
        panelController = pc;
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        String clientFileName = panelController.getSelectedName();
        String serverFileName = serverController.getSelectedName();
        if (clientFileName == null && serverFileName == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No file selected", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (serverFileName != null
                && serverController.mainTable.getSelectionModel().getSelectedItem().getType()
                    .equals(FilesInfo.ObjectType.FILE)) {
            if (Files.exists(Paths.get(panelController.getCurrentPath()).resolve(Paths.get(serverFileName)))) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "A file with the same name already exists! Do you want to replace it with the new file?");
                Optional<ButtonType> option = alert.showAndWait();
                if (option.get() == ButtonType.CANCEL) {
                    Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Operation aborted");
                    alert1.showAndWait();
                    return;
                }
            }
            if (checkLocalFreeSpace((long) serverController.mainTable.getSelectionModel().getSelectedItem().getSize())) {
                ClientNetty.send(new FileTransferRequest(serverController.getCurrentPath(), serverFileName));
                System.out.println("File copying initialized.");
            } else {
                Alert alertAlert = new Alert(Alert.AlertType.ERROR, "Not enough free disc space!", ButtonType.OK);
                alertAlert.showAndWait();
            }
        }
        if (clientFileName != null
                && panelController.mainTable.getSelectionModel().getSelectedItem().getType()
                    .equals(FilesInfo.ObjectType.FILE)) {
            ClientNetty.send(new FilePushRequest(panelController.getCurrentPath(), serverController.getCurrentPath(), clientFileName,
                    panelController.mainTable.getSelectionModel().getSelectedItem().getSize()));
            savingCopyBtn = copyBtn;
            savingCopyBtn.setDisable(true);
        }
    }

    public void folderBtnAction(ActionEvent actionEvent) {
        if (!panelController.mainTable.isFocused() && !serverController.mainTable.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please, select a side where a new folder is to be created", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog("Folder");

        dialog.setTitle(null);
        dialog.setHeaderText("Enter a folder name:");
        dialog.setContentText("Folder:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        Pattern regex = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*%!-]");
        if (regex.matcher(dialog.getEditor().getText()).find() || dialog.getEditor().getText().length() > 15) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please, don't use special symbols or more than 15 characters", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (panelController.mainTable.isFocused()) {
            createClientFolder(dialog);
        } else {
            createServerFolder(dialog);
        }
    }

    private void createClientFolder(TextInputDialog dialog) {
        Path path = Paths.get(panelController.getCurrentPath()).resolve(Paths.get(dialog.getEditor().getText()));

        if (Files.exists(path)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "A folder with the same name already exists!", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        panelController.list(path.getParent());
    }

    private void createServerFolder(TextInputDialog dialog) {
        String path = serverController.getCurrentPath();
        ClientNetty.send(new CreateFolderRequest(path, dialog.getEditor().getText()));
    }

    public void failedToCompleteServerRequest(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    public void renameBtnAction(ActionEvent actionEvent) {
        if (panelController.getSelectedName() == null && serverController.getSelectedName() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please, select a file or a folder to be renamed", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog("Name");

        dialog.setTitle(null);
        dialog.setHeaderText("Enter a new name:");
        dialog.setContentText("Object:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        Pattern regex = Pattern.compile("[$&+,:;=\\\\?@#|/'<>^*%!-]");
        if (regex.matcher(dialog.getEditor().getText()).find() || dialog.getEditor().getText().length() > 15) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please, don't use special symbols or more than 15 characters", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        if (panelController.mainTable.isFocused()) {
            renameClient(dialog);
        } else {
            renameServer(dialog);
        }
    }

    private void renameClient(TextInputDialog dialog) {
        Path oldPath = Paths.get(panelController.getCurrentPath(), panelController.getSelectedName());
        try {
            Files.move(oldPath, oldPath.resolveSibling(dialog.getEditor().getText()));
        } catch (FileAlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "An object with the same name already exists!", ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Renaming failed for some reason!", ButtonType.OK);
            alert.showAndWait();
            e.printStackTrace();
        }
        panelController.list(Paths.get(panelController.getCurrentPath()));
    }

    private void renameServer(TextInputDialog dialog) {
        String path = serverController.getCurrentPath();
        String oldName = serverController.getSelectedName();
        String newName = dialog.getEditor().getText();
        ClientNetty.send(new RenameRequest(path, oldName, newName));
    }

    public void deleteBtnAction(ActionEvent actionEvent) {
        if (panelController.getSelectedName() == null && serverController.getSelectedName() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please, select a file or a folder to be deleted", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the object? This action cannot be undone!");
        Optional<ButtonType> option = alert.showAndWait();

        if (option.get() == ButtonType.OK) {
            System.out.println("Trying to delete an object");
        } else if (option.get() == ButtonType.CANCEL) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Operation aborted");
            alert1.showAndWait();
            return;
        }
        if (panelController.mainTable.isFocused()) {
            if (panelController.mainTable.getSelectionModel().getSelectedItem().getType().equals(FilesInfo.ObjectType.FILE)) {
                deleteClientFile();
            } else {
                deleteClientDir();
            }
        } else {
            if (serverController.mainTable.getSelectionModel().getSelectedItem().getType().equals(FilesInfo.ObjectType.FILE)) {
                ClientNetty.send(new DeleteObjectRequest("file", serverController.getCurrentPath(), serverController.getSelectedName()));
            } else {
                ClientNetty.send(new DeleteObjectRequest("dir", serverController.getCurrentPath(), serverController.getSelectedName()));
            }
        }
    }

    private void deleteClientFile() {
        Path path = Paths.get(panelController.getCurrentPath());
        try {
            Files.deleteIfExists(path.resolve(Paths.get(panelController.getSelectedName())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        panelController.list(path);
    }

    private void deleteClientDir() {
        Path path = Paths.get(panelController.getCurrentPath());
        File dir = new File(String.valueOf(path.resolve(Paths.get(panelController.getSelectedName()))));
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        panelController.list(path);
    }

    public void loginBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login.isEmpty() || passwordField.getText().isEmpty()) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Login or password field is empty.");
            alert1.showAndWait();
            return;
        }
        try {
            nt.connect(new AuthRequest(login, password), actionEvent, this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void regBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login.isEmpty() || passwordField.getText().isEmpty()) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Login or password field is empty.");
            alert1.showAndWait();
            return;
        }
        try {
            nt.regMe(new AuthRequest(login, password), this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void regSuccess() {
        Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Registration successful.");
        ClientNetty.disconnect();
        alert1.showAndWait();
    }

    public void regFailure(String message) {
        Alert alert1 = new Alert(Alert.AlertType.INFORMATION, message);
        ClientNetty.disconnect();
        alert1.showAndWait();
    }

    public void buildMainScene(ActionEvent actionEvent, List<?> list) {
        ServerController.list = (List<File>) list;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1280, 600);
            Stage stage = new Stage();
            stage.setTitle("File Manager");
            stage.setScene(scene);
            stage.show();
            stage.setOnCloseRequest(windowEvent -> btnExitAction());
            ((Node) (actionEvent.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateServerList(List<File> list) {
        serverController.serverList(list);
    }

    public void setServerPath(String path) {
        serverController.setTextFiled(path);
    }

    public boolean checkLocalFreeSpace(Long fileSize) {
        Path path = Paths.get(panelController.getCurrentPath());
        final long SAFETY_MEASURE = 10000000L;
        try {
            return Files.getFileStore(path).getUsableSpace() > (fileSize - SAFETY_MEASURE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void copySmallFileFromServer(byte[] message) {
        System.out.println(message.getClass());
        try {
            Files.write(getFileNameToCopy(), message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        panelController.list(Paths.get(panelController.getCurrentPath()));
    }

    public void copyLargeFileFromServerStart(FileFirstChunk message) {
        try {
            Files.write(getFileNameToCopy(), message.chunk());
            panelController.wholeWindowClient.addEventFilter(MouseEvent.ANY, filter2);
            panelController.wholeWindowClient.addEventFilter(KeyEvent.ANY, filter);
            serverController.wholeWindowServer.addEventFilter(MouseEvent.ANY, filter2);
            serverController.wholeWindowServer.addEventFilter(KeyEvent.ANY, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromServerMiddle(FileChunk message) {
        try {
            Files.write(getFileNameToCopy(), message.chunk(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyLargeFileFromServerEnd(FileLastChunk message) {
        try {
            Files.write(getFileNameToCopy(), message.chunk(), StandardOpenOption.APPEND);
            panelController.list(Paths.get(panelController.getCurrentPath()));
            panelController.wholeWindowClient.removeEventFilter(MouseEvent.ANY, filter2);
            panelController.wholeWindowClient.removeEventFilter(KeyEvent.ANY, filter);
            serverController.wholeWindowServer.removeEventFilter(MouseEvent.ANY, filter2);
            serverController.wholeWindowServer.removeEventFilter(KeyEvent.ANY, filter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getFileNameToCopy() {
        return Paths.get(panelController.getCurrentPath()).resolve(serverController.getSelectedName());
    }

    public static void loginWindow(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 600);
        stage.setTitle("File Manager");
        stage.setScene(scene);
        stage.show();
    }

    public void logoutBtnAction(ActionEvent actionEvent) {
        ClientNetty.disconnect();
        try {
            Stage stage = new Stage();
            loginWindow(stage);
            barParent.getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateServerListAfterSomeAction() {
        HashMap<String, String> listRequest = new HashMap<>();
        listRequest.put("type", "dir");
        listRequest.put("path", serverController.getCurrentPath());
        ClientNetty.send(listRequest);
        if (savingCopyBtn != null) {
            savingCopyBtn.setDisable(false);
            savingCopyBtn = null;
        }
    }

    public void sendFileToServerAfterConfirmation(ChannelHandlerContext ctx, Path pathToSend, Path pathToPlace) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "A file with the same name already exists! Do you want to replace it with the new file?");
        Optional<ButtonType> option = alert.showAndWait();
        if (option.get() == ButtonType.CANCEL) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Operation aborted");
            alert1.showAndWait();
            savingCopyBtn.setDisable(false);
            savingCopyBtn = null;
        } else if (option.get() == ButtonType.OK) {
            sendFileToServer(ctx, pathToSend, pathToPlace);
        }
    }

    public void sendFileToServer(ChannelHandlerContext ctx, Path pathToSend, Path pathToPlace) {
        System.out.println("Tried to send a file");

        try {
            ctx.writeAndFlush(new FilePlacementOnServer(pathToPlace.toString()));
            byte[] file = Files.readAllBytes(pathToSend);
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
}