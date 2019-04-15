package eu.ill.webx.relay.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = RelayConnectionResponse.class, name = "connection"),
        @Type(value = RelayImageResponse.class, name = "image"),
        @Type(value = RelayWindowsResponse.class, name = "windows")
})
public class RelayResponse {

    private long commandId;

    public RelayResponse() {
    }

    public RelayResponse(long commandId) {
        this.commandId = commandId;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }
}
