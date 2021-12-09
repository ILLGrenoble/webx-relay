package eu.ill.webx.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class WebXInstructionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WebXInstructionPublisher.class);

    private ZContext context;
    private ZMQ.Socket socket;

    public WebXInstructionPublisher() {
    }

    public void connect(ZContext context, String address) {
        if (this.context == null) {
            this.context = context;
            this.socket = this.context.createSocket(SocketType.PUB);
            this.socket.setLinger(0);
            this.socket.connect(address);
            logger.info("WebX Instruction Publisher connected");
        }
    }

    public void disconnect() {
        if (this.context != null) {
            this.socket.close();
            this.socket = null;

            this.context = null;
            logger.info("WebX Instruction Publisher disconnected");
        }
    }

    public void sendInstructionData(byte[] requestData) {
        this.socket.send(requestData, 0);
    }
}
