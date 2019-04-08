package eu.ill.webx.relay.response;


public class RelayImageResponse extends RelayResponse {

    private String data;
    private long windowId;

    public RelayImageResponse(long commandId) {
        super("Image", commandId);
    }

    public RelayImageResponse(long commandId, long windowId, String data) {
        super("Image", commandId);
        this.windowId = windowId;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getWindowId() {
        return windowId;
    }

    public void setWindowId(long windowId) {
        this.windowId = windowId;
    }
}
