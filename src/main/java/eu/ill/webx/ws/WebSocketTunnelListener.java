package eu.ill.webx.ws;

import eu.ill.webx.relay.Client;
import eu.ill.webx.relay.Host;
import eu.ill.webx.relay.WebXRelay;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);
    private static final String WEBX_HOST_PARAM = "webxhost";
    private static final String WEBX_PORT_PARAM = "webxport";

    private final WebXRelay relay;

    private Client client;
    private Host host;

    public WebSocketTunnelListener(final WebXRelay relay) {
        this.relay = relay;
    }

    @Override
    public void onWebSocketConnect(final Session session) {

        // Get host and port from request parameters if they exist
        String hostname = null;
        Integer port = null;
        Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();
        if (params.containsKey(WEBX_HOST_PARAM)) {
            hostname = params.get(WEBX_HOST_PARAM).get(0);
        }
        if (params.containsKey(WEBX_PORT_PARAM)) {
            String portParam = params.get(WEBX_PORT_PARAM).get(0);
            try {
                port = Integer.parseInt(portParam);
            } catch (NumberFormatException ignore) {
                logger.warn("Unable to parse port parameter \"{}\"", portParam);
            }
        }

        // Connect to host
        this.host = this.relay.onClientConnect(hostname, port);
        if (this.host != null) {
            logger.debug("Creating client for {}...", this.host.getHostname());

            this.client = new Client(session);
            if (this.host.addClient(this.client)) {
                logger.info("... client created.");

            } else {
                logger.warn("... not connected to server {}. Client not created.", this.host.getHostname());
                session.close();
            }

        } else {
            logger.error("Failed to connect to webx server. Client not created.");
            session.close();
        }

    }

    @Override
    public void onWebSocketText(String message) {
        throw new UnsupportedOperationException("Text WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        if (this.client != null && this.client.isRunning()) {
            this.client.queueInstruction(payload);

        } else {
            logger.error("Received instruction on closed client");
        }

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
            this.host.removeClient(client);

            this.client = null;

            this.relay.onClientDisconnect(host);
            this.host = null;
        }
    }

}
