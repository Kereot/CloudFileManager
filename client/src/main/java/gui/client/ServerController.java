package gui.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    public static List<File> list = new ArrayList<>();

    @FXML
    public VBox wholeWindowServer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<FilesInfo, String> filesNameColumn = new TableColumn<>("Name");
        filesNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        filesNameColumn.prefWidthProperty().bind(mainTable.widthProperty().multiply(0.5));

        TableColumn<FilesInfo, Float> filesSizeColumn = new TableColumn<>("Size (KB)");
        filesSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize() / 1000f));
        filesSizeColumn.prefWidthProperty().bind(mainTable.widthProperty().multiply(0.2));
        filesSizeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Float item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else if (item < 0) {
                    setText("");
                } else {
                    String text = String.format("%1$,.1f", item);
                    setText(text);
                }
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FilesInfo, String> filesDateColumn = new TableColumn<>("Last Modified");
        filesDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        filesDateColumn.prefWidthProperty().bind(mainTable.widthProperty().multiply(0.3));

        mainTable.getColumns().addAll(filesNameColumn, filesSizeColumn, filesDateColumn);
        mainTable.getSortOrder().add(filesSizeColumn);

        mainTable.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2 && mainTable.getSelectionModel().getSelectedItem() != null) {
                Path path = Paths.get(pathField.getText()).resolve(mainTable.getSelectionModel().getSelectedItem().getName());
                HashMap<String, String> listRequest = new HashMap<>();
                listRequest.put("type", "dir");
                listRequest.put("path", path.toString());
                ClientNetty.send(listRequest);
                System.out.println("Tried to request path");
                System.out.println(listRequest);
            }
        });

        serverList(list);
        Controller.rememberServerController(this);
    }

    public void setTextFiled(String path) {
        pathField.setText(path);
    }

    public void serverList(List<File> list) {
            mainTable.getItems().clear();
            List<FilesInfo> serverList = list.stream()
                    .map(File::toPath)
                    .map(FilesInfo::new)
                    .toList();
            mainTable.getItems().addAll(serverList);
            mainTable.sort();
    }

    @FXML
    TableView<FilesInfo> mainTable;

    @FXML
    ComboBox<String> drivesBox;

    @FXML
    TextField pathField;

    public void btnParentPathAction(ActionEvent actionEvent) {
        if (Paths.get(getCurrentPath()).getParent() != null) {
            Path path = Paths.get(getCurrentPath()).getParent();
            HashMap<String, String> listRequest = new HashMap<>();
            listRequest.put("type", "dir");
            listRequest.put("path", path.toString());
            ClientNetty.send(listRequest);
        }
    }

    public String getSelectedName() {
        if (!mainTable.isFocused() || mainTable.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        return mainTable.getSelectionModel().getSelectedItem().getName();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }
}
