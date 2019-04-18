package eu.ill.webx.transport.message;

import eu.ill.webx.domain.utils.Size;

public class ConnectionMessage extends Message {

    private int publisherPort;
    private int collectorPort;
    private String serializer;
    private Size screenSize;

    public ConnectionMessage() {
    }

    public ConnectionMessage(long commandId, Size screenSize) {
        super(commandId);
        this.screenSize = screenSize;
    }

    public int getPublisherPort() {
        return publisherPort;
    }

    public void setPublisherPort(int publisherPort) {
        this.publisherPort = publisherPort;
    }

    public int getCollectorPort() {
        return collectorPort;
    }

    public void setCollectorPort(int collectorPort) {
        this.collectorPort = collectorPort;
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
        sb.append("publisherPort=").append(publisherPort);
        sb.append(", collectorPort=").append(collectorPort);
        sb.append(", serializer=").append(serializer);
        sb.append(", screenSize=").append(screenSize);
        sb.append('}');
        return sb.toString();
    }
}
