package eu.ill.webx.transport.message;

public class ConnectionMessage extends Message {

    private int publisherPort;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXConnectionResponse{");
        sb.append("publisherPort=").append(publisherPort);
        sb.append('}');
        return sb.toString();
    }
}
