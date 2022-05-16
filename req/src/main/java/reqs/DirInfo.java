package reqs;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public record DirInfo(List<File> dirInfoList) implements Serializable {
}
