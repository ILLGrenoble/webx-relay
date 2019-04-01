package eu.ill.webx.ws;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTunnelListener implements WebSocketListener {


    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);


    @Override
    public void onWebSocketConnect(final Session session) {
      logger.debug("WebSocket connection");
    }


    @Override
    public void onWebSocketText(String message) {
       logger.debug("Received message: {}", message);

    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        throw new UnsupportedOperationException("Binary WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error: {}", throwable);
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing with reason: {}", reason);
    }

}