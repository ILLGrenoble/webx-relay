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

import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQException;

import java.util.Date;

/**
 * The ClientConnector provides an interface to the REP-REQ ZMQ socket to make requests to the WebX Router or Engine.
 * It's the main entry point to the connection, obtaining ports for the remaining sockets, client connection and disconnection requests.
 */
public class ClientConnector {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnector.class);

    private final int socketTimeoutMs;
    private final int socketRetries;
    private final boolean standalone;

    private ZMQ.Socket socket;
    private ConnectionData connectionData;
    private ZContext context;
    private Poller poller;
    private String address;

    /**
     * Default constructor
     * @param socketTimeoutMs The timeout for all requests
     * @param socketRetries The number of times to retry a request when timeout occurs
     * @param standalone specifies whether the connection is directly to a standalone engine or to a router
     */
    ClientConnector(int socketTimeoutMs, int socketRetries, boolean standalone) {
        this.socketTimeoutMs = socketTimeoutMs;
        this.socketRetries = socketRetries;
        this.standalone = standalone;
    }

    /**
     * Connects to the Webx Router or Engine.
     * If connecting directly to the engine (standalone) sends a command to obtain subscriber and publisher socket ports
     * If connecting to the router the same command also sends the port of the session channel and the public key for encryption
     * @param context The ZMQ context
     * @param address The address of the client connector socket
     * @return The connection data for the other sockets
     * @throws WebXDisconnectedException thrown if the connection fails
     */
    synchronized ConnectionData connect(ZContext context, String address) throws WebXDisconnectedException {

        if (this.socket == null) {
            this.context = context;
            this.poller = this.context.createPoller(1);
            this.address = address;

            this.makeSocket();

            try {
                String commResponse = this.sendRequest("comm").toString();
                String[] data = commResponse.split(",");

                final int publisherPort = Integer.parseInt(data[0]);
                final int collectorPort = Integer.parseInt(data[1]);

                if (standalone) {
                    this.connectionData = new ConnectionData(publisherPort, collectorPort);

                } else {
                    final int sessionPort = Integer.parseInt(data[2]);
                    final String publicKey = data[3];
                    this.connectionData = new ConnectionData(publisherPort, collectorPort, sessionPort, publicKey);
                }

                logger.debug("WebX Connector connected");

            } catch (WebXDisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new WebXDisconnectedException();
            }
        }

        return this.connectionData;
    }

    /**
     * Disconnects from the ZQM socket and unregisters if from the poller
     */
    synchronized void disconnect() {
        if (this.socket != null) {
            this.poller.unregister(this.socket);
            this.socket.close();
            this.socket = null;

            if (this.connectionData != null) {
                this.connectionData = null;
                logger.debug("WebX Connector disconnected");
            }
        }
    }

    /**
     * Builds the socket connection to the server and registers it in the poller
     */
    private synchronized void makeSocket() {
        if (this.socket == null) {
            this.socket = this.context.createSocket(SocketType.REQ);
            this.socket.setLinger(0);

            this.socket.connect(this.address);
            poller.register(this.socket, Poller.POLLIN);
        }
    }

    /**
     * Sends a synchronous command to the client connector
     * @param request the command data
     * @return the socket response
     * @throws WebXCommunicationException thrown if the request fails
     * @throws WebXDisconnectedException thrown if the server is not connected
     */
    synchronized SocketResponse sendRequest(String request) throws WebXCommunicationException, WebXDisconnectedException {
        if (this.socket != null) {
            try {
                Date requestDate = new Date();
                this.socket.send(request);

                int retriesLeft = this.socketRetries;
                while (retriesLeft > 0) {
                    //  Poll socket for a reply, with timeout
                    int rc = poller.poll(this.socketTimeoutMs);
                    if (rc == -1) {
                        // Failed
                        throw new WebXCommunicationException("Communication with WebX Engine failed");
                    }

                    if (poller.pollin(0)) {
                        byte[] data = socket.recv();

                        Date responseDate = new Date();
                        long rtt = responseDate.getTime() - requestDate.getTime();
                        return new SocketResponse(data, rtt);

                    } else if (--retriesLeft == 0) {
                        logger.warn("Failed to communicate with WebX Engine at {}: abandoning", this.address);

                    } else {
                        logger.warn("No response from {}: reconnecting and sending request again", this.address);
                        //  Old socket is confused; close it and open a new one, then send request again
                        this.disconnect();
                        this.makeSocket();

                        requestDate = new Date();
                        this.socket.send(request);
                    }
                }

                throw new WebXCommunicationException(String.format("Failed to communicate with WebX Engine at %s", this.address));

            } catch (ZMQException e) {
                logger.warn("Caught ZMQ Exception with ClientConnector: {}", e.getMessage());
                throw new WebXCommunicationException(String.format("Failed to send request to WebX Engine: %s", e.getMessage()));
            }

        } else {
            throw new WebXDisconnectedException();
        }
    }
}
