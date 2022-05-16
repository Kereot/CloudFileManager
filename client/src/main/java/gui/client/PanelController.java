package gui.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PanelController implements Initializable {

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

        drivesBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            drivesBox.getItems().add(p.toString());
        }
        drivesBox.getSelectionModel().select(0);

        mainTable.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2 && mainTable.getSelectionModel().getSelectedItem() != null) {
                Path path = Paths.get(pathField.getText()).resolve(mainTable.getSelectionModel().getSelectedItem().getName());
                if (Files.isDirectory(path)) {
                    list(path);
                }
            }
        });

        list(Paths.get("."));

    }

    public void list(Path path) {
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            mainTable.getItems().clear();
            mainTable.getItems().addAll(Files.list(path).map(FilesInfo::new).toList());
            mainTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Files list update failed", ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    TableView<FilesInfo> mainTable;

    @FXML
    ComboBox<String> drivesBox;

    @FXML
    TextField pathField;

    public void btnParentPathAction(ActionEvent actionEvent) {
        Path parentPath = Paths.get(pathField.getText()).getParent();
        if (parentPath != null) {
            list(parentPath);
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> e = (ComboBox<String>) actionEvent.getSource();
        list(Paths.get(e.getSelectionModel().getSelectedItem()));
    }

    public String getSelectedName() {
        if (!mainTable.isFocused()) {
            return null;
        }
        return mainTable.getSelectionModel().getSelectedItem().getName();
    }

    public String getCurrentPath() {
        return pathField.getText();
    }
}
