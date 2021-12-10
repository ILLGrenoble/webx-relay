package eu.ill.webx.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class InstructionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InstructionPublisher.class);

    private ZMQ.Socket socket;

    public InstructionPublisher() {
    }

    public void connect(ZContext context, String address) {
        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.PUB);
            this.socket.setLinger(0);
            this.socket.connect(address);
            logger.info("WebX Instruction Publisher connected");
        }
    }

    public void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            logger.info("WebX Instruction Publisher disconnected");
        }
    }

    public void sendInstructionData(byte[] requestData) {
        this.socket.send(requestData, 0);
    }
}