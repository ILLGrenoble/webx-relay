package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonSerializer implements Serializer {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    public JsonSerializer() {
        logger.info("JSON serializer instantiated");
        objectMapper.addMixInAnnotations(Message.class, MessageMixIn.class);
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
        byte[] messageData = null;
        try {
            messageData = objectMapper.writeValueAsBytes(message);

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON message");

        } catch (IOException e) {
            logger.error("Unable to convert message to JSON");
        }

        return messageData;
    }

    @Override
    public Message deserializeMessage(byte[] data) {
        Message message = null;
        try {
           message = objectMapper.readValue(data, Message.class);

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON message");

        } catch (IOException e) {
            logger.error("Unable to convert JSON message");
        }

        return message;
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
