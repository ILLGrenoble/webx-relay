package eu.ill.webx;

public class Configuration {


    private final Integer socketTimeoutMs;
    private final boolean standalone;
    private final int defaultScreenWidth;
    private final int defaultScreenHeight;
    private final String defaultKeyboardLayout;

    public Configuration(final Integer socketTimeoutMs,
                         final boolean standalone,
                         final int defaultScreenWidth,
                         final int defaultScreenHeight,
                         final String defaultKeyboardLayout) {
        this.socketTimeoutMs = socketTimeoutMs;
        this.standalone = standalone;
        this.defaultScreenWidth = defaultScreenWidth;
        this.defaultScreenHeight = defaultScreenHeight;
        this.defaultKeyboardLayout = defaultKeyboardLayout;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public boolean isStandalone() {
        return standalone;
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
}
