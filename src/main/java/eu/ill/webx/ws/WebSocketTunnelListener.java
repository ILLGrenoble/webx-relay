package eu.ill.webx.ws;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.relay.Relay;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);
    private final WebXConnector connector;

    private Relay relay;

    public WebSocketTunnelListener(final WebXConnector connector) {
        this.connector = connector;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        logger.debug("WebSocket connection, creating relay...");
        this.relay = new Relay(session, connector);
        this.relay.start();

        // Add relay as a listener to webx messages
        this.connector.getMessageSubscriber().addListener(relay);
    }

    @Override
    public void onWebSocketText(String message) {

        if (this.relay == null) {
            logger.error("Received command {} on closed relay", message);
            return;
        }

        logger.debug("Received command: {}", message);
        // Debugging
//        Instruction command = serializer.deserializeInstruction(message.getBytes());


        this.relay.queueCommand(message.getBytes());
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        throw new UnsupportedOperationException("Binary WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error: {}", throwable);

        // Remove relay from webx subscriber
        this.connector.getMessageSubscriber().removeListener(relay);

        this.relay.stop();
        this.relay = null;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing with reason: {}", reason);

        // Remove relay from webx subscriber
        this.connector.getMessageSubscriber().removeListener(relay);

        this.relay.stop();
        this.relay = null;
    }

}