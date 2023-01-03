package eu.ill.webx;

public class WebXConfiguration {

    private final String hostname;
    private final Integer port;
    private boolean isStandalone;

    private Integer socketTimeoutMs = 15000;

    public WebXConfiguration(final String hostname, final Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

    public WebXConfiguration(final String hostname, final Integer port, boolean isStandalone) {
        this.hostname = hostname;
        this.port = port;
        this.isStandalone = isStandalone;
    }

    public WebXConfiguration(final String hostname, final Integer port, final Integer socketTimeoutMs) {
        this.hostname = hostname;
        this.port = port;
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public boolean isStandalone() {
        return isStandalone;
    }
}
