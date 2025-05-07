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
import eu.ill.webx.WebXEngineConfiguration;
import eu.ill.webx.WebXHostConfiguration;
import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.Transport;
import eu.ill.webx.utils.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides connection to a WebXRouter or standalone WebXEngine.
 * WebXTransport management connections to the ZMQ sockets.
 * WebXHost manages all sessions that are in use on the host. Creation of sessions and connection/creation of clients is handled here.
 */
public class WebXHost {

    private static final Logger logger = LoggerFactory.getLogger(WebXHost.class);

    private final WebXHostConfiguration configuration;
    private final Transport transport = new Transport();

    private List<WebXSession> sessions = new ArrayList<>();

    /**
     * Constructor taking a host configuration
     * @param configuration The host configuration
     */
    WebXHost(final WebXHostConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Returns the hostname
     * @return the hostname
     */
    public String getHostname() {
        return this.configuration.getHostname();
    }

    /**
     * Returns the principal (client connector) port of the host
     * @return the host port
     */
    public int getPort() {
        return this.configuration.getPort();
    }

    /**
     * Starts the connection to the WebX Host. Connects all ZMQ sockets.
     * @throws WebXConnectionException thrown if the connectionfails
     */
    void connect() throws WebXConnectionException {
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

    /**
     * Disconnects all the ZMQ sockets. Blocks until all threads are joined.
     */
    void disconnect() {
        // Disconnect from WebX server
        this.transport.disconnect();
        logger.info("Disconnected from WebX server at {}:{}...", this.getHostname(), this.getPort());
    }

    /**
     * Called when a client connects. Depending on the connection type it will start a new session. In all cases the client is
     * connected to WebX engine.
     * @param clientConfiguration The client connection configuration
     * @param engineConfiguration The engine configuration (WebX Engine)
     * @return a new WebX client
     * @throws WebXConnectionException thrown if the connection fails
     */
    public WebXClient onClientConnection(final WebXClientConfiguration clientConfiguration, final WebXEngineConfiguration engineConfiguration) throws WebXConnectionException {
        if (this.transport.isConnected()) {
            SessionId sessionId;
            if (clientConfiguration.getSessionId() == null) {
                logger.info("Connecting to WebX using password authentication");
                sessionId = this.startSession(clientConfiguration, engineConfiguration);
                logger.info("Authentication successful. Got session Id \"{}\"", sessionId.hexString());

            } else {
                logger.info("Connecting to existing WebX session using sessionId \"{}\"", clientConfiguration.getSessionId());
                sessionId = new SessionId(clientConfiguration.getSessionId());
            }

            // Connect to the session and get a client Id
            final ClientIdentifier clientIdentifier = this.connectClient(sessionId, clientConfiguration.getClientVersion());

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

    /**
     * Called when a client disconnects. The client is disconnected in the WebX engine. The session will be removed if it is empty.
     * @param client the WebX client
     */
    public void onClientDisconnected(WebXClient client) {
        this.disconnectClient(client);

        this.getSession(client.getSessionId()).ifPresent(session -> {
            session.onClientDisconnected(client);

            if (session.getClientCount() == 0) {
                logger.debug("Client removed from session with Id \"{}\". Session now has no clients: stopping it", session.getSessionId().hexString());
                session.stop();

                this.removeSession(session);
            }
        });
    }

    /**
     * Returns the total number of clients connected
     * @return the number of clients connected
     */
    public synchronized int getClientCount() {
        return this.sessions.stream()
                .mapToInt(WebXSession::getClientCount)
                .reduce(0, Integer::sum);
    }

    /**
     * Ensures that there are no empty sessions
     */
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

    /**
     * Adds a new session to the session list
     * @param session the session to add
     */
    private synchronized void addSession(final WebXSession session) {
        this.sessions.add(session);
    }

    /**
     * Removes a session from the sessions list
     * @param session the session to remove
     */
    private synchronized void removeSession(final WebXSession session) {
        this.sessions.remove(session);
    }

    /**
     * Returns a session optional given the Id of the session
     * @param sessionId the id of the session
     * @return and Optional session
     */
    private synchronized Optional<WebXSession> getSession(final SessionId sessionId) {
        return this.sessions.stream().filter(session -> sessionId.equals(session.getSessionId())).findFirst();
    }

    /**
     * Callback from the message publisher when a new message has been sent from the server. The host determines which session is valid (from the header of the message)
     * and forwards it accordingly.
     * @param messageData The raw binary message data
     */
    private void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {} from {}", messageData.length, this.configuration.getHostname());

        // Get session Id
        SessionId sessionId = new SessionId(messageData);
        this.getSession(sessionId).ifPresent(session -> {
            session.onMessage(messageData);
        });
    }

    /**
     * Sends requests to the WebX Router to start a new session.
     * @param clientConfiguration The client configuration (login, screen size, etc)
     * @param engineConfiguration The engine configuration (WebX Engine)
     * @return a unique Session Id for the session
     * @throws WebXConnectionException thrown if the session creation fails
     */
    private SessionId startSession(final WebXClientConfiguration clientConfiguration, final WebXEngineConfiguration engineConfiguration) throws WebXConnectionException {
        try {
            // Start WebX session via the router and get a session ID
            String sessionIdString = this.transport.startSession(clientConfiguration, engineConfiguration);
            return new SessionId(sessionIdString);

        } catch (WebXCommunicationException e) {
            logger.error("Cannot start session: communication failed with the WebX Server");
            throw new WebXConnectionException("Communication failed with the WebX Server when creating WebX session");

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot start session: WebX Server is disconnected");
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    /**
     * Sends a request to create a new client for a specific session. The identifier of the client is used to create a new WebXClient.
     * @param sessionId the session Id
     * @param clientVersion the client version
     * @return a unique client identifier
     * @throws WebXConnectionException thrown if the request fails
     */
    private ClientIdentifier connectClient(final SessionId sessionId, final String clientVersion) throws WebXConnectionException {
        try {
            Tuple<String, String> responseElements = this.sendConnectionRequest(sessionId, clientVersion);
            String clientIdString = responseElements.getX();
            final String clientIndexString = responseElements.getY();

            int clientId = Integer.parseUnsignedInt(clientIdString, 16);
            long clientIndex = Long.parseUnsignedLong(clientIndexString, 16);

            logger.info("Client connected to WebX session \"{}\":  Got client Id \"{}\" and index \"{}\"", sessionId.hexString(), clientIdString, clientIndexString);

            return new ClientIdentifier(clientIndex, clientId);

        } catch (NumberFormatException exception) {
            logger.error("Cannot connect client: Failed to parse client id and index");
            throw new WebXConnectionException("Failed to parse client id and index");

        } catch (WebXCommunicationException e) {
            logger.error("Cannot connect client: Communication with the WebX Server failed");
            throw new WebXConnectionException("Communication with the WebX Server failed when creating WebX session");

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot connect client: WebX Server is disconnected");
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    /**
     * Sends a request to connect a client to a session. The client version is passed also to the WebX Engine.
     * @param sessionId the session Id
     * @param clientVersion the client version
     * @return a tuple containing the client Id and client index
     * @throws WebXConnectionException thrown if the request fails
     * @throws WebXDisconnectedException thrown if the server is disconnected
     * @throws WebXCommunicationException thrown if the communication fails
     */
    private Tuple<String, String> sendConnectionRequest(final SessionId sessionId, final String clientVersion) throws WebXConnectionException, WebXDisconnectedException, WebXCommunicationException {
        try {
            final String request = String.format("connect,%s,%s", sessionId.hexString(), clientVersion);
            final String response = this.sendConnectionRequest(request, sessionId);

            final String[] responseElements = response.split(",");

            if (responseElements.length == 2) {
                return new Tuple<>(responseElements[0], responseElements[1]);
            }

            logger.warn("Failed to connect client with sessionId and client version, using legacy client connection method.");

        } catch (WebXConnectionException exception) {
            logger.warn("Failed to connect client with sessionId and client version ({}), using legacy client connection method.", exception.getMessage());
        }

        return this.sendConnectionRequest(sessionId);
    }

    /**
     * Sends a request to connect a client to a session (legacy method with the client version).
     * @param sessionId the session Id
     * @return a tuple containing the client Id and client index
     * @throws WebXConnectionException thrown if the request fails
     * @throws WebXDisconnectedException thrown if the server is disconnected
     * @throws WebXCommunicationException thrown if the communication fails
     */
    private Tuple<String, String> sendConnectionRequest(final SessionId sessionId) throws WebXConnectionException, WebXDisconnectedException, WebXCommunicationException {
        final String request = String.format("connect,%s", sessionId.hexString());
        final String response = this.sendConnectionRequest(request, sessionId);

        final String[] responseElements = response.split(",");

        if (responseElements.length != 2) {
            throw new WebXConnectionException("WebX Server returned an invalid connection response");
        }

        return new Tuple<>(responseElements[0], responseElements[1]);
    }

    /**
     * Sends a request to the WebX server to connect a client to a session.
     * @param sessionId the session Id
     * @return the raw response from the server
     * @throws WebXConnectionException thrown if the request fails
     * @throws WebXDisconnectedException thrown if the server is disconnected
     * @throws WebXCommunicationException thrown if the communication fails
     */
    private String sendConnectionRequest(final String request, final SessionId sessionId) throws WebXConnectionException, WebXDisconnectedException, WebXCommunicationException {
        String response = this.transport.sendRequest(request).toString();
        if (response == null) {
            throw new WebXConnectionException("WebX Server returned a null connection response");

        } else if (response.isEmpty()) {
            throw new WebXConnectionException(String.format("WebX Server refused connection with sessionId %s", sessionId.hexString()));
        }

        return response;
    }

    /**
     * Sends a request to remove a client from a session
     * @param client The client to remove
     */
    private void disconnectClient(final WebXClient client) {
        if (client != null && client.isConnected()) {
            try {
                final String request = String.format("disconnect,%s,%s", client.getSessionId().hexString(), client.getClientIdentifier().clientIdString());
                SocketResponse response = this.transport.sendRequest(request);
                if (response == null) {
                    logger.error("Failed to get response from WebX server");

                } else {
                    logger.info("Client (Id \"{}\" and index \"{}\") disconnected from WebX session \"{}\"", client.getClientIdentifier().clientIdString(), client.getClientIdentifier().clientIndexString(), client.getSessionId().hexString());
                }

            } catch (WebXCommunicationException e) {
                logger.warn("Cannot disconnect client {}: Communication with the WebX Server failed", client.getClientIdentifier().clientIdString());

            } catch (WebXDisconnectedException e) {
                logger.warn("Cannot disconnect client {}: WebX Server is disconnected", client.getClientIdentifier().clientIdString());
            }
        }
    }

}
