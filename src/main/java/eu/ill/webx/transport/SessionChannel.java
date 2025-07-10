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
import eu.ill.webx.model.SessionCreation;
import eu.ill.webx.model.SessionId;
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
    private enum CreationResponseCode {
        SUCCESS,
        INVALID_REQUEST_PARAMETERS,
        CREATION_ERROR,
        AUTHENTICATION_ERROR,
        UNKNOWN_ERROR;

        public static CreationResponseCode fromInteger(int x) {
            return switch (x) {
                case 0 -> SUCCESS;
                case 1 -> INVALID_REQUEST_PARAMETERS;
                case 2 -> CREATION_ERROR;
                case 3 -> AUTHENTICATION_ERROR;
                default -> UNKNOWN_ERROR;
            };
        }
    }

    private final static String ASYNC_CREATE = "create_async";
    private final static String SYNC_CREATE = "create";

    /**
     * Contains the response data from a session creation request
     * @param responseCode The respone code
     * @param payload Either sesssion Id or error message
     * @param creationStatus The creation status if an async creation is made
     */
    private record SessionCreationResponse(CreationResponseCode responseCode, String payload, SessionCreation.CreationStatus creationStatus) {
    }

    private static final Logger logger = LoggerFactory.getLogger(SessionChannel.class);

    private ZMQ.Socket socket;
    private boolean routerCanAsync = true;

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
            logger.warn("Caught ZMQ Exception: {}", e.getMessage());
            throw new WebXCommunicationException(String.format("Failed to send request to WebX Router: %s", e.getMessage()));
        }
    }

    /**
     * Legacy connection method: Sends a request to start a new session with connection credentials
     * @param clientConfiguration The configuration for the session (login, screen size and keyboard)
     * @return a SessionCreation object containing a unique Session Id and the creation status
     * @throws WebXCommunicationException thrown if an error occurs with the socket connection
     * @throws WebXDisconnectedException thrown if the server is not connected
     */
    synchronized SessionCreation startSession(final WebXClientConfiguration clientConfiguration) throws WebXCommunicationException, WebXDisconnectedException, WebXConnectionException {
        final String clientConfigurationConnectionString = clientConfiguration.connectionString();
        final String request = String.format("%s,%s", this.routerCanAsync ? ASYNC_CREATE : SYNC_CREATE, clientConfigurationConnectionString);

        SocketResponse response = this.sendRequest(request);

        // Check for empty response (command unknown) and retry
        if (response.isEmpty() && this.routerCanAsync) {
            logger.debug("Response from async creation was empty: assuming legacy WebX Router and attempting synchronous creation command");
            this.routerCanAsync = false;
            return this.startSession(clientConfiguration);
        }

        final SessionCreationResponse sessionCreationResponse = this.parseSessionCreationResponse(response);
        if (sessionCreationResponse.responseCode.equals(CreationResponseCode.SUCCESS)) {
            return new SessionCreation(new SessionId(sessionCreationResponse.payload), sessionCreationResponse.creationStatus);
        }

        throw new WebXConnectionException(String.format("Couldn't create WebX session (response code %s): %s", sessionCreationResponse.responseCode, sessionCreationResponse.payload));
    }

    /**
     * Sends a request to start a new session with connection credentials and engine configuration parameters
     * @param clientConfiguration The configuration for the session (login, screen size and keyboard)
     * @param engineConfiguration The configuration for the WebX engine
     * @return a SessionCreation object containing a unique Session Id and the creation status
     * @throws WebXCommunicationException thrown if an error occurs with the socket connection
     */
    synchronized SessionCreation startSession(final WebXClientConfiguration clientConfiguration, final WebXEngineConfiguration engineConfiguration) throws WebXCommunicationException, WebXDisconnectedException, WebXConnectionException {
        // Check for null engine configuration
        if (engineConfiguration == null) {
            return this.startSession(clientConfiguration);
        }

        final String clientConfigurationConnectionString = clientConfiguration.connectionString();
        final String engineConfigurationConnectionString = engineConfiguration.connectionString();
        String request = String.format("%s,%s,%s", this.routerCanAsync ? ASYNC_CREATE : SYNC_CREATE, clientConfigurationConnectionString, engineConfigurationConnectionString);

        SocketResponse response = this.sendRequest(request);

        // Check for empty response (command unknown) and retry
        if (response.isEmpty() && this.routerCanAsync) {
            logger.debug("Response from async creation was empty: assuming legacy WebX Router and attempting synchronous creation command");
            this.routerCanAsync = false;
            return this.startSession(clientConfiguration, engineConfiguration);
        }

        final SessionCreationResponse sessionCreationResponse = this.parseSessionCreationResponse(response);
        if (sessionCreationResponse.responseCode.equals(CreationResponseCode.SUCCESS)) {
            return  new SessionCreation(new SessionId(sessionCreationResponse.payload), sessionCreationResponse.creationStatus);

        } else if (sessionCreationResponse.responseCode.equals(CreationResponseCode.INVALID_REQUEST_PARAMETERS)) {
            logger.warn("Failed to start session with engine configuration (response code {}), using legacy connection method. NOTE: engine parameters will be ignored.", sessionCreationResponse.responseCode);
            // try legacy connection format
            return this.startSession(clientConfiguration);
        }

        throw new WebXConnectionException(String.format("Couldn't create WebX session (response code %s): %s", sessionCreationResponse.responseCode, sessionCreationResponse.payload));
    }

    /**
     * Parses the response from the WebX Router to create a session. If the response code isn't 0 then the connection is not valid.
     * @param response The socket response from the connection request
     * @return The session creation response
     * @throws WebXConnectionException Thrown if the response is invalid or an error occurs with the handling
     */
    private SessionCreationResponse parseSessionCreationResponse(SocketResponse response) throws WebXConnectionException {
        try {
            String responseString = response.toString();
            String[] responseData = responseString.split(",");
            int responseCodeValue = Integer.parseInt(responseData[0]);
            CreationResponseCode responseCode = CreationResponseCode.fromInteger(responseCodeValue);
            String data = responseData[1];

            if (responseCode == CreationResponseCode.SUCCESS) {
                SessionCreation.CreationStatus creationStatus = this.routerCanAsync ? SessionCreation.CreationStatus.fromInteger(Integer.parseInt(responseData[2])) : SessionCreation.CreationStatus.RUNNING;
                return new SessionCreationResponse(responseCode, data, creationStatus);

            } else {
                return new SessionCreationResponse(responseCode, data, SessionCreation.CreationStatus.UNKNOWN);
            }


        } catch (NullPointerException exception) {
            throw new WebXConnectionException(String.format("Failed to parse response from WebX Router: %s", exception.getMessage()));
        }
    }
}
