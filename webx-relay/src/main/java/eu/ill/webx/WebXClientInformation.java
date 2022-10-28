package eu.ill.webx;

public class WebXClientInformation {

    private final String username;
    private final String password;
    private Integer screenWidth = 1440;
    private Integer screenHeight = 900;
    private String keyboardLayout = "gb";

    public WebXClientInformation(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public WebXClientInformation(String username, String password, Integer screenWidth, Integer screenHeight) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public WebXClientInformation(String username, String password, Integer screenWidth, Integer screenHeight, String keyboardLayout) {
        this.username = username;
        this.password = password;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.keyboardLayout = keyboardLayout;
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

    public void setScreenWidth(Integer screenWidth) {
        this.screenWidth = screenWidth;
    }

    public Integer getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(Integer screenHeight) {
        this.screenHeight = screenHeight;
    }

    public String getKeyboardLayout() {
        return keyboardLayout;
    }

    public void setKeyboardLayout(String keyboardLayout) {
        this.keyboardLayout = keyboardLayout;
    }
}
