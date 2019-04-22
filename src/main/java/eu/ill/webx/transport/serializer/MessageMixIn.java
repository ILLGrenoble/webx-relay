package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.ill.webx.transport.message.ConnectionMessage;
import eu.ill.webx.transport.message.ImageMessage;
import eu.ill.webx.transport.message.SubImagesMessage;
import eu.ill.webx.transport.message.WindowsMessage;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConnectionMessage.class, name = "connection"),
        @JsonSubTypes.Type(value = ImageMessage.class, name = "image"),
        @JsonSubTypes.Type(value = SubImagesMessage.class, name = "subimages"),
        @JsonSubTypes.Type(value = WindowsMessage.class, name = "windows")
})
public class MessageMixIn {


}
