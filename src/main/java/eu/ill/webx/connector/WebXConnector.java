package eu.ill.webx.connector;

import eu.ill.webx.transport.instruction.ConnectInstruction;
import eu.ill.webx.transport.instruction.Instruction;
import eu.ill.webx.transport.message.ConnectionMessage;
import eu.ill.webx.transport.message.Message;
import eu.ill.webx.transport.serializer.BinarySerializer;
import eu.ill.webx.transport.serializer.JsonSerializer;
import eu.ill.webx.transport.serializer.Serializer;
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

    private Serializer serializer;

    private WebXMessageSubscriber messageSubscriber;
    private WebXCommandPublisher commandPublisher;

    public WebXConnector() {
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public WebXMessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public WebXCommandPublisher getCommandPublisher() {
        return commandPublisher;
    }

    public void connect(String webXServerAddress, int webXServerPort) {
        this.webXServerAddress = webXServerAddress;

        if (this.context == null) {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
            socket.connect(fullAddress);

            // Send connection request
            String serializerType = this.sendCommRequest();
            if (serializerType.equals("json")) {
                this.serializer = new JsonSerializer();

            } else if (serializerType.equals("binary")) {
                this.serializer = new BinarySerializer();
            }

            ConnectionMessage connectionResponse = (ConnectionMessage) this.sendRequest(new ConnectInstruction());
            if (connectionResponse != null) {
                this.webXCollectorPort = connectionResponse.getCollectorPort();
                this.webXPublisherPort = connectionResponse.getPublisherPort();

                this.messageSubscriber = new WebXMessageSubscriber(this.serializer, this.context, this.webXServerAddress, this.webXPublisherPort);
                this.messageSubscriber.start();

                this.commandPublisher = new WebXCommandPublisher();
                this.commandPublisher.connect(this.context, this.webXServerAddress, this.webXCollectorPort);

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

            if (this.commandPublisher != null) {
                this.commandPublisher.disconnect();
                this.commandPublisher = null;
            }

            this.context.destroy();
            this.context = null;
            logger.info("WebX Connector stopped");
        }
    }

    public boolean isConnected() {
        return this.socket != null;
    }

    public String sendCommRequest() {
        this.socket.send("comm", 0);

        byte[] responseData = socket.recv();
        String serializerType = new String(responseData);

        return serializerType;
    }

    public Message sendRequest(Instruction instruction) {
        Message response = null;
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

        byte[] responseData = socket.recv(0);

        return responseData;
    }
}
