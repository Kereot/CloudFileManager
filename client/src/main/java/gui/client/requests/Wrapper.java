package gui.client.requests;

import java.io.Serializable;

public interface Wrapper extends Serializable {
    Request getType();
}
