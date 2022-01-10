package eu.ill.webx;

public class Configuration {


    private final Integer socketTimeoutMs;
    private final boolean standalone;

    public Configuration(final Integer socketTimeoutMs,
                         final boolean standalone) {
        this.socketTimeoutMs = socketTimeoutMs;
        this.standalone = standalone;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public boolean isStandalone() {
        return standalone;
    }
}
