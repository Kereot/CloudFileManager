package reqs;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public record AuthSuccess(List<File> authList) implements Serializable {
}
