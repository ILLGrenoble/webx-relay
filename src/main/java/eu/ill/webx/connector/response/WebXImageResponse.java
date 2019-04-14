package eu.ill.webx.connector.response;

public class WebXImageResponse extends WebXResponse {

    private long windowId;
    private int depth;
    private String data;

    public WebXImageResponse() {
    }

    public WebXImageResponse(long windowId, int depth, String data) {
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
        final StringBuilder sb = new StringBuilder("WebXImageResponse{");
        sb.append("windowId=").append(windowId);
        sb.append(", depth=").append(depth);
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
