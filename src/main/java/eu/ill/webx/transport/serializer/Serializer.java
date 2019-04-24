package eu.ill.webx.transport.serializer;

import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.Message;

public interface Serializer {

    String getType();

    byte[] serializeInstruction(Instruction instruction);
    byte[] serializeMessage(Message message);
    Message deserializeMessage(byte[] data);
    Instruction deserializeInstruction(byte[] data);
}
