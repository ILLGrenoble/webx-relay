package eu.ill.webx.transport.message;

import eu.ill.webx.domain.Size;

public class ScreenMessage extends Message {

    private String serializer;
    private Size screenSize;

    public ScreenMessage() {
    }

    public ScreenMessage(long commandId, Size screenSize) {
        super(commandId);
        this.screenSize = screenSize;
    }

    public String getSerializer() {
        return serializer;
    }

    public void setSerializer(String serializer) {
        this.serializer = serializer;
    }

    public Size getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(Size screenSize) {
        this.screenSize = screenSize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXConnectionResponse{");
        sb.append(", serializer=").append(serializer);
        sb.append(", screenSize=").append(screenSize);
        sb.append('}');
        return sb.toString();
    }
}
