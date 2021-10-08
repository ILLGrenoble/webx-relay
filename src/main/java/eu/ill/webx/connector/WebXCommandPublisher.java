package eu.ill.webx.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class WebXCommandPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WebXCommandPublisher.class);

    private ZContext context;
    private ZMQ.Socket socket;

    public WebXCommandPublisher() {
    }

    public void connect(ZContext context, String webXServerAddress, int webXServerPort) {
        if (this.context == null) {
            this.context = context;
            this.socket = this.context.createSocket(SocketType.PUSH);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
            socket.connect(fullAddress);
            logger.info("WebX Command Publisher connected");
        }
    }

    public void disconnect() {
        if (this.context != null) {
            this.socket.close();
            this.socket = null;

            this.context = null;
            logger.info("WebX Command Publisher disconnected");
        }
    }

    public void sendCommandData(byte[] requestData) {
        this.socket.send(requestData, 0);
    }
}