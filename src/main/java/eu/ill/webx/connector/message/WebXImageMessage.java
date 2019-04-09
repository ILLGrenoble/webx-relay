package eu.ill.webx.connector.message;


public class WebXImageMessage extends WebXMessage {

    private long windowId;
    private String data;

    public WebXImageMessage() {
        super("Image");
    }

    public WebXImageMessage(long windowId, String data) {
        super("Image");
        this.windowId = windowId;
        this.data = data;
    }

    public long getWindowId() {
        return windowId;
    }

    public void setWindowId(long windowId) {
        this.windowId = windowId;
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
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
