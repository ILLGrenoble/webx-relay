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
package eu.ill.webx.relay;

import eu.ill.webx.WebXClientConfiguration;
import eu.ill.webx.WebXHostConfiguration;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides connection to a WebXRouter or standalone WebXEngine.
 * WebXTransport management connections to the ZMQ sockets.
 * WebXHost managed all sessions that are in use on the host. Creation of sessions and connection/creation of clients is handled here.
 */
public class WebXHost {

    private static final Logger logger = LoggerFactory.getLogger(WebXHost.class);

    private final WebXHostConfiguration configuration;
    private final Transport transport;

    private List<WebXSession> sessions = new ArrayList<>();

    WebXHost(final WebXHostConfiguration configuration) {
        this.configuration = configuration;

        this.transport = new Transport();
    }

    public String getHostname() {
        return this.configuration.getHostname();
    }

    public int getPort() {
        return this.configuration.getPort();
    }

    public void connect() throws WebXConnectionException {
        if (!this.transport.isConnected()) {
            // Initialise transport: verify that the host has a running WebX server
            try {
                logger.info("Connecting to WebX server at {}:{}...", this.configuration.getHostname(), this.configuration.getPort());
                this.transport.connect(this.configuration.getHostname(), this.configuration.getPort(), configuration.getSocketTimeoutMs(), configuration.isStandalone(), this::onMessage);
                logger.info("... connected to {}", this.configuration.getHostname());

            } catch (WebXDisconnectedException e) {
                throw new WebXConnectionException("Failed to connect to WebX host");
            }
        }
    }

    public void disconnect() {
        // Disconnect from WebX server
        this.transport.disconnect();
    }

    public WebXClient onClientConnection(final WebXClientConfiguration clientConfiguration) throws WebXConnectionException {
        if (this.transport.isConnected()) {
            SessionId sessionId;
            if (clientConfiguration.getSessionId() == null) {
                logger.info("Connecting to WebX using password authentication");
                sessionId = this.startSession(transport, clientConfiguration);
                logger.info("Authentication successful. Got session Id \"{}\"", sessionId.hexString());

            } else {
                logger.info("Connecting to existing WebX session using sessionId \"{}\"", clientConfiguration.getSessionId());
                sessionId = new SessionId(clientConfiguration.getSessionId());
            }

            // Connect to the session and get a client Id
            final ClientIdentifier clientIdentifier = this.connectClient(transport, sessionId);

            final WebXSession session = this.getSession(sessionId).orElseGet(() -> {
               final WebXSession webXSession = new WebXSession(sessionId, transport);
               webXSession.start();

               this.addSession(webXSession);
               return webXSession;
            });

            return session.createClient(clientIdentifier);
        }

        logger.error("Trying to create client but transport to host is not connected");
        throw new WebXConnectionException("Transport to host not connected when creating client");
    }

    public void onClientDisconnected(WebXClient client) {
        this.disconnectClient(transport, client);

        this.getSession(client.getSessionId()).ifPresent(session -> {
            session.disconnectClient(client);

            if (session.getClientCount() == 0) {
                logger.debug("Client removed from session with Id \"{}\". Session now has no clients: stopping it", session.getSessionId().hexString());
                session.stop();

                this.removeSession(session);
            }
        });
    }

    public synchronized void cleanupSessions() {
        this.sessions = this.sessions.stream().filter(session -> {
            if (session.getClientCount() == 0) {
                logger.debug("Cleanup: Session with Id \"{}\" has no clients: stopping it", session.getSessionId().hexString());
                session.stop();
                return false;
            }
            return true;
        }).toList();
    }

    private synchronized void addSession(final WebXSession session) {
        this.sessions.add(session);
    }

    private synchronized void removeSession(final WebXSession session) {
        this.sessions.remove(session);
    }

    private synchronized Optional<WebXSession> getSession(final SessionId sessionId) {
        return this.sessions.stream().filter(session -> sessionId.equals(session.getSessionId())).findFirst();
    }

    private void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {} from {}", messageData.length, this.configuration.getHostname());

        // Get session Id
        SessionId sessionId = new SessionId(messageData);
        this.getSession(sessionId).ifPresent(session -> {
            session.onMessage(messageData);
        });
    }

    public synchronized int getClientCount() {
        return this.sessions.stream()
                .mapToInt(WebXSession::getClientCount)
                .reduce(0, Integer::sum);
    }

    private SessionId startSession(Transport transport, WebXClientConfiguration clientConfiguration) throws WebXConnectionException {
        try {
            // Start WebX session via the router and get a session ID
            String response = transport.getSessionChannel().startSession(clientConfiguration);
            String[] responseData = response.split(",");
            int responseCode = Integer.parseInt(responseData[0]);
            String sessionIdString = responseData[1];
            if (responseCode == 0) {
                return new SessionId(sessionIdString);

            } else {
                logger.error("Couldn't create WebX session: {}", sessionIdString);
                throw new WebXConnectionException("Couldn't create WebX session: session response invalid");
            }

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot start session: WebX Server is disconnected");
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    private ClientIdentifier connectClient(final Transport transport, final SessionId sessionId) throws WebXConnectionException {
        try {
            final String request = String.format("connect,%s", sessionId.hexString());
            String response = transport.sendRequest(request).toString();
            if (response == null) {
                throw new WebXConnectionException("WebX Server returned a null connection response");

            } else if (response.isEmpty()) {
                throw new WebXConnectionException(String.format("WebX Server refused connection with sessionId %s", sessionId.hexString()));
            }

            final String[] responseElements = response.split(",");

            if (responseElements.length != 2) {
                throw new WebXConnectionException("WebX Server returned an invalid connection response");
            }

            String clientIdString = responseElements[0];
            final String clientIndexString = responseElements[1];

            int clientId = Integer.parseUnsignedInt(responseElements[0], 16);
            long clientIndex = Long.parseUnsignedLong(responseElements[1], 16);

            logger.info("Client connected to WebX session \"{}\":  Got client Id \"{}\" and index \"{}\"", sessionId.hexString(), clientIdString, clientIndexString);

            return new ClientIdentifier(clientIndex, clientId);

        } catch (NumberFormatException exception) {
            logger.error("Cannot connect client: Failed to parse client id and index");
            throw new WebXConnectionException("Failed to parse client id and index");

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot connect client: WebX Server is disconnected");
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    private void disconnectClient(final Transport transport, final WebXClient client) {
        if (client != null && client.isConnected()) {
            try {
                final String request = String.format("disconnect,%s,%s", client.getSessionId().hexString(), client.getClientIdentifier().clientIdString());
                SocketResponse response = transport.sendRequest(request);
                if (response == null) {
                    logger.error("Failed to get response from WebX server");

                } else {
                    logger.info("Client (Id \"{}\" and index \"{}\") disconnected from WebX session \"{}\"", client.getClientIdentifier().clientIdString(), client.getClientIdentifier().clientIndexString(), client.getSessionId().hexString());
                }

            } catch (WebXDisconnectedException e) {
                logger.warn("Cannot disconnect client {}: WebX Server is disconnected", client.getClientIdentifier().clientIdString());
            }
        }
    }

}
