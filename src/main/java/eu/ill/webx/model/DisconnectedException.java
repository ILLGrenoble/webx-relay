package eu.ill.webx.model;

public class DisconnectedException extends Exception{
    public DisconnectedException() {
        super("Not connected to WebX server");
    }
}
