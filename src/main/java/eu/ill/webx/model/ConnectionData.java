package eu.ill.webx.model;

public class ConnectionData {

    private int publisherPort;
    private int collectorPort;
    private int sessionPort;
    private String serverPublicKey;

    public ConnectionData(int publisherPort, int collectorPort, int sessionPort, String serverPublicKey) {
        this.publisherPort = publisherPort;
        this.collectorPort = collectorPort;
        this.sessionPort = sessionPort;
        this.serverPublicKey = serverPublicKey;
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

    public int getSessionPort() {
        return sessionPort;
    }

    public void setSessionPort(int sessionPort) {
        this.sessionPort = sessionPort;
    }

    public String getServerPublicKey() {
        return serverPublicKey;
    }

    public void setServerPublicKey(String serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }
}
