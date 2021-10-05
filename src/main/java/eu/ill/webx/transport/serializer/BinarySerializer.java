package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.ConnectionMessage;
import eu.ill.webx.transport.message.Message;
import eu.ill.webx.transport.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class BinarySerializer implements Serializer {

    private static final Logger logger = LoggerFactory.getLogger(BinarySerializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    public BinarySerializer() {
        logger.info("JSON serializer instantiated");
        objectMapper.addMixInAnnotations(Instruction.class, InstructionMixIn.class);
    }

    public String getType() {
        return "binary";
    }

    @Override
    public byte[] serializeInstruction(Instruction instruction) {
        byte[] instructionData = null;
        try {
            instructionData = objectMapper.writeValueAsBytes(instruction);

        } catch (JsonParseException e) {
            logger.error("Error parsing JSON response for request type " + instruction.getType());

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON response for request type " + instruction.getType());

        } catch (IOException e) {
            logger.error("Unable to convert response to JSON for request type " + instruction.getType());
        }

        return instructionData;
    }

    @Override
    public Message deserializeMessage(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);
        int messageType = buffer.getHeader().getMessageTypeId();
        long commandId = buffer.getInt();

        if (messageType == MessageType.CONNECTION) {
            int publisherPort = buffer.getInt();

            ConnectionMessage connectionMessage = new ConnectionMessage(commandId);
            connectionMessage.setPublisherPort(publisherPort);

            return connectionMessage;
        }

        return null;
    }

}
