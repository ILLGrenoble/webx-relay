package eu.ill.webx.transport.message;

public class ConnectionMessage extends Message {

    private int publisherPort;
    private int collectorPort;

    public ConnectionMessage() {
    }

    public ConnectionMessage(long commandId) {
        super(commandId);
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXConnectionResponse{");
        sb.append("publisherPort=").append(publisherPort);
        sb.append(", collectorPort=").append(collectorPort);
        sb.append('}');
        return sb.toString();
    }
}
