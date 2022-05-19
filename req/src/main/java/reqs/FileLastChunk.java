package reqs;

import java.io.Serializable;

public record FileLastChunk(byte[] chunk) implements Serializable {
}
