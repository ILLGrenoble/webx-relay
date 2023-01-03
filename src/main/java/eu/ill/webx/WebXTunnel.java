package eu.ill.webx;

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebXTunnel {

    private static final Logger logger = LoggerFactory.getLogger(WebXTunnel.class);

    private WebXHost host;
    private WebXClient client;

    public WebXTunnel() {
    }

    public void connect(final WebXConfiguration configuration) throws WebXConnectionException {
        this.connect(configuration, null);
    }

    public void connect(final WebXConfiguration configuration, final WebXClientInformation clientInformation) throws WebXConnectionException {
        if (this.client == null) {
            WebXHost host = WebXRelay.getInstance().onClientConnect(configuration);

            logger.debug("Creating client for {}...", host.getHostname());
            WebXClient client = clientInformation == null ? host.createClient() : host.createClient(clientInformation);

            logger.info("... client created.");
            this.client = client;
            this.host = host;
        }
    }

    public String getConnectionId() throws WebXClientException {
        if (this.client != null) {
            return this.client.getWebXSessionId();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public void disconnect() {
        if (this.client != null) {
            this.client.stop();
            this.host.removeClient(client);

            WebXRelay.getInstance().onClientDisconnect(this.host);
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

    public void write(byte[] payload) throws WebXClientException {
        if (this.client != null) {
            this.client.queueInstruction(payload);

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }
}
