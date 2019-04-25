package eu.ill.webx.relay;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StringEndpointCommunicator implements EndpointCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(StringEndpointCommunicator.class);

    private RemoteEndpoint remoteEndpoint;

    public StringEndpointCommunicator(RemoteEndpoint remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
    }

    @Override
    public void sendData(byte[] data) {
        this.sendData(new String(data));
    }

    public void sendData(String data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendString(new String(data));
            }

        } catch (IOException e) {
            logger.error("Failed to write string data to web socket");
        }
    }

}
