package eu.ill.webx.connector.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = WebXImageMessage.class, name = "image"),
        @Type(value = WebXSubImagesMessage.class, name = "subimages"),
        @Type(value = WebXWindowsMessage.class, name = "windows")
})
public abstract class WebXMessage {

    private String type;

    public WebXMessage() {
    }
}
