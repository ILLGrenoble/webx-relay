package eu.ill.webx.transport.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = ConnectionMessage.class, name = "connection"),
        @Type(value = ImageMessage.class, name = "image"),
        @Type(value = SubImagesMessage.class, name = "subimages"),
        @Type(value = WindowsMessage.class, name = "windows")
})
public abstract class Message {

    private String type;
    private long commandId;

    public Message() {
    }

    public Message(long commandId) {
        this.commandId = commandId;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }
}
