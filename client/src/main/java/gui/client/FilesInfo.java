package gui.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FilesInfo {
    public enum ObjectType {
        FILE("F"), DIR("D");

        private String type;

        ObjectType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private String name;
    private ObjectType type;
    private float size;
    private LocalDateTime lastModified;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public FilesInfo(Path path) {
        try {
            this.name = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? ObjectType.DIR : ObjectType.FILE;
            if (this.type == ObjectType.DIR) {
                this.size = -1f;
            }
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
