package eu.ill.webx.connector;

import eu.ill.webx.transport.instruction.WebXConnectWebXInstruction;
import eu.ill.webx.transport.instruction.WebXInstruction;
import eu.ill.webx.transport.message.WebXConnectionMessage;
import eu.ill.webx.transport.message.WebXMessage;
import eu.ill.webx.transport.serializer.WebXDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class WebXConnector {

    private static final Logger logger = LoggerFactory.getLogger(WebXConnector.class);

    private ZContext context;
    private ZMQ.Socket socket;
    private String webXServerAddress;
    private int webXPublisherPort;
    private int webXCollectorPort;

    private WebXDataSerializer serializer;

    private WebXMessageSubscriber    messageSubscriber;
    private WebXInstructionPublisher instructionPublisher;

    public WebXConnector() {
    }

    public WebXDataSerializer getSerializer() {
        return serializer;
    }

    public WebXMessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public WebXInstructionPublisher getInstructionPublisher() {
        return instructionPublisher;
    }

    public void connect(String webXServerAddress, int webXServerPort) {
        this.webXServerAddress = webXServerAddress;

        if (this.context == null) {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
            socket.connect(fullAddress);
            this.serializer = new WebXDataSerializer();

            WebXConnectionMessage connectionResponse = (WebXConnectionMessage) this.sendRequest(new WebXConnectWebXInstruction());
            if (connectionResponse != null) {
                this.webXCollectorPort = connectionResponse.getCollectorPort();
                this.webXPublisherPort = connectionResponse.getPublisherPort();

                this.messageSubscriber = new WebXMessageSubscriber(this.serializer, this.context, this.webXServerAddress, this.webXPublisherPort);
                this.messageSubscriber.start();

                this.instructionPublisher = new WebXInstructionPublisher();
                this.instructionPublisher.connect(this.context, this.webXServerAddress, this.webXCollectorPort);

                logger.info("WebX Connector started");
            } else {
                logger.error("Unable to establish connection to WebX server");

                this.disconnect();
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

    public boolean isConnected() {
        return this.socket != null;
    }


    public WebXMessage sendRequest(WebXInstruction instruction) {
        WebXMessage response = null;
        byte[] requestData = serializer.serializeInstruction(instruction);

        if (requestData != null) {
            this.socket.send(requestData, 0);

            byte[] responseData = socket.recv(0);
            response = serializer.deserializeMessage(responseData);
        }

        return response;
    }

    public byte[] sendRequestData(byte[] requestData) {
        this.socket.send(requestData, 0);

        return socket.recv(0);
    }
}
