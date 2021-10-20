package eu.ill.webx.relay;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class WebXDataCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(WebXDataCommunicator.class);

    private final RemoteEndpoint remoteEndpoint;

    public WebXDataCommunicator(RemoteEndpoint remoteEndpoint) {
        this.remoteEndpoint = remoteEndpoint;
    }

    public synchronized void sendData(byte[] data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendBytes(ByteBuffer.wrap(data));
            }

        } catch (IOException exception) {
            logger.error("Failed to write binary data to web socket", exception);
        }
    }
}
