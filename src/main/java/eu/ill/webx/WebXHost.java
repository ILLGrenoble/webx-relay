/*
 * WebX Relay
 * Copyright (C) 2023 Institut Laue-Langevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ill.webx;

import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.MessageListener;
import eu.ill.webx.transport.Transport;
import eu.ill.webx.utils.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebXHost implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WebXHost.class);
    private static final int FAST_PING_MS = 1000;
    private static final int SLOW_PING_MS = 5000;

    private final WebXConfiguration configuration;
    private final Transport transport;
    private boolean pingReceived = false;
    private int pingInterval = SLOW_PING_MS;

    private final Map<SessionId, List<WebXClient>> clients = new HashMap<>();

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
        SessionId sessionId = client.getSessionId();
        List<WebXClient> sessionClients = this.clients.computeIfAbsent(sessionId, k -> new ArrayList<>());
        sessionClients.add(client);
    }

    public synchronized void removeClient(WebXClient client) {
        client.disconnect(transport);

        SessionId sessionId = client.getSessionId();
        List<WebXClient> sessionClients = this.clients.get(sessionId);
        if (sessionClients != null) {
            sessionClients.remove(client);
            if (sessionClients.isEmpty()) {
                this.clients.remove(sessionId);
            }
        }
    }

    private boolean waitForPing() {
        // Wait for a ping to ensure comms have been set up
        long startTime = new Date().getTime();
        long delay = 0;
        this.pingInterval = FAST_PING_MS;
        while (delay < 5000 && !this.pingReceived) {
            try {
                Thread.sleep(100);
                delay = new Date().getTime() - startTime;

            } catch (InterruptedException ignored) {
            }
        }
        this.pingInterval = SLOW_PING_MS;

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
                            this.transport.sendRequest("ping");

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
                Thread.sleep(this.pingInterval);

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
        for (Map.Entry<SessionId, List<WebXClient>> entry : this.clients.entrySet()) {
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
        SessionId sessionId = new SessionId(messageData);
        List<WebXClient> sessionClients = this.clients.get(sessionId);
        if (sessionClients != null) {
            for (WebXClient client : sessionClients) {
                client.onMessage(messageData);
            }

        } else {
            // TODO stop engine from sending messages if no client is connected
//            logger.warn("Message received but no client connected");
        }
    }
}
