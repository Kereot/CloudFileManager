module server {

    requires io.netty.transport;
    requires org.apache.commons.io;
    requires io.netty.codec;
    requires req;
    requires io.netty.handler;
    requires io.netty.buffer;
    requires java.logging;
    requires java.sql;
    requires spring.security.crypto;

    exports server;
}