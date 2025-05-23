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
import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.util.Z85;

/**
 * The Session Channel provides an encrypted socket to connect initiate and create sessions with the WebX Router.
 * For new sessions a login and password are sent and as such encryption is required. ZMQ uses the curve encryption layer.
 * The session channel is created with the servers public key. The session channel generates its own private-public key-pair
 * and sends the public key back to the server. this way 2-way encryption can be made.
 * Sessions are created with user credentials, screen size and keyboard layout parameters. On success a session Id is returned.
 */
public class SessionChannel {

    private static final Logger logger = LoggerFactory.getLogger(SessionChannel.class);

    private ZMQ.Socket socket;

    /**
     * Default constructor
     */
    SessionChannel() {
    }

    /**
     * Connects to the ZQM session channel socket of the WebX Router
     * @param context The ZMQ context
     * @param address The address of the session channel socket
     * @param socketTimeoutMs The timeout in milliseconds for responses
     * @param serverPublicKey The public key of the WebX Router
     */
    void connect(ZContext context, String address, int socketTimeoutMs, String serverPublicKey) {
        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.REQ);
            this.socket.setReceiveTimeOut(socketTimeoutMs);
            this.socket.setLinger(0);

            ZMQ.Curve.KeyPair keypair = ZMQ.Curve.generateKeyPair();
            this.socket.setCurveServerKey(Z85.decode(serverPublicKey));
            this.socket.setCurveSecretKey(keypair.secretKey.getBytes());
            this.socket.setCurvePublicKey(keypair.publicKey.getBytes());

            socket.connect(address);
            logger.debug("WebX Session Channel connected");
        }
    }

    /**
     * Disconnects from the ZQM socket
     */
    void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            logger.debug("WebX Session Channel disconnected");
        }
    }

    /**
     * Sends a synchronous request to the server
     * @param request The string request
     * @return Returns a SocketResponse
     * @throws WebXCommunicationException Thrown if there is a communication error
     * @throws WebXDisconnectedException thrown if the server is not connected
     */
    synchronized SocketResponse sendRequest(String request) throws WebXCommunicationException, WebXDisconnectedException {
        try {
            if (this.socket != null) {
                this.socket.send(request);
                return new SocketResponse(socket.recv());

            } else {
                throw new WebXDisconnectedException();
            }

        } catch (ZMQException e) {
            logger.error("Caught ZMQ Exception: {}", e.getMessage());
            throw new WebXCommunicationException(String.format("Failed to send request to WebX Router: %s", e.getMessage()));
        }
    }

    /**
     * Legacy connection method: Sends a request to start a new session with connection credentials
     * @param clientConfiguration The configuration for the session (login, screen size and keyboard)
     * @return The session Id string
     * @throws WebXCommunicationException thrown if an error occurs with the socket connection
     * @throws WebXDisconnectedException thrown if the server is not connected
     */
    synchronized String startSession(final WebXClientConfiguration clientConfiguration) throws WebXCommunicationException, WebXDisconnectedException, WebXConnectionException {
        final String clientConfigurationConnectionString = clientConfiguration.connectionString();
        final String request = String.format("create,%s", clientConfigurationConnectionString);

        SocketResponse response = this.sendRequest(request);
        return this.parseSessionCreationResponse(response);
    }

    /**
     * Sends a request to start a new session with connection credentials and engine configuration parameters
     * @param clientConfiguration The configuration for the session (login, screen size and keyboard)
     * @param engineConfiguration The configuration for the WebX engine
     * @return The session Id string
     * @throws WebXCommunicationException thrown if an error occurs with the socket connection
     */
    synchronized String startSession(final WebXClientConfiguration clientConfiguration, final WebXEngineConfiguration engineConfiguration) throws WebXCommunicationException, WebXDisconnectedException, WebXConnectionException {
        // Check for null engine configuration
        if (engineConfiguration == null) {
            return this.startSession(clientConfiguration);
        }

        try {
            final String clientConfigurationConnectionString = clientConfiguration.connectionString();
            final String engineConfigurationConnectionString = engineConfiguration.connectionString();
            String request = String.format("create,%s,%s", clientConfigurationConnectionString, engineConfigurationConnectionString);

            SocketResponse response = this.sendRequest(request);
            return this.parseSessionCreationResponse(response);

        } catch (WebXConnectionException e) {
            logger.warn("Failed to start session with engine configuration, using legacy connection method. NOTE: engine parameters will be ignored.");
            // try legacy connection format
            return this.startSession(clientConfiguration);
        }
    }

    /**
     * Parses the response from the WebX Router to create a session. If the response code isn't 0 then the connection is not valid.
     * @param response The socket response from the connection request
     * @return The session Id string
     * @throws WebXConnectionException Thrown if the response is invalid or an error occurs with the handling
     */
    private String parseSessionCreationResponse(SocketResponse response) throws WebXConnectionException {
        try {
            String responseString = response.toString();
            String[] responseData = responseString.split(",");
            int responseCode = Integer.parseInt(responseData[0]);
            if (responseCode == 0) {
                return responseData[1];

            } else {
                throw new WebXConnectionException(String.format("Couldn't create WebX session: session response invalid (response code %d)", responseCode));
            }

        } catch (NullPointerException exception) {
            throw new WebXConnectionException(String.format("Failed to parse response from WebX Router: %s", exception.getMessage()));
        }
    }
}
