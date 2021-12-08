package eu.ill.webx.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class WebXConnector {

    private static final Logger logger = LoggerFactory.getLogger(WebXConnector.class);

    private ZContext context;
    private ZMQ.Socket socket;

    private WebXMessageSubscriber messageSubscriber;
    private WebXInstructionPublisher instructionPublisher;
    private WebXSessionChannel sessionChannel;

    public WebXConnector() {
    }


    public WebXMessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public WebXInstructionPublisher getInstructionPublisher() {
        return instructionPublisher;
    }

    public WebXSessionChannel getSessionChannel() {
        return sessionChannel;
    }

    public void connect(String webXServerAddress, int webXServerPort) {

        if (this.context == null) {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;

            socket.connect(fullAddress);

            try {
                String commResponse = this.sendCommRequest();
                String[] data = commResponse.split(",");

                final int publisherPort = Integer.parseInt(data[0]);
                final int collectorPort = Integer.parseInt(data[1]);
                final int sessionPort = Integer.parseInt(data[2]);
                final String publicKey = data[3];

                this.messageSubscriber = new WebXMessageSubscriber();
                this.messageSubscriber.start(this.context, "tcp://" + webXServerAddress + ":" + publisherPort);

                this.instructionPublisher = new WebXInstructionPublisher();
                this.instructionPublisher.connect(this.context, "tcp://" + webXServerAddress + ":" + collectorPort);

                this.sessionChannel = new WebXSessionChannel();
                this.sessionChannel.connect(this.context, "tcp://" + webXServerAddress + ":" + sessionPort, publicKey);

                logger.info("WebX Connector started");

            } catch (Exception e) {
                logger.error("Failed to connect: {}", e.getMessage());
                System.exit(1);
            }

        }
    }

    public void disconnect() {
        if (this.context != null) {
            this.socket.close();
            this.socket = null;

            if (this.messageSubscriber != null) {
                this.messageSubscriber.stop();
                this.messageSubscriber = null;
            }

            if (this.instructionPublisher != null) {
                this.instructionPublisher.disconnect();
                this.instructionPublisher = null;
            }

            this.context.destroy();
            this.context = null;
            logger.info("WebX Connector stopped");
        }
    }

    public String sendCommRequest() {
        this.socket.send("comm", 0);
        return new String(socket.recv());
    }

    public byte[] sendRequestData(byte[] requestData) {
        this.socket.send(requestData, 0);

        return socket.recv(0);
    }

}
