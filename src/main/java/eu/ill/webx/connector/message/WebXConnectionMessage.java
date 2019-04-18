package eu.ill.webx.connector.message;

import eu.ill.webx.domain.utils.Size;

public class WebXConnectionMessage extends WebXMessage {

    private int publisherPort;
    private int collectorPort;
    private Size screenSize;

    public WebXConnectionMessage() {
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
        sb.append(", screenSize=").append(screenSize);
        sb.append('}');
        return sb.toString();
    }
}
