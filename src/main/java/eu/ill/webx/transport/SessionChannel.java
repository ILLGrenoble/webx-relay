package eu.ill.webx.transport;

import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.util.Z85;

public class SessionChannel {

    private static final Logger logger = LoggerFactory.getLogger(SessionChannel.class);

    private ZMQ.Socket socket;

    public SessionChannel() {
    }

    public void connect(ZContext context, String address, int socketTimeoutMs, String serverPublicKey) {
        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.REQ);
            this.socket.setReceiveTimeOut(socketTimeoutMs);
            this.socket.setLinger(0);

            ZMQ.Curve.KeyPair keypair = ZMQ.Curve.generateKeyPair();
            this.socket.setCurveServerKey(Z85.decode(serverPublicKey));
            this.socket.setCurveSecretKey(keypair.secretKey.getBytes());
            this.socket.setCurvePublicKey(keypair.publicKey.getBytes());

            socket.connect(address);
            logger.info("WebX Session Channel connected");
        }
    }

    public void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            logger.info("WebX Session Channel disconnected");
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

    public synchronized String startSession(String username, String password) throws DisconnectedException {
        String request = "create," + username + "," + password;
        String sessionId = this.sendRequest(request).toString();

        return sessionId;
    }

}
