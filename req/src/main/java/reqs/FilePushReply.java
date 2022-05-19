package reqs;

import java.io.Serializable;
import java.nio.file.Path;

public record FilePushReply(String string, String fileToSend, String fileToPlace) implements Serializable {
}

// EXISTS = file already exists, need confirmation to rewrite
// OK = no problems, autosend