package eu.ill.webx.transport;

import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.model.DisconnectedException;
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

    public ConnectionData connect(ZContext context, String address, int socketTimeoutMs) throws DisconnectedException {

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
                final int sessionPort = Integer.parseInt(data[2]);
                final String publicKey = data[3];

                this.connectionData = new ConnectionData(publisherPort, collectorPort, sessionPort, publicKey);

                logger.info("WebX Connector connected");

            } catch (DisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new DisconnectedException();
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
                logger.info("WebX Connector disconnected");
            }
        }
    }

    public synchronized SocketResponse sendRequest(String request) throws DisconnectedException {
        try {
            if (this.socket != null) {
                this.socket.send(request);
                return new SocketResponse(socket.recv());

            } else {
                throw new DisconnectedException();
            }

        } catch (ZMQException e) {
            logger.error("Caught ZMQ Exception: {}", e.getMessage());
            throw new DisconnectedException();
        }
    }
}