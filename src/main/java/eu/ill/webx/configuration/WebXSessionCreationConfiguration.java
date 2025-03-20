package eu.ill.webx.configuration;


public class WebXSessionCreationConfiguration implements WebXConnectionConfiguration {

    private final String username;
    private final String password;
    private final Integer screenWidth;
    private final Integer screenHeight;
    private final String keyboardLayout;

    public WebXSessionCreationConfiguration(final String username, final String password, final Integer screenWidth, final Integer screenHeight, final String keyboardLayout) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = keyboardLayout;
    }

    public WebXSessionCreationConfiguration(final String username, final String password, final Integer screenWidth, final Integer screenHeight) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = "gb";
    }

    public WebXSessionCreationConfiguration(final String username, final String password) {
        this.username = username;
        this.password = password;
        this.screenWidth = 1920;
        this.screenHeight = 1024;
        this.keyboardLayout = "gb";
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
}
