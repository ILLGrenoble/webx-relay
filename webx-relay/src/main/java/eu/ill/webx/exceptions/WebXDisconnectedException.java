package eu.ill.webx.exceptions;

public class WebXDisconnectedException extends WebXException {
    public WebXDisconnectedException() {
        super("Not connected to WebX server");
    }
}
