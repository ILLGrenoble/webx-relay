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


    private WebXMessageSubscriber    messageSubscriber;
    private WebXInstructionPublisher instructionPublisher;

    public WebXConnector() {
    }


    public WebXMessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public WebXInstructionPublisher getInstructionPublisher() {
        return instructionPublisher;
    }

    public void connect(String webXServerAddress, int webXServerPort) {

        if (this.context == null) {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
            socket.connect(fullAddress);
            String commResponse = this.sendCommRequest();
            String[] ports = commResponse.split(",");

            final int publisherPort = Integer.parseInt(ports[0]);
            final int collectorPort = Integer.parseInt(ports[1]);

            this.messageSubscriber = new WebXMessageSubscriber(this.context, webXServerAddress, publisherPort);
            this.messageSubscriber.start();

            this.instructionPublisher = new WebXInstructionPublisher();
            this.instructionPublisher.connect(this.context, webXServerAddress, collectorPort);

            logger.info("WebX Connector started");

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
