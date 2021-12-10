package eu.ill.webx.ws;

import eu.ill.webx.relay.Client;
import eu.ill.webx.relay.WebXRelay;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);
    private final WebXRelay relay;

    private Client client;

    public WebSocketTunnelListener(final WebXRelay relay) {
        this.relay = relay;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        logger.debug("WebSocket connection, creating client...");
        this.client = new Client(session);

        if (this.relay.addClient(this.client)) {
            logger.info("... client created.");

        } else {
            logger.warn("... not connected to server. Client not created.");
            session.close();
        }
    }

    @Override
    public void onWebSocketText(String message) {
        throw new UnsupportedOperationException("Text WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        if (this.client == null) {
            logger.error("Received instruction on closed client");
            return;
        }

        this.client.queueInstruction(payload);
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error", throwable);

        this.disconnect();
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing{}", reason != null ? " with reason " + reason : "");

        this.disconnect();
    }

    private void disconnect() {
        if (this.client != null) {
            this.relay.removeClient(client);

            this.client = null;
        }
    }

}
