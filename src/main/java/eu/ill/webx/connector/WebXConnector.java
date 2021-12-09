package eu.ill.webx.connector;

import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class WebXConnector {

    private static final Logger logger = LoggerFactory.getLogger(WebXConnector.class);

    private ZContext context;
    private ZMQ.Socket socket;
    private boolean connected;

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

    public boolean isConnected() {
        return this.connected;
    }

    public void connect(String webXServerAddress, int webXServerPort) throws DisconnectedException {

        if (this.context == null) {
            this.connected = false;
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            this.socket.setLinger(0);
            this.socket.setReceiveTimeOut(15000);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;

            this.socket.connect(fullAddress);

            try {
                String commResponse = this.sendRequest("comm").toString();
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

                logger.info("WebX Connector connected");
                this.connected = true;

            } catch (DisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new DisconnectedException();
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

            if (this.sessionChannel != null) {
                this.sessionChannel.disconnect();
                this.sessionChannel = null;
            }

            if (this.connected) {
                logger.info("WebX Connector disconnected");
                this.connected = false;
            }

            this.context.destroy();
            this.context = null;
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
