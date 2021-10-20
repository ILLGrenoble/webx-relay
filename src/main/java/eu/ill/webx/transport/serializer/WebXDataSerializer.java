package eu.ill.webx.transport.serializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.transport.instruction.WebXInstruction;
import eu.ill.webx.transport.message.WebXConnectionMessage;
import eu.ill.webx.transport.message.WebXMessage;
import eu.ill.webx.transport.message.WebXMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class WebXDataSerializer {

    private static final Logger logger = LoggerFactory.getLogger(WebXDataSerializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    public WebXDataSerializer() {
        logger.info("Binary serializer instantiated");
    }




    // TODO send binary
    public byte[] serializeInstruction(WebXInstruction instruction) {
        byte[] instructionData = null;

        try {
//            instructionData = objectMapper.writeValueAsBytes(instruction);

        } catch (JsonParseException e) {
            logger.error("Error parsing JSON response for request type " + instruction.getType());

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON response for request type " + instruction.getType());

        } catch (IOException e) {
            logger.error("Unable to convert response to JSON for request type " + instruction.getType());
        }

        return instructionData;
    }

    public WebXMessage deserializeMessage(byte[] data) {
        WebXMessageBuffer buffer = new WebXMessageBuffer(data);
        int messageType = buffer.getHeader().getMessageTypeId();
        long commandId = buffer.getInt();

        if (messageType == WebXMessageType.CONNECTION) {
            int publisherPort = buffer.getInt();
            int collectorPort = buffer.getInt();

            WebXConnectionMessage connectionMessage = new WebXConnectionMessage(commandId);
            connectionMessage.setPublisherPort(publisherPort);
            connectionMessage.setCollectorPort(collectorPort);

            return connectionMessage;
        }

        return null;
    }

    public WebXInstruction deserializeInstruction(byte[] data) {
        final WebXInstructionBuffer buffer = new WebXInstructionBuffer(data);

        WebXInstruction instruction = null;
        try {
            instruction = objectMapper.readValue(data, WebXInstruction.class);

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON instruction: {}", e.getMessage());

        } catch (IOException e) {
            logger.error("Unable to convert JSON instruction: {}", e.getMessage());
        }

        return instruction;
    }
}
