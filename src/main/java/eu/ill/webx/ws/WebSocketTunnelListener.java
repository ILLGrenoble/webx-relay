package eu.ill.webx.ws;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.relay.Relay;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.serializer.Serializer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);

    private Relay relay;


    @Override
    public void onWebSocketConnect(final Session session) {
        logger.debug("WebSocket connection, creating relay...");
        this.relay = new Relay(session);
        this.relay.start();

        // Add relay as a listener to webx messages
        WebXConnector.instance().getSubscriber().addListener(relay);
    }

    @Override
    public void onWebSocketText(String message) {
        Serializer serializer = WebXConnector.instance().getSerializer();

        if (this.relay == null) {
            logger.error("Received command {} on closed relay", message);
            return;
        }

       logger.debug("Received command: {}", message);
        Instruction command = serializer.deserializeInstruction(message.getBytes());
        if (command != null) {
            this.relay.queueCommand(command);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        throw new UnsupportedOperationException("Binary WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error: {}", throwable);

        // Remove relay from webx subscriber
        WebXConnector.instance().getSubscriber().removeListener(relay);

        this.relay.stop();
        this.relay = null;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing with reason: {}", reason);

        // Remove relay from webx subscriber
        WebXConnector.instance().getSubscriber().removeListener(relay);

        this.relay.stop();
        this.relay = null;
    }

}