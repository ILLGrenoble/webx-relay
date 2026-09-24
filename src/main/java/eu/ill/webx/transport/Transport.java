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
package eu.ill.webx.transport;

import eu.ill.webx.WebXClientConfiguration;
import eu.ill.webx.WebXEngineConfiguration;
import eu.ill.webx.WebXHostConfiguration;
import eu.ill.webx.exceptions.WebXClientConnectionException;
import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.model.SessionCreation;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

/**
 * Wraps all ZMQ sockets into a single interface.
 * Each WebX Host uses an individual Transport to communicate with the server.
 */
public class Transport {
    private static final Logger logger = LoggerFactory.getLogger(Transport.class);

    private ZContext context;
    private boolean connected = false;

    private final WebXHostConfiguration configuration;
    private final ClientConnector connector;
    private final MessageSubscriber messageSubscriber;
    private final InstructionPublisher instructionPublisher;
    private final SessionChannel sessionChannel;

    /**
     * Constructs the Transport object
     * @param configuration The host configuration
     * @param messageHandler a handler for all incoming messages from the server
     */
    public Transport(final WebXHostConfiguration configuration, final MessageSubscriber.MessageHandler messageHandler) {
        this.configuration = configuration;
        this.connector = new ClientConnector(configuration.getSocketTimeoutMs(), configuration.getSocketRetries(), configuration.isStandalone());
        this.messageSubscriber = new MessageSubscriber(messageHandler);
        this.instructionPublisher = new InstructionPublisher();
        this.sessionChannel = !configuration.isStandalone() ? new SessionChannel(configuration.getSocketTimeoutMs(), configuration.getSocketRetries()) : null;
    }

    /**
     * Returns true if connected
     * @return true if connected
     */
    public boolean isConnected() {
        return this.connected;
    }

    /**
     * Returns true if standalone session
     * @return true if standalone session
     */
    public boolean isStandalone() {
        return this.configuration.isStandalone();
    }

    /**
     * Starts the connection to the different ZQM sockets of the server.
     * @throws WebXDisconnectedException thrown in the connection fails
     */
    public synchronized void connect() throws WebXDisconnectedException {

        if (this.context == null) {
            this.connected = false;
            this.context = new ZContext();

            final String hostname = this.configuration.getHostname();
            final Integer port = this.configuration.getPort();

            try {
                ConnectionData connectionData = this.connector.connect(this.context, "tcp://" + hostname + ":" + port);
                this.messageSubscriber.connect(this.context, "tcp://" + hostname + ":" + connectionData.publisherPort());
                this.instructionPublisher.connect(this.context, "tcp://" + hostname + ":" + connectionData.subscriberPort());

                if (!this.configuration.isStandalone()) {
                    this.sessionChannel.connect(this.context, "tcp://" + hostname + ":" + connectionData.sessionPort(), connectionData.serverPublicKey());
                }

                this.connected = true;

            } catch (WebXDisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new WebXDisconnectedException();
            }

        }
    }

    /**
     * Disconnects all ZMQ sockets and waits for any associated threads to terminate.
     */
    public synchronized void disconnect() {
        if (this.context != null) {
            this.connected = false;

            if (this.sessionChannel != null) {
                this.sessionChannel.disconnect();
            }
            this.instructionPublisher.disconnect();
            this.messageSubscriber.disconnect();
            this.connector.disconnect();

            this.context.destroy();
            this.context = null;
        }
    }

    /**
     * Sends an instruction to the WebX server
     * @param instructionData the instruction data
     */
    public synchronized void sendInstruction(byte[] instructionData) {
        this.instructionPublisher.queueInstruction(instructionData);
    }

    /**
     * Sends a synchronous request to the server using either the client connector or session channel depending on whether the server is running in
     * standalone or not
     * @param request The string formatted request
     * @return The Socket response
     * @throws WebXCommunicationException thrown if the communication fails
     * @throws WebXDisconnectedException thrown if the server is not connected
     */
    public synchronized SocketResponse sendRequest(final String request) throws WebXCommunicationException, WebXDisconnectedException {
        if (!this.connected) {
             throw new WebXDisconnectedException();
        }

        if (this.configuration.isStandalone()) {
            return this.connector.sendRequest(request);

        } else {
            return this.sessionChannel.sendRequest(request);
        }
    }

    /**
     * Sends a request to the session channel to start a new session with connection credentials and engine configuration parameters
     * @param configuration The configuration for the session (login, screen size and keyboard)
     * @param engineConfiguration The configuration for the WebX Engine
     * @return a SessionCreation object containing a unique Session Id and the creation status
     * @throws WebXCommunicationException thrown if an error occurs with the socket communication
     * @throws WebXDisconnectedException thrown if the server is not running in standalone mode
     * @throws WebXClientConnectionException Thrown if the connection response is invalid or an error occurs with the handling
     */
    public synchronized SessionCreation startSession(final WebXClientConfiguration configuration, final WebXEngineConfiguration engineConfiguration) throws WebXCommunicationException, WebXDisconnectedException, WebXClientConnectionException {
        if (!this.configuration.isStandalone()) {
            return this.sessionChannel.startSession(configuration, engineConfiguration);

        } else {
            throw new WebXDisconnectedException("Cannot start session in standalone mode");
        }
    }
}
