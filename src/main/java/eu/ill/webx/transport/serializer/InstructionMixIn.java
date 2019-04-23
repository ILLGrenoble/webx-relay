package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.ill.webx.transport.instruction.*;
import eu.ill.webx.transport.message.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConnectInstruction.class, name = "1"),
        @JsonSubTypes.Type(value = WindowsInstruction.class, name = "2"),
        @JsonSubTypes.Type(value = ImageInstruction.class, name = "3"),
        @JsonSubTypes.Type(value = ScreenInstruction.class, name = "4"),
        @JsonSubTypes.Type(value = MouseInstruction.class, name = "5"),
})
public class InstructionMixIn {


}
