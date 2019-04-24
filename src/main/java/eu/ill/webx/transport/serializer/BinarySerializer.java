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

        } else if (messageType == MessageType.WINDOWS) {
            int numberOfWindows = buffer.getInt();
            for (int i = 0; i < numberOfWindows; i++) {
                WindowProperties windowProperties = new WindowProperties();
                windowProperties.setId(buffer.getInt());
                windowProperties.setX(buffer.getInt());
                windowProperties.setY(buffer.getInt());
                windowProperties.setWidth(buffer.getInt());
                windowProperties.setHeight(buffer.getInt());
            }

        } else if (messageType == MessageType.IMAGE) {
            long windowId = buffer.getInt();
            int depth = buffer.getInt();
            String imageType = buffer.getString(4);
            int imageSize = buffer.getInt();

            ImageMessage imageMessage = new ImageMessage(commandId);
            imageMessage.setWindowId(windowId);
            imageMessage.setDepth(depth);

        } else if (messageType == MessageType.SCREEN) {
            int screenWidth = buffer.getInt();
            int screenHeight = buffer.getInt();

            ScreenMessage screenMessage = new ScreenMessage(commandId, new Size(screenWidth, screenHeight));

            return screenMessage;


        } else if (messageType == MessageType.SUBIMAGES) {

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
