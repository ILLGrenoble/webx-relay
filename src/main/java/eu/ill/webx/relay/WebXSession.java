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

import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a particular WebX X11 session, identified by a unique sessionId.
 * Manages filtering of messages to groups of clients or specific clients in the session.
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

    private final SessionId sessionId;
    private final Transport transport;
    private final OnErrorHandler onErrorHandler;

    private final List<WebXClient> clients = new ArrayList<>();

    private final WebXSessionValidator sessionValidator;

    /**
     * Constructor taking a unique sessionId and Transport encapsulating all ZMQ sockets. The session validator is created
     * with a callback to handle ping failures and interrupt the client message queue.
     * @param sessionId the unique Session Id
     * @param transport the ZMQ transport layer
     * @param onErrorHandler the callback function to handle errors during session validation
     */
    WebXSession(final SessionId sessionId, final Transport transport, final OnErrorHandler onErrorHandler) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.onErrorHandler = onErrorHandler;
        this.sessionValidator = new WebXSessionValidator(this.sessionId, transport, (error -> {
            logger.warn("Session validation error: {}", error);
            this.sendMessageToClients(new Message.InterruptMessage("Failed to ping WebX Session"));
            this.onErrorHandler.onError(this);
        }));
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
     * @param clientIdentifier the unique Client Identifier
     * @return a WebXClient object
     */
    public synchronized WebXClient createClient(final ClientIdentifier clientIdentifier) {
        final WebXClient client = new WebXClient(clientIdentifier, this);
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

}
