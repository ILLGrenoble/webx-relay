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

import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SessionCreation;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.transport.Transport;
import eu.ill.webx.utils.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a particular WebX X11 session, identified by a unique sessionId.
 * Manages client connections and filtering of messages to groups of clients or specific clients in the session.
 * Pinging of sessions delegated to WebXSessionValidator.
 */
public class WebXSession {

    /**
     * Defines an interface to handle errors that occur in the session.
     */
    interface OnErrorHandler {
        /**
         * Called when an error occurs on the session
         * @param session the WebXSession that encountered the error
         */
        void onError(final WebXSession session);
    }
    private static final Logger logger = LoggerFactory.getLogger(WebXSession.class);

    private SessionCreation.CreationStatus creationStatus;
    private final SessionId sessionId;
    private final Transport transport;
    private final OnErrorHandler onErrorHandler;

    private final List<WebXClient> clients = new ArrayList<>();

    private final WebXSessionValidator sessionValidator;

    /**
     * Constructor taking a unique sessionId and Transport encapsulating all ZMQ sockets. The session validator is created
     * with a callback to handle ping failures and interrupt the client message queue.
     * @param sessionCreation the session creation including unique Session Id and creation status
     * @param transport the ZMQ transport layer
     * @param onErrorHandler the callback function to handle errors during session validation
     */
    WebXSession(final SessionCreation sessionCreation, final Transport transport, final OnErrorHandler onErrorHandler) {
        this.sessionId = sessionCreation.sessionId();
        this.creationStatus = sessionCreation.status();
        this.transport = transport;
        this.onErrorHandler = onErrorHandler;
        this.sessionValidator = new WebXSessionValidator(this.sessionId, transport, this.creationStatus, this::onCreationStatusUpdate, this::onSessionValidationError);
    }

    /**
     * Returns the session Id
     * @return the session Id
     */
    public SessionId getSessionId() {
        return sessionId;
    }


    /**
     * Starts the session validator thread (pings the session - either to the WebX Engine or via the WebX Router - to ensure it
     * is running correctly)
     */
    public void start() {
        this.sessionValidator.start();
    }

    /**
     * Stops the session validator thread and waits for it to join.
     */
    public void stop() {
        try {
            if (this.sessionValidator.isRunning()) {
                this.sessionValidator.interrupt();
                this.sessionValidator.join();

                logger.debug("Session {} stopped", this.sessionId.hexString());
            }

        } catch (InterruptedException exception) {
            logger.warn("Stop of relay message listener and client instruction threads interrupted", exception);
        }
    }

    /**
     * Creates a new WebXClient object with a unique Client Identifier and adds it to the clients list.
     * If the session is running we connect the client immediately to the WebX Engine otherwise we wait.
     * @param clientVersion the version of the client
     * @return a WebXClient object
     * @throws WebXConnectionException thrown if the connection request fails
     */
    public synchronized WebXClient createClient(final String clientVersion) throws WebXConnectionException {
        WebXClient client;
        if (this.creationStatus == SessionCreation.CreationStatus.RUNNING) {
            final ClientIdentifier clientIdentifier = this.connectClient(sessionId, clientVersion);
            client = new WebXClient(clientIdentifier, this, clientVersion);

        } else {
            client = new WebXClient(this, clientVersion);
        }
        this.clients.add(client);
        return client;
    }

    /**
     * Called when a client disconnects. Removes the client from the clients list.
     * @param client the client that has disconnected
     */
    public synchronized void onClientDisconnected(final WebXClient client) {
        client.onDisconnected();
        this.clients.remove(client);
    }

    /**
     * Returns a list of all connected clients to the session
     * @return a list of all connected clients to the session
     */
    public List<WebXClient> getClients() {
        return new ArrayList<>(this.clients);
    }

    /**
     * Returns the number of connected clients to the session
     * @return the number of connected clients to the session
     */
    public synchronized int getClientCount() {
        return this.clients.size();
    }

    /**
     * Sends a binary instruction to the transport layer
     * @param instructionData the binary instruction data
     */
    public void sendInstruction(byte[] instructionData) {
        this.transport.sendInstruction(instructionData);
    }

    /**
     * Called when the WebX Engine for this session has sent a message. The message contains a client index mask
     * which is used to filter specific clients to which the message is destined.
     * @param messageData The raw binary message data
     */
    public synchronized void onMessage(byte[] messageData) {
        List<WebXClient> indexAssociatedClients = this.clients.stream()
                .filter(webXClient -> webXClient.matchesMessageIndexMask(messageData))
                .toList();

        for (WebXClient client : indexAssociatedClients) {
            client.onMessage(messageData);
        }
    }

    /**
     * Send a Message object to the clients. Used uniquely to interrupt clients when the session is no longer
     * pinging correctly.
     * @param message the message to send to clients
     */
    private void sendMessageToClients(final Message message) {
        for (WebXClient client : this.clients) {
            client.onMessage(message);
        }
    }

    /**
     * Called from the WebXSessionValidator during the session startup process. The validator determines the
     * status of the session from the WebX Router and forwards the current status here. We send clients messages
     * always to ensure that the connection is kept alive.
     * @param creationStatus The creation status of the session
     */
    private synchronized void onCreationStatusUpdate(SessionCreation.CreationStatus creationStatus) {
        this.creationStatus = creationStatus;
        if (creationStatus.equals(SessionCreation.CreationStatus.RUNNING)) {
            for (WebXClient client : this.clients) {
                try {
                    final ClientIdentifier clientIdentifier = this.connectClient(sessionId, client.getClientVersion());
                    client.setClientIdentifier(clientIdentifier);

                    client.onMessage(new Message.ConnectionMessage(false));

                } catch (WebXConnectionException e) {
                    logger.warn("Failed to connect to WebX client", e);
                    client.onDisconnected();
                }
            }

        } else {
            // Send NOP message to all clients to keep the communication channel alive
            for (WebXClient client : this.clients) {
                client.onMessage(new Message.NopMessage());
            }
        }
    }

    /**
     * Called when a communication error occurs with the WebX Router or Engine. This closes the session.
     * @param error The error that occurred
     */
    private void onSessionValidationError(String error) {
        logger.warn("Session validation error: {}", error);
        this.sendMessageToClients(new Message.InterruptMessage("Failed to validate WebX Session"));
        this.onErrorHandler.onError(this);
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
            logger.warn("Cannot connect client: Failed to parse client id and index");
            throw new WebXConnectionException("Failed to parse client id and index");

        } catch (WebXCommunicationException e) {
            logger.warn("Cannot connect client: Communication with the WebX Server failed");
            throw new WebXConnectionException("Communication with the WebX Server failed when creating WebX session");

        } catch (WebXDisconnectedException e) {
            logger.warn("Cannot connect client: WebX Server is disconnected");
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

}
