package eu.ill.webx.ws;

import eu.ill.webx.model.Credentials;
import eu.ill.webx.relay.Client;
import eu.ill.webx.relay.Host;
import eu.ill.webx.relay.WebXRelay;
import eu.ill.webx.services.AuthService;
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
    private static final String TOKEN_PARAM = "token";
    private static final String WIDTH_PARAM = "width";
    private static final String HEIGHT_PARAM = "height";
    private static final String KEYBOARD_PARAM = "keyboard";

    private final WebXRelay relay;

    private Client client;
    private Host host;

    public WebSocketTunnelListener(final WebXRelay relay) {
        this.relay = relay;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();

        String token = this.getStringParam(params, TOKEN_PARAM);
        Credentials credentials = AuthService.instance().getCredentials(token);
        if (!credentials.isValid()) {
            logger.warn("Connection credentials are invalid. Disconnecting");
            session.close();
            return;
        }
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        // Get all the other params
        Integer port = this.getIntegerParam(params, WEBX_PORT_PARAM);
        String hostname = this.getStringParam(params, WEBX_HOST_PARAM);
        Integer width = this.getIntegerParam(params, WIDTH_PARAM);
        Integer height = this.getIntegerParam(params, HEIGHT_PARAM);
        String keyboard = this.getStringParam(params, KEYBOARD_PARAM);

        // Connect to host
        this.host = this.relay.onClientConnect(hostname, port);
        if (this.host != null) {
            logger.debug("Creating client for {}...", this.host.getHostname());

            this.client = new Client(session);
            if (this.host.connectClient(this.client, username, password, width, height, keyboard)) {
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

    private String getStringParam(Map<String, List<String>> params, String paramName) {
        return params.containsKey(paramName) ? params.get(paramName).get(0) : null;
    }

    private Integer getIntegerParam(Map<String, List<String>> params, String paramName) {
        Integer param = null;
        String paramString = params.containsKey(paramName) ? params.get(paramName).get(0) : null;
        if (paramString != null) {
            try {
                param = Integer.parseInt(paramString);

            } catch (NumberFormatException ignore) {
                logger.warn("Unable to parse integer {} parameter (\"{}\")", paramName, paramString);
            }
        }
        return param;
    }

}
