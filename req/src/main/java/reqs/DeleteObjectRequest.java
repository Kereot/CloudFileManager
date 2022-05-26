package reqs;

import java.io.Serializable;

public record DeleteObjectRequest(String type, String path, String name) implements Serializable {
}
// type "file" for files
// type "dir" for folders/directories