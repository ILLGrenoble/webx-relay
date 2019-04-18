package eu.ill.webx.transport.instruction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Instruction {
    public enum Type {
        Connect(1),
        Windows(2),
        Image(3);

        private final int value;

        private Type(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return this.value;
        }


        public static Type fromValue(int value) {
            for (Type type : Type.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }

        @JsonCreator
        public static Type forValue(String v) {
            return Type.fromValue(Integer.parseInt(v));
        }
    }

    private Type type;
    private long id;
    private String stringPayload;
    private long numericPayload;

    public Instruction() {
    }

    public Instruction(Type type) {
        this.type = type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumericPayload() {
        return numericPayload;
    }

    public void setNumericPayload(long numericPayload) {
        this.numericPayload = numericPayload;
    }

    public String getStringPayload() {
        return stringPayload;
    }

    public void setStringPayload(String stringPayload) {
        this.stringPayload = stringPayload;
    }
}
