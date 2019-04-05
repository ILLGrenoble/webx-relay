package eu.ill.webx.ws;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.relay.Relay;
import eu.ill.webx.relay.command.ClientCommand;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);

    private Relay relay;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onWebSocketConnect(final Session session) {
      logger.debug("WebSocket connection, creating relay...");
      this.relay = new Relay(session);
      this.relay.start();
    }

    @Override
    public void onWebSocketText(String message) {
        if (this.relay == null) {
            logger.error("Received command {} on closed relay", message);
            return;
        }

       logger.debug("Received command: {}", message);
        try {
            ClientCommand command = objectMapper.readValue(message, ClientCommand.class);
            this.relay.queueCommand(command);

        } catch (JsonParseException e) {
            logger.error("Error parsing JSON command", e);

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON command", e);

        } catch (IOException e) {
            logger.error("Unable to convert command to JSON");
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        throw new UnsupportedOperationException("Binary WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error: {}", throwable);
        this.relay.stop();
        this.relay = null;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing with reason: {}", reason);
        this.relay.stop();
        this.relay = null;
    }

}