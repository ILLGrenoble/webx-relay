package eu.ill.webx.connector;

public class DisconnectedException extends Exception{
    public DisconnectedException() {
        super("Not connected to WebX server");
    }
}
