package reqs;

import java.io.Serializable;

public record CreateFolderRequest(String path, String name) implements Serializable {
}
