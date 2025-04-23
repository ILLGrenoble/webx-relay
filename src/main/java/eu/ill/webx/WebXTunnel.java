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

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.Message;
import eu.ill.webx.relay.WebXClient;
import eu.ill.webx.relay.WebXHost;
import eu.ill.webx.relay.WebXRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebXTunnel provides the main entry point to connecting to a WebX Host. Session creation and client connection is handled
 * through the connect method.
 * Each client connected to the relay has an individual tunnel through which instructions can be passed to the WebX Engine and
 * messages can be read from the engine.
 */
public class WebXTunnel {

    private static final Logger logger = LoggerFactory.getLogger(WebXTunnel.class);

    private WebXHost host;
    private WebXClient client;

    /**
     * Static method to create a WebXTunnel, connect to the host and create a client.
     * @param hostConfiguration Configuration for the WebX Host (eg hostname and port)
     * @param clientConfiguration Configuration for the client (login parameters or session Id)
     * @return a connected WebXTunnel
     * @throws WebXConnectionException thrown if the connection fails
     */
    public static WebXTunnel Connect(final WebXHostConfiguration hostConfiguration, final WebXClientConfiguration clientConfiguration) throws WebXConnectionException {
        WebXTunnel tunnel = new WebXTunnel();
        tunnel.connect(hostConfiguration, clientConfiguration);
        return tunnel;
    }

    /**
     * Default constructor of a tunnel
     */
    public WebXTunnel() {
    }

    /**
     * Connects to a WebX Engine on a specific host/port and connects the client to a WebX Session.
     * The connection parameters determine whether a new session is created or connection is required to a session that is already running.
     * @param hostConfiguration Configuration for the WebX Host (eg hostname and port)
     * @param clientConfiguration Configuration for the client (login parameters or session Id)
     * @throws WebXConnectionException thrown if the connection fails
     */
    public void connect(final WebXHostConfiguration hostConfiguration, final WebXClientConfiguration clientConfiguration) throws WebXConnectionException {
        if (this.client == null) {
            this.host = WebXRelay.getInstance().connectToHost(hostConfiguration);

            try {
                logger.debug("Creating client for {}...", this.host.getHostname());
                this.client = this.host.onClientConnection(clientConfiguration);

                // Send the connection message to the client
                this.client.onMessage(new Message.ConnectionMessage());
                logger.info("... client created.");

            } catch (WebXConnectionException error) {
                logger.info("... client connection failed: {}", error.getMessage());
                // Cleanup after connection failure
                this.host.cleanupSessions();
                WebXRelay.getInstance().onClientDisconnect(this.host);
                throw error;
            }
        }
    }

    /**
     * Returns the connection Id of the client: corresponds to the session Id of the WebX Engine
     * @return The connection Id of the client: corresponds to the session Id of the WebX Engine
     * @throws WebXClientException thrown if the tunnel is not connected
     */
    public String getConnectionId() throws WebXClientException {
        if (this.client != null) {
            return this.client.getSessionId().hexString();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    /**
     * Disconnects the client from the WebX Session: message sent to the WebX Engine do disconnect the client.
     * Client removed from the session.
     * Session closed if no more connected clients (session pinging will be halted accordingly)
     * Host disconnected if no sessions are running on the specific host.
     */
    public void disconnect() {
        if (this.client != null) {
            this.host.onClientDisconnected(client);

            WebXRelay.getInstance().onClientDisconnect(this.host);
        }
    }

    /**
     * Returns true if the client is connected
     * @return True if the client is connected.
     */
    public boolean isConnected() {
        if (this.client != null) {
            return this.client.isConnected();

        } else {
            return false;
        }
    }

    /**
     * Blocking call to get next message from the Client. The client stores all messages in a queue liberating the ZMQ thread as quickly as possible.
     * Client applications must read the messages from the queue and send them to the client.
     * The read method is blocking and returns only when a message is available.
     * @return The byte array data for the next message
     * @throws WebXClientException thrown when an error occurs with the client or if an error is detected in the message data
     * @throws WebXConnectionInterruptException thrown when the read is interrupted for example the session doesn't respond to a ping
     * @throws WebXDisconnectedException thrown when the client is disconnected from the server
     */
    public byte[] read() throws WebXClientException, WebXConnectionInterruptException, WebXDisconnectedException {
        if (this.client != null) {
            try {
                return this.client.getMessage();

            } catch (WebXDisconnectedException exception) {
                this.disconnect();
                throw exception;
            }

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    /**
     * Writes data to the WebX Engine, sending instructions from the client.
     * The instruction is queued and the client thread is liberated quickly.
     * Instruction headers are automatically modified to include the session Id and the Client Id so that they are correctly routed and controlled in the server.
     * @param payload The instruction data to send to the WebX Engine.
     * @throws WebXClientException Thrown if the client is in error
     */
    public void write(byte[] payload) throws WebXClientException {
        if (this.client != null) {
            this.client.sendInstruction(payload);

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }
}
