package eu.ill.webx.relay;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BinaryEndpointCommunicator implements EndpointCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(BinaryEndpointCommunicator.class);

    private RemoteEndpoint remoteEndpoint;

    public BinaryEndpointCommunicator(RemoteEndpoint remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
    }

    @Override
    public void sendData(byte[] data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendBytes(ByteBuffer.wrap(data));
            }

        } catch (IOException e) {
            logger.error("Failed to write binary data to web socket");
        }
    }
}
