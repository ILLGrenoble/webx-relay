package eu.ill.webx;

import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.MessageListener;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebXHost implements MessageListener {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private static final Logger logger = LoggerFactory.getLogger(WebXHost.class);

    private final WebXConfiguration configuration;
    private final Transport transport;
    private boolean pingReceived = false;

    private final Map<String, List<WebXClient>> clients = new HashMap<>();

    private Thread thread;
    private boolean running = false;

    public WebXHost(final WebXConfiguration configuration) {
        this.configuration = configuration;

        this.transport = new Transport();
    }

    public String getHostname() {
        return this.configuration.getHostname();
    }

    public int getPort() {
        return this.configuration.getPort();
    }

    public synchronized int getClientCount() {
        return this.clients.size();
    }

    public void start() throws WebXConnectionException {
        synchronized (this) {
            if (!this.running) {
                // Initialise transport: verify that the host has a running webx server
                this.connectToWebXHost();
                this.running = true;

                // Start connection checker
                this.thread = new Thread(this::connectionCheck);
                this.thread.start();
            }
        }

        if (!this.waitForPing()) {
            logger.error("Timeout will waiting to receive ping from WebX Host {}", this.configuration.getHostname());
            throw new WebXConnectionException("Failed to ping WebX Host after startup");
        }
    }

    public void stop() {
        if (this.running) {
            synchronized (this) {
                this.running = false;

                // Disconnect from webx server
                this.transport.disconnect();
            }

            try {
                this.thread.interrupt();
                this.thread.join();
                this.thread = null;

                logger.info("Host disconnected from {} and thread stopped", this.configuration.getHostname());

            } catch (InterruptedException exception) {
                logger.error("Stop of Host thread for {} interrupted", this.configuration.getHostname());
            }
        }
    }

    public synchronized WebXClient createClient() throws WebXConnectionException {
        if (this.transport.isConnected()) {

            WebXClient client = new WebXClient();
            client.connect(transport);
            this.addClient(client);

            return client;
        }

        logger.error("Trying to create client but transport to standalone host is not connected");
        throw new WebXConnectionException("Transport to standalone host not connected when creating client");
    }

    public synchronized WebXClient createClient(WebXClientInformation clientInformation) throws WebXConnectionException {
        if (this.transport.isConnected()) {

            WebXClient client = new WebXClient();
            client.connect(transport, clientInformation);

            this.addClient(client);

            return client;
        }

        logger.error("Trying to create client but transport to host is not connected");
        throw new WebXConnectionException("Transport to host not connected when creating client");
    }

    private void addClient(WebXClient client) {
        String sessionId = client.getWebXSessionId();
        List<WebXClient> sessionClients = this.clients.get(sessionId);
        if (sessionClients == null) {
            sessionClients = new ArrayList<>();
            this.clients.put(sessionId, sessionClients);
        }
        sessionClients.add(client);
    }

    public synchronized void removeClient(WebXClient client) {
        String sessionId = client.getWebXSessionId();
        List<WebXClient> sessionClients = this.clients.get(sessionId);
        if (sessionClients != null) {
            sessionClients.remove(client);
            if (sessionClients.size() == 0) {
                this.clients.remove(sessionId);
            }
        }
    }

    private boolean waitForPing() {
        // Wait for a ping to ensure comms have been set up
        long startTime = new Date().getTime();
        long delay = 0;
        while (delay < 5000 && !this.pingReceived) {
            try {
                Thread.sleep(1000);
                delay = new Date().getTime() - startTime;

            } catch (InterruptedException ignored) {
            }
        }

        return this.pingReceived;
    }

    private void connectionCheck() {
        while (this.running) {
            synchronized (this) {
                if (this.running) {
                    if (this.transport.isConnected()) {
                        try {
                            // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                            logger.trace("Sending router ping to {}", this.configuration.getHostname());
                            this.transport.sendPing();

                            this.pingReceived = true;

                        } catch (WebXDisconnectedException e) {
                            logger.error("Failed to get response from connector ping at {}", this.configuration.getHostname());

                            // Remove subscription to messages
                            this.transport.getMessageSubscriber().removeListener(this);

                            this.transport.disconnect();
                            this.disconnectClients();
                        }

                    } else {
                        try {
                            this.connectToWebXHost();

                        } catch (WebXConnectionException exception) {
                            logger.warn("Failed to connect to WebX host {}: {}", this.getHostname(), exception.getMessage());
                        }
                    }
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException ignored) {
            }
        }
    }

    private void connectToWebXHost() throws WebXConnectionException {
        try {
            logger.info("Connecting to WebX server at {}:{}...", this.configuration.getHostname(), this.configuration.getPort());
            this.transport.connect(this.configuration.getHostname(), this.configuration.getPort(), configuration.getSocketTimeoutMs(), configuration.isStandalone());
            logger.info("... connected to {}", this.configuration.getHostname());

            // Subscribe to messages once connected
            this.transport.getMessageSubscriber().addListener(this);

        } catch (WebXDisconnectedException e) {
            throw new WebXConnectionException("Failed to connect to WebX host");
        }
    }

    private synchronized void disconnectClients() {
        for (Map.Entry<String, List<WebXClient>> entry : this.clients.entrySet()) {
            List<WebXClient> sessionClients = entry.getValue();

            for (WebXClient client : sessionClients) {
                client.close();
            }
        }

        logger.info("Disconnected all clients from {}", this.configuration.getHostname());
    }

    @Override
    public synchronized void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {} from {}", messageData.length, this.configuration.getHostname());

        // Get session Id
        String uuid = this.sessionIdToHex(messageData);
        List<WebXClient> sessionClients = this.clients.get(uuid);
        if (sessionClients != null) {
            for (WebXClient client : sessionClients) {
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
