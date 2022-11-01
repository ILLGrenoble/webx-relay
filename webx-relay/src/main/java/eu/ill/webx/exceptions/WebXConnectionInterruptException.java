package eu.ill.webx.exceptions;

public class WebXConnectionInterruptException extends WebXException {

    public WebXConnectionInterruptException() {
    }

    public WebXConnectionInterruptException(String message) {
        super(message);
    }

    public WebXConnectionInterruptException(String message, Throwable cause) {
        super(message, cause);
    }
}
