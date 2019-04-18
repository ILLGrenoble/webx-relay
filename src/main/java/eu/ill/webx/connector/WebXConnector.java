package eu.ill.webx.connector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.transport.message.ConnectionMessage;
import eu.ill.webx.transport.message.Message;
import eu.ill.webx.domain.utils.Size;
import eu.ill.webx.transport.instruction.Instruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;

public class WebXConnector {

    private static final Logger logger = LoggerFactory.getLogger(WebXConnector.class);

    private ObjectMapper objectMapper = new ObjectMapper();
    private ZContext context;
    private ZMQ.Socket socket;
    private String webXServerAddress;
    private int webXServerPort;
    private int webXPublisherPort;
    private int webXCollectorPort;

    private WebXSubscriber subscriber;

    private Size screenSize;

    private static WebXConnector instance = null;

    private WebXConnector() {
    }

    public static WebXConnector instance() {
        if (instance == null) {
            synchronized (WebXConnector.class) {
                if (instance == null) {
                    instance = new WebXConnector();
                }
            }
        }
        return instance;
    }

    public Size getScreenSize() {
        return screenSize;
    }

    public WebXSubscriber getSubscriber() {
        return subscriber;
    }

    public void connect(String webXServerAddress, int webXServerPort) {
        this.webXServerAddress = webXServerAddress;
        this.webXServerPort = webXServerPort;

        if (this.context == null) {
            this.context = new ZContext();
            this.socket = context.createSocket(SocketType.REQ);
            String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
            socket.connect(fullAddress);

            // Send connection request
            ConnectionMessage connectionResponse = (ConnectionMessage)this.sendRequest(new Instruction(Instruction.Type.Connect));
            if (connectionResponse != null) {
                this.webXCollectorPort = connectionResponse.getCollectorPort();
                this.webXPublisherPort = connectionResponse.getPublisherPort();
                this.screenSize = connectionResponse.getScreenSize();

                this.subscriber = new WebXSubscriber(this.context, this.webXServerAddress, this.webXPublisherPort);
                this.subscriber.start();

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

            if (this.subscriber != null) {
                this.subscriber.stop();
                this.subscriber = null;
            }

            this.context.destroy();
            this.context = null;
            logger.info("WebX Connector stopped");
        }
    }

    public boolean isConnected() {
        return this.socket != null;
    }

    public Message sendRequest(Instruction instruction) {
        Message response = null;
        try {
            byte[] requestData = objectMapper.writeValueAsBytes(instruction);
            this.socket.send(requestData, 0);

            byte[] responseData = socket.recv(0);
            response = objectMapper.readValue(responseData, Message.class);

        } catch (JsonParseException e) {
            logger.error("Error parsing JSON response for request type " + instruction.getType());

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON response for request type " + instruction.getType());

        } catch (IOException e) {
            logger.error("Unable to convert response to JSON for request type " + instruction.getType());
        }

        return response;
    }
}
