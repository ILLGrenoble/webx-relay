package eu.ill.webx.relay;

import eu.ill.webx.Configuration;
import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.model.MessageListener;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebXRelay implements MessageListener {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final Transport transport;

    private final Configuration configuration;

    private final Map<String, List<Client>> clients = new HashMap<>();

    private Thread thread;

    public WebXRelay(Configuration configuration) {
        this.configuration = configuration;

        this.transport = new Transport();
    }

    public void run() {
        // Start connection checker
        this.thread = new Thread(this::connectionCheck);
        this.thread.start();
    }

    public synchronized boolean addClient(Client client) {
        if (this.transport.isConnected()) {

            if (client.start(this.transport, this.configuration.isStandalone())) {
                String sessionId = client.getWebXSessionId();
                List<Client> sessionClients = this.clients.get(sessionId);
                if (sessionClients == null) {
                    sessionClients = new ArrayList<>();
                    this.clients.put(sessionId, sessionClients);
                }
                sessionClients.add(client);

                return true;
            }
        }

        return false;
    }

    public synchronized void removeClient(Client client) {
        client.stop();

        String sessionId = client.getWebXSessionId();
        List<Client> sessionClients = this.clients.get(sessionId);
        if (sessionClients != null) {
            sessionClients.remove(client);
            if (sessionClients.size() == 0) {
                this.clients.remove(sessionId);
            }
        }
    }

    private void connectionCheck() {
        boolean running = true;
        while (running) {
            if (this.transport.isConnected()) {
                try {
                    // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                    if (configuration.isStandalone()) {
                        this.transport.getConnector().sendRequest("ping");

                    } else {
                        this.transport.getSessionChannel().sendRequest("ping");
                    }

                } catch (DisconnectedException e) {
                    logger.error("Failed to get response from connector ping");

                    // Remove subscription to messages
                    this.transport.getMessageSubscriber().removeListener(this);

                    this.transport.disconnect();
                    this.disconnectClients();
                }

            } else {
                try {
                    logger.info("Connecting to WebX server...");
                    this.transport.connect(this.configuration);
                    logger.info("... connected");

                    // Subscribe to messages once connected
                    this.transport.getMessageSubscriber().addListener(this);

                } catch (DisconnectedException e) {
                    // Failed to connect again
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                logger.warn("WebXRelay sleep connection check sleep interrupted");
            }
        }
    }

    private synchronized void disconnectClients() {
        for (Map.Entry<String, List<Client>> entry : this.clients.entrySet()) {
            List<Client> sessionClients = entry.getValue();

            for (Client client : sessionClients) {
                client.getSession().close();
            }
        }

        logger.info("Disconnected all clients");
    }

    @Override
    public synchronized void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {}", messageData.length);

        // Get session Id
        String uuid = this.sessionIdToHex(messageData);
        List<Client> sessionClients = this.clients.get(uuid);
        if (sessionClients != null) {
            for (Client client : sessionClients) {
                client.onMessage(messageData);
            }

        } else {
            // TODO stop engine from sending messages if no client is connected
//            logger.warn("Message received but no client connected");
        }
    }

    private String sessionIdToHex(byte[] bytes) {
        char[] hexChars = new char[32];
        for (int j = 0; j < 16; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
