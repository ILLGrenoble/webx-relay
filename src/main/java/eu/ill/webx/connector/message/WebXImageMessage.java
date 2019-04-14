package eu.ill.webx.connector.message;


public class WebXImageMessage extends WebXMessage {

    private long windowId;
    private int depth;
    private String data;

    public WebXImageMessage() {
        super("Image");
    }

    public WebXImageMessage(long windowId, int depth, String data) {
        super("Image");
        this.windowId = windowId;
        this.depth = depth;
        this.data = data;
    }

    public long getWindowId() {
        return windowId;
    }

    public void setWindowId(long windowId) {
        this.windowId = windowId;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXImageMessage{");
        sb.append("windowId=").append(windowId);
        sb.append(", depth=").append(depth);
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}