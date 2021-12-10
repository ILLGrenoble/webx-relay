package eu.ill.webx;

public class Configuration {


    private final String webXHost;
    private final Integer webXPort;
    private final Integer socketTimeoutMs;

    public Configuration(final String webXHost,
                         final Integer webXPort,
                         final Integer socketTimeoutMs) {
        this.webXHost = webXHost;
        this.webXPort = webXPort;
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public String getWebXHost() {
        return webXHost;
    }

    public Integer getWebXPort() {
        return webXPort;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }
}
