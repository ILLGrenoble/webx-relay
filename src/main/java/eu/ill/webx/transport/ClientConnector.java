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

import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class ClientConnector {

    private static final Logger logger = LoggerFactory.getLogger(ClientConnector.class);

    private ZMQ.Socket socket;
    private ConnectionData connectionData;

    public ClientConnector() {
    }

    public ConnectionData connect(ZContext context, String address, int socketTimeoutMs, boolean standalone) throws WebXDisconnectedException {

        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.REQ);
            this.socket.setLinger(0);
            this.socket.setReceiveTimeOut(socketTimeoutMs);

            this.socket.connect(address);

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

    public void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            if (this.connectionData != null) {
                this.connectionData = null;
                logger.debug("WebX Connector disconnected");
            }
        }
    }

    public synchronized SocketResponse sendRequest(String request) throws WebXDisconnectedException {
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
}
