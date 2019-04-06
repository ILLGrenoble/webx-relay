package eu.ill.webx.connector.message;

public abstract class WebXMessage {

    private String type;

    public WebXMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
