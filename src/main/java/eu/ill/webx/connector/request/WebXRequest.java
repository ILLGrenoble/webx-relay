package eu.ill.webx.connector.request;

import com.fasterxml.jackson.annotation.JsonValue;

public class WebXRequest {

    public enum Type {
        Connect(1),
        Image(2),
        Images(3),
        Window(4),
        Windows(5),
        Mouse(6);

        private final int value;
        private Type(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return this.value;
        }
    }

    private Type type;
    private String stringPayload;
    private long numericPayload;

    public WebXRequest(Type type) {
        this.type = type;
    }

    public WebXRequest(Type type, long numericPayload) {
        this.type = type;
        this.numericPayload = numericPayload;
    }

    public Type getType() {
        return type;
    }

    public String getStringPayload() {
        return stringPayload;
    }

    public void setStringPayload(String stringPayload) {
        this.stringPayload = stringPayload;
    }

    public long getNumericPayload() {
        return numericPayload;
    }

    public void setNumericPayload(long numericPayload) {
        this.numericPayload = numericPayload;
    }
}
