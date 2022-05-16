import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBHandler {
    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());
    private static Connection connection;

    public static void connect() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite:server/auth.db");
    }

    public static void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "EXCEPTION!", e);
//                e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}