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

import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.WebXClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.util.Z85;

import java.util.Base64;

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
     * @throws WebXDisconnectedException Thrown if the server is disconnected
     */
    synchronized SocketResponse sendRequest(String request) throws WebXDisconnectedException {
        try {
            if (this.socket != null) {
                this.socket.send(request);
                return new SocketResponse(socket.recv());

            } else {
                throw new WebXDisconnectedException();
            }

        } catch (ZMQException e) {
            logger.error("Caught ZMQ Exception: {}", e.getMessage());
            throw new WebXDisconnectedException();
        }
    }

    /**
     * Sends a request to start a new session with connection credentials
     * @param configuration The configuration for the session (login, screen size and keyboard)
     * @return The session Id string
     * @throws WebXDisconnectedException thrown if an error occurs with the socket connection
     */
    synchronized String startSession(WebXClientConfiguration configuration) throws WebXDisconnectedException {
        String usernameBase64 = Base64.getEncoder().encodeToString(configuration.getUsername().getBytes());
        String passwordBase64 = Base64.getEncoder().encodeToString(configuration.getPassword().getBytes());
        String request = "create," +
                usernameBase64 + "," +
                passwordBase64 + "," +
                configuration.getScreenWidth() + "," +
                configuration.getScreenHeight() + "," +
                configuration.getKeyboardLayout();
        return this.sendRequest(request).toString();
    }
}
