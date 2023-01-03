package eu.ill.webx.exceptions;

public class WebXException extends Exception {
    public WebXException() {
    }

    public WebXException(String message) {
        super(message);
    }

    public WebXException(String message, Throwable cause) {
        super(message, cause);
    }
}
