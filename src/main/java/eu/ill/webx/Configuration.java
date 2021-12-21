package eu.ill.webx;

public class Configuration {


    private final String webXHost;
    private final Integer webXPort;
    private final Integer socketTimeoutMs;
    private final boolean standalone;

    public Configuration(final String webXHost,
                         final Integer webXPort,
                         final Integer socketTimeoutMs,
                         final boolean standalone) {
        this.webXHost = webXHost;
        this.webXPort = webXPort;
        this.socketTimeoutMs = socketTimeoutMs;
        this.standalone = standalone;
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

    public boolean isStandalone() {
        return standalone;
    }
}
