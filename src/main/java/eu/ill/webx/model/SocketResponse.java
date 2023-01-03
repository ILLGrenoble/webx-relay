package eu.ill.webx.model;

public class SocketResponse {

    private final byte[] data;

    public SocketResponse(final byte[] data) {
        this.data = data;
    }

    public byte[] data() {
        return this.data;
    }

    public String toString() {
        return new String(this.data);
    }
}
