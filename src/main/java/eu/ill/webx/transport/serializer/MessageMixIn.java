package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.ill.webx.transport.message.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConnectionMessage.class, name = "connection"),
        @JsonSubTypes.Type(value = ScreenMessage.class, name = "screen"),
        @JsonSubTypes.Type(value = ImageMessage.class, name = "image"),
        @JsonSubTypes.Type(value = SubImagesMessage.class, name = "subimages"),
        @JsonSubTypes.Type(value = WindowsMessage.class, name = "windows")
})
public class MessageMixIn {


}
