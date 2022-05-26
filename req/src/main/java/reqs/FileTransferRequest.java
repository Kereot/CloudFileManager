package reqs;

import java.io.Serializable;

public record FileTransferRequest(String path, String fileName) implements Serializable {
}
