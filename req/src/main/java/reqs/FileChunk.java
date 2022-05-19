package reqs;

import java.io.Serializable;

public record FileChunk(byte[] chunk) implements Serializable {
}