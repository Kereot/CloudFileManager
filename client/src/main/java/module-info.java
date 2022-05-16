module gui.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires io.netty.transport;
    requires org.apache.commons.io;
    requires io.netty.codec;
    requires req;

    opens gui.client to javafx.fxml;
    exports gui.client;
}