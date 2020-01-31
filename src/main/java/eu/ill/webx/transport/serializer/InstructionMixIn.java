package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import eu.ill.webx.transport.instruction.*;

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
        @JsonSubTypes.Type(value = KeyboardInstruction.class, name = "6"),
        @JsonSubTypes.Type(value = CursorImageInstruction.class, name = "7"),
})
public class InstructionMixIn {


}
