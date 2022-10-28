package eu.ill.webx.exceptions;

public class WebXDisconnectedException extends Exception{
    public WebXDisconnectedException() {
        super("Not connected to WebX server");
    }
}
