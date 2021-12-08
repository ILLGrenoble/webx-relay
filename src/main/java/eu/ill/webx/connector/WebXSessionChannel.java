package eu.ill.webx.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import zmq.util.Z85;

public class WebXSessionChannel {

    private static final Logger logger = LoggerFactory.getLogger(WebXSessionChannel.class);

    private ZContext context;
    private ZMQ.Socket socket;

    public WebXSessionChannel() {
    }

    public void connect(ZContext context, String address, String serverPublicKey) {
        if (this.context == null) {
            this.context = context;
            this.socket = this.context.createSocket(SocketType.REQ);

            ZMQ.Curve.KeyPair keypair = ZMQ.Curve.generateKeyPair();
            this.socket.setCurveServerKey(Z85.decode(serverPublicKey));
            this.socket.setCurveSecretKey(keypair.secretKey.getBytes());
            this.socket.setCurvePublicKey(keypair.publicKey.getBytes());

            socket.connect(address);
            logger.info("WebX Session Channel connected");
        }
    }

    public void disconnect() {
        if (this.context != null) {
            this.socket.close();
            this.socket = null;

            this.context = null;
            logger.info("WebX Session Channel disconnected");
        }
    }

    public byte[] sendRequest(String requestData) {
        this.socket.send(requestData, 0);

        return socket.recv(0);
    }

    public byte[] sendRequest(byte[] requestData) {
        this.socket.send(requestData, 0);

        return socket.recv(0);
    }

    public String startSession(String username, String password) {
        String request = "create," + username + "," + password;
        String sessionId = new String(this.sendRequest(request));

        return sessionId;
    }

}
