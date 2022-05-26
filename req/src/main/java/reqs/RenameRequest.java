package reqs;

import java.io.Serializable;

public record RenameRequest(String path, String oldName, String newName) implements Serializable {
}
