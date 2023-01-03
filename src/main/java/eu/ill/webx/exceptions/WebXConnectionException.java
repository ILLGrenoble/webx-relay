package eu.ill.webx.exceptions;

public class WebXConnectionException extends WebXException {

    public WebXConnectionException() {
    }

    public WebXConnectionException(String message) {
        super(message);
    }

    public WebXConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
