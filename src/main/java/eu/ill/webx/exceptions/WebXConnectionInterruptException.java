package eu.ill.webx.exceptions;

public class WebXConnectionInterruptException extends Exception{

    public WebXConnectionInterruptException() {
    }

    public WebXConnectionInterruptException(String message) {
        super(message);
    }

    public WebXConnectionInterruptException(String message, Throwable cause) {
        super(message, cause);
    }
}
