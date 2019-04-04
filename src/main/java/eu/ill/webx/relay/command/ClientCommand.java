package eu.ill.webx.relay.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

public class ClientCommand {
    public enum Type {
        Connect(1),
        Windows(1);

        @JsonProperty("value")
        private final int value;

        private Type(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return this.value;
        }

        @JsonCreator
        public static Type fromValue(final JsonNode jsonNode) {

            for (Type type : Type.values()) {
                if (type.value == jsonNode.get("value").asInt()) {
                    return type;
                }
            }
            return null;
        }
    }

    private Type type;
    private String stringPayload;
    private int integerPayload;

    public ClientCommand(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

}
