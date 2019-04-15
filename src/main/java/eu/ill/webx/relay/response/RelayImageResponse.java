package eu.ill.webx.relay.response;


public class RelayImageResponse extends RelayResponse {

    private String data;
    private int depth;
    private long windowId;

    public RelayImageResponse(long commandId) {
        super(commandId);
    }

    public RelayImageResponse(long commandId, long windowId, int depth, String data) {
        super(commandId);
        this.windowId = windowId;
        this.depth = depth;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public long getWindowId() {
        return windowId;
    }

    public void setWindowId(long windowId) {
        this.windowId = windowId;
    }
}
