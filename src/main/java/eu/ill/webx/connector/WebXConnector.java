package eu.ill.webx.connector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.connector.response.WebXConnectionResponse;
import eu.ill.webx.connector.response.WebXResponse;
import eu.ill.webx.connector.response.WebXWindowsResponse;
import eu.ill.webx.domain.utils.Size;
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

    public int getWebXPublisherPort() {
        return webXPublisherPort;
    }

    public int getWebXCollectorPort() {
        return webXCollectorPort;
    }

    public Size getScreenSize() {
        return screenSize;
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
            WebXConnectionResponse connectionResponse = (WebXConnectionResponse)this.sendRequest(new WebXRequest(WebXRequest.Type.Connect));
            if (connectionResponse != null) {
                this.webXCollectorPort = connectionResponse.getCollectorPort();
                this.webXPublisherPort = connectionResponse.getPublisherPort();
                this.screenSize = connectionResponse.getScreenSize();

            } else {
               logger.error("Unable to establish connection to WebX server");

               this.disconnect();
            }
        }
    }

    public void disconnect() {
        if (this.context != null) {
            this.socket.close();
            this.context.destroy();

            this.socket = null;
            this.context = null;
        }
    }

    public boolean isConnected() {
        return this.socket != null;
    }

    public WebXResponse sendRequest(WebXRequest request) {
        WebXResponse response = null;
        try {
            byte[] requestData = objectMapper.writeValueAsBytes(request);
            this.socket.send(requestData, 0);

            byte[] responseData = socket.recv(0);
            if (request.getType().equals(WebXRequest.Type.Connect)) {
                response = objectMapper.readValue(responseData, WebXConnectionResponse.class);

            } else if (request.getType().equals(WebXRequest.Type.Windows)) {
                response = objectMapper.readValue(responseData, WebXWindowsResponse.class);

            }
        } catch (JsonParseException e) {
            logger.error("Error parsing JSON response");

        } catch (JsonMappingException e) {
            logger.error("Error mapping JSON response");

        } catch (IOException e) {
            logger.error("Unable to convert request to JSON");
        }

        return response;
    }
}
