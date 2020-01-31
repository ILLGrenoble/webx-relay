package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.domain.Size;
import eu.ill.webx.domain.WindowProperties;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.*;
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
    public byte[] serializeMessage(Message message) {
        return null;
    }

    @Override
    public Message deserializeMessage(byte[] data) {
        BinaryBuffer buffer = new BinaryBuffer(data);
        int messageType = buffer.getHeader().getMessageTypeId();
        long commandId = buffer.getInt();

        if (messageType == MessageType.CONNECTION) {
            int publisherPort = buffer.getInt();
            int collectorPort = buffer.getInt();

            ConnectionMessage connectionMessage = new ConnectionMessage(commandId);
            connectionMessage.setPublisherPort(publisherPort);
            connectionMessage.setCollectorPort(collectorPort);

            return connectionMessage;
        }

        return null;
    }

    @Override
    public Instruction deserializeInstruction(byte[] data) {
        Instruction instruction = null;
        try {
            instruction = objectMapper.readValue(data, Instruction.class);

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON instruction");

        } catch (IOException e) {
            logger.error("Unable to convert JSON instruction");
        }

        return instruction;
    }
}
