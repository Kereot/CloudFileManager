package reqs;

import java.io.Serializable;

public record AuthRequest(String login, String password) implements Serializable {
}
