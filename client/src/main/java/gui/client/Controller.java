package gui.client;

import gui.client.requests.AuthRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;


public class Controller {


    public void btnExitAction(ActionEvent actionEvent) {
        if (ClientNetty.hasConnected()) {
            ClientNetty.disconnect();
        }
        Platform.exit();
    }

    public void copyBtnAction(ActionEvent actionEvent) {
        try {
//            PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
//            ServerController rightPC = (ServerController) rightPanel.getProperties().get("srv");
//
//
//            if (leftPC.getSelectedName() != null) {
//                PanelController srcPC = leftPC;
//                ServerController dstPC = rightPC;
//                Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedName());
//
//                Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
//
//                if (Files.isDirectory(srcPath)) {
//                    copyDir(srcPath, dstPath, dstPC);
//                } else {
//                    copyFile(srcPath, dstPath, dstPC);
//                }
//            } else if (rightPC.getSelectedName() != null) {
//                ServerController srcPC = rightPC;
//                PanelController dstPC = leftPC;
//                Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedName());
//
//                Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
//
//                if (Files.isDirectory(srcPath)) {
//                    copyDir(srcPath, dstPath, dstPC);
//                } else {
//                    copyFile(srcPath, dstPath, dstPC);
//                }
//            }
//
//            Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedName());
//
//            Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
//
//            if (Files.isDirectory(srcPath)) {
//                copyDir(srcPath, dstPath, dstPC);
//            } else {
//                copyFile(srcPath, dstPath, dstPC);
//            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, CommonMessages.INACTIVE, ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void copyFile(Path srcPath, Path dstPath, PanelController dstPC) {
        try {
            Files.copy(srcPath, dstPath);
            dstPC.list(Paths.get(dstPC.getCurrentPath()));
        } catch (FileAlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "A file with the same name already exists! Do you want to replace it with the new file?");
            Optional<ButtonType> option = alert.showAndWait();

            if (option.get() == ButtonType.OK) {
                try {
                    Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
                    dstPC.list(Paths.get(dstPC.getCurrentPath()));
                } catch (AccessDeniedException a) {
                    Alert alert1 = new Alert(Alert.AlertType.ERROR, "An object to be replaced must have 'Read only' tag or be otherwise protected. Operation failed!");
                    alert1.showAndWait();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else if (option.get() == ButtonType.CANCEL) {
                Alert alert1 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
                alert1.showAndWait();
            } else {
                Alert alert2 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
                alert2.showAndWait();
            }
        } catch (AccessDeniedException a) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "An object to be replaced must have 'Read only' tag or be otherwise protected. Operation failed!");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDir(Path srcPath, Path dstPath, PanelController dstPC) {
        File srcFile = new File(String.valueOf(srcPath));
        File dstFile = new File(String.valueOf(dstPath));
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "In respected folders this will replace files with the same names if any. Proceed?");
        Optional<ButtonType> option = alert.showAndWait();
        if (option.get() == ButtonType.OK) {
            try {
                FileUtils.copyDirectory(srcFile, dstFile);
                dstPC.list(Paths.get(dstPC.getCurrentPath()));
            } catch (IllegalArgumentException a) {
                Alert alert1 = new Alert(Alert.AlertType.ERROR, "Some objects to be replaced must have 'Read only' tag or be otherwise protected. Operation failed fully or partially!");
                alert1.showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (option.get() == ButtonType.CANCEL) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert1.showAndWait();
        } else {
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert2.showAndWait();
        }
    }

    public void transferBtnAction(ActionEvent actionEvent) {
        try {
            PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
            PanelController rightPC = (PanelController) rightPanel.getProperties().get("srv");

            PanelController srcPC = null;
            PanelController dstPC = null;
            if (leftPC.getSelectedName() != null) {
                srcPC = leftPC;
                dstPC = rightPC;
            }
            if (rightPC.getSelectedName() != null) {
                srcPC = rightPC;
                dstPC = leftPC;
            }

            Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedName());
            Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());
            try {
                Files.move(srcPath, dstPath);
                dstPC.list(Paths.get(dstPC.getCurrentPath()));
                srcPC.list(Paths.get(srcPC.getCurrentPath()));
            } catch (FileAlreadyExistsException e) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "A file with the same name already exists! Do you want to replace it with the new file?");
                Optional<ButtonType> option = alert.showAndWait();

                if (option.get() == ButtonType.OK) {
                    try {
                        Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
                        dstPC.list(Paths.get(dstPC.getCurrentPath()));
                        srcPC.list(Paths.get(srcPC.getCurrentPath()));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else if (option.get() == ButtonType.CANCEL) {
                    Alert alert1 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
                    alert1.showAndWait();
                } else {
                    Alert alert2 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
                    alert2.showAndWait();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, CommonMessages.INACTIVE, ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void deleteBtnAction(ActionEvent actionEvent) {
        try {
            PanelController leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
            PanelController rightPC = (PanelController) rightPanel.getProperties().get("srv");

            PanelController srcPC = null;
            if (leftPC.getSelectedName() != null) {
                srcPC = leftPC;
            }
            if (rightPC.getSelectedName() != null) {
                srcPC = rightPC;
            }

            Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedName());
            if (Files.isDirectory(srcPath)) {
                deleteDir(srcPath, srcPC);
            } else {
                deleteFile(srcPath, srcPC);
            }
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, CommonMessages.INACTIVE, ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(Path srcPath, PanelController srcPC) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the file?");
        Optional<ButtonType> option = alert.showAndWait();

        if (option.get() == ButtonType.OK) {
            Files.deleteIfExists(srcPath);
            srcPC.list(Paths.get(srcPC.getCurrentPath()));
        } else if (option.get() == ButtonType.CANCEL) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert1.showAndWait();
        } else {
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert2.showAndWait();
        }
    }

    private void deleteDir(Path srcPath, PanelController srcPC) throws IOException {
        File srcFile = new File(String.valueOf(srcPath));
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the folder?");
        Optional<ButtonType> option = alert.showAndWait();

        if (option.get() == ButtonType.OK) {
            FileUtils.deleteDirectory(srcFile);
            srcPC.list(Paths.get(srcPC.getCurrentPath()));
        } else if (option.get() == ButtonType.CANCEL) {
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert1.showAndWait();
        } else {
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION, CommonMessages.ABORT);
            alert2.showAndWait();
        }
    }

    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    private ClientNetty nt = new ClientNetty();

    public void loginBtnAction(ActionEvent actionEvent) {
        String login = loginField.getText();
        int password = passwordField.getText().hashCode();
        if (login.isEmpty() || passwordField.getText().isEmpty()) {
            return; // нужно дописать вывод сообщения
        }
        try {
            nt.connect(new AuthRequest(login, password), actionEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void buildMainScene(ActionEvent actionEvent, List<?> list) {
        ServerController.list = (List<File>) list;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main.fxml"));
//            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("server.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1280, 600);
            Stage stage = new Stage();
            stage.setTitle("File Manager");
            stage.setScene(scene);
            stage.show();
            ((Node) (actionEvent.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ServerController serverController;

    public static void rememberMe(ServerController svc) {
        serverController = svc;
    }

    public void updateList(List<File> list) {
        serverController.serverList(list);
    }

    public void setServerPath(String path) {
        serverController.setTextFiled(path);
    }

    public static void loginWindow(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 600);
        stage.setTitle("File Manager");
        stage.setScene(scene);
        stage.show();
    }
    @FXML
    MenuBar barParent;
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

}