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

public class Host implements MessageListener {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private final String hostname;
    private final int port;
    private final Configuration configuration;
    private final Transport transport;


    private final Map<String, List<Client>> clients = new HashMap<>();

    private Thread thread;
    private boolean running = false;

    public Host(final String hostname, int port, final Configuration configuration) {
        this.hostname = hostname;
        this.port = port;
        this.configuration = configuration;

        this.transport = new Transport();
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public synchronized int getClientCount() {
        return this.clients.size();
    }

    public synchronized boolean start() {
        if (!this.running) {
            // Initialise transport: verify that the host has a running webx server
            if (this.connectToWebXHost()) {
                this.running = true;

                // Start connection checker
                this.thread = new Thread(this::connectionCheck);
                this.thread.start();
            }
        }

        return this.transport.isConnected();
    }

    public synchronized void stop() {
        if (this.running) {
            try {
                this.running = false;

                // Disconnect from webx server
                this.transport.disconnect();

                this.thread.interrupt();
                this.thread.join();
                this.thread = null;

                logger.info("Host disconnected from {} and thread stopped", this.hostname);

            } catch (InterruptedException exception) {
                logger.error("Stop of Host thread for {} interrupted", this.hostname);
            }
        }
    }

    public synchronized boolean connectClient(Client client, String username, String password, Integer width, Integer height) {
        if (this.transport.isConnected()) {

            int screenWidth = width != null ? width : this.configuration.getDefaultScreenWidth();
            int screenHeight = height != null ? height : this.configuration.getDefaultScreenHeight();
            if (client.start(this.transport, this.configuration.isStandalone(), username, password, screenWidth, screenHeight)) {
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
        while (this.running) {
            synchronized (this) {
                if (this.transport.isConnected()) {
                    try {
                        // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                        logger.trace("Sending router ping to {}", this.hostname);
                        if (configuration.isStandalone()) {
                            this.transport.getConnector().sendRequest("ping");

                        } else {
                            this.transport.getSessionChannel().sendRequest("ping");
                        }

                    } catch (DisconnectedException e) {
                        logger.error("Failed to get response from connector ping at {}", this.hostname);

                        // Remove subscription to messages
                        this.transport.getMessageSubscriber().removeListener(this);

                        this.transport.disconnect();
                        this.disconnectClients();
                    }

                } else {
                    this.connectToWebXHost();
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException ignored) {
            }
        }
    }

    private boolean connectToWebXHost() {
        try {
            logger.info("Connecting to WebX server at {}:{}...", this.hostname, this.port);
            this.transport.connect(this.hostname, this.port, configuration.getSocketTimeoutMs(), configuration.isStandalone());
            logger.info("... connected to {}", this.hostname);

            // Subscribe to messages once connected
            this.transport.getMessageSubscriber().addListener(this);

        } catch (DisconnectedException e) {
            // Failed to connect
        }

        return this.transport.isConnected();
    }

    private synchronized void disconnectClients() {
        for (Map.Entry<String, List<Client>> entry : this.clients.entrySet()) {
            List<Client> sessionClients = entry.getValue();

            for (Client client : sessionClients) {
                client.getSession().close();
            }
        }

        logger.info("Disconnected all clients from {}", this.hostname);
    }

    @Override
    public synchronized void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {} from {}", messageData.length, this.hostname);

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
