package eu.ill.webx;

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebXTunnel {

    private static final Logger logger = LoggerFactory.getLogger(WebXTunnel.class);

    private final WebXRelay relay;
    private final WebXHost host;
    private WebXClient client;

    public WebXTunnel(final WebXRelay relay, final WebXHost host) {
        this.relay = relay;
        this.host = host;
    }

    protected boolean connect(final WebXClientInformation clientInformation) {
        if (this.client == null) {
            this.client = new WebXClient();
            if (host.connectClient(client, clientInformation)) {
                logger.info("... client created.");

                return true;
            } else {
                logger.warn("... not connected to server {}. Client not created.", host.getHostname());
                return false;
            }

        } else {
            return this.client.isRunning();
        }
    }

    public void disconnect() {
        if (this.client != null) {
            this.client.stop();
            this.host.removeClient(client);
            this.relay.onClientDisconnect(this.host);
        }
    }

    public void start() throws WebXClientException {
        if (this.client != null) {
            this.client.start();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public boolean isRunning() {
        if (this.client != null) {
            return this.client.isRunning();

        } else {
            return false;
        }
    }

    public byte[] read() throws WebXClientException, WebXConnectionInterruptException {
        if (this.client != null) {
            return this.client.getMessage();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public void write(byte[] payload) {
        this.client.queueInstruction(payload);
    }
}
