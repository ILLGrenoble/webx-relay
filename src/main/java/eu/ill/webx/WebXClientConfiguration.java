package eu.ill.webx;


public class WebXClientConfiguration {

    private final String username;
    private final String password;
    private final String sessionId;
    private final Integer screenWidth;
    private final Integer screenHeight;
    private final String keyboardLayout;

    public static WebXClientConfiguration ForLogin(final String username, final String password, final Integer screenWidth, final Integer screenHeight, final String keyboardLayout) {
        return new WebXClientConfiguration(username, password, screenWidth, screenHeight, keyboardLayout);
    }

    public static WebXClientConfiguration ForLogin(final String username, final String password, final Integer screenWidth, final Integer screenHeight) {
        return new WebXClientConfiguration(username, password, screenWidth, screenHeight, "gb");
    }

    public static WebXClientConfiguration ForLogin(final String username, final String password) {
        return new WebXClientConfiguration(username, password, 1920, 1024, "gb");
    }

    public static WebXClientConfiguration ForExistingSession(final String sessionId) {
        return new WebXClientConfiguration(sessionId);
    }

    public static WebXClientConfiguration ForStandaloneSession() {
        return new WebXClientConfiguration("00000000000000000000000000000000");
    }

    private WebXClientConfiguration(final String username, final String password, final Integer screenWidth, final Integer screenHeight, final String keyboardLayout) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = keyboardLayout;
        this.sessionId = null;
    }

    private WebXClientConfiguration(final String sessionId) {
        this.username = null;
        this.password = null;
        this.screenWidth = null;
        this.screenHeight = null;
        this.keyboardLayout = null;
        this.sessionId = sessionId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Integer getScreenWidth() {
        return screenWidth;
    }

    public Integer getScreenHeight() {
        return screenHeight;
    }

    public String getKeyboardLayout() {
        return keyboardLayout;
    }

    public String getSessionId() {
        return sessionId;
    }
}
