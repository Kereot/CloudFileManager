package reqs;

import java.io.Serializable;

public record FileFirstChunk(byte[] chunk) implements Serializable {
}