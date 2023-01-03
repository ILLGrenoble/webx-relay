package eu.ill.webx.transport;

import eu.ill.webx.utils.HexString;
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
            logger.debug("WebX Instruction Publisher connected");
        }
    }

    public void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            logger.debug("WebX Instruction Publisher disconnected");
        }
    }

    public synchronized void sendInstructionData(byte[] requestData) {
        logger.trace("Sending instruction {}", HexString.toString(requestData, 32));
        this.socket.send(requestData, 0);
    }
}
