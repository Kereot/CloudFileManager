package reqs;

import java.io.Serializable;

public record FilePushRequest(String pathClient, String pathServer, String fileName, float size) implements Serializable {
}
