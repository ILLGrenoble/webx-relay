package eu.ill.webx;

public class Configuration {


    private final Integer socketTimeoutMs;
    private final String standaloneHost;
    private final Integer standalonePort;
    private final int defaultScreenWidth;
    private final int defaultScreenHeight;
    private final String defaultKeyboardLayout;

    public Configuration(final Integer socketTimeoutMs,
                         final String standaloneHost,
                         final Integer standalonePort,
                         final int defaultScreenWidth,
                         final int defaultScreenHeight,
                         final String defaultKeyboardLayout) {
        this.socketTimeoutMs = socketTimeoutMs;
        this.standaloneHost = standaloneHost;
        this.standalonePort = standalonePort;
        this.defaultScreenWidth = defaultScreenWidth;
        this.defaultScreenHeight = defaultScreenHeight;
        this.defaultKeyboardLayout = defaultKeyboardLayout;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public String getStandaloneHost() {
        return standaloneHost;
    }

    public Integer getStandalonePort() {
        return standalonePort;
    }

    public int getDefaultScreenWidth() {
        return defaultScreenWidth;
    }

    public int getDefaultScreenHeight() {
        return defaultScreenHeight;
    }

    public String getDefaultKeyboardLayout() {
        return defaultKeyboardLayout;
    }

    public boolean isStandalone() {
        return this.standaloneHost != null && this.standalonePort != null;
    }
}
