package eu.ill.webx.configuration;


public class WebXSessionJoinConfiguration implements WebXConnectionConfiguration {

    private final String sessionId;

    public WebXSessionJoinConfiguration(final String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
