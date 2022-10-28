package eu.ill.webx;

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebXTunnel {

    private static final Logger logger = LoggerFactory.getLogger(WebXTunnel.class);

    private WebXHost host;
    private WebXClient client;

    public WebXTunnel() {
    }

    public boolean connect(final WebXConfiguration configuration, final WebXClientInformation clientInformation) {
        if (this.client == null) {
            WebXHost host = WebXRelay.getInstance().onClientConnect(configuration, clientInformation);

            if (host != null) {
                logger.debug("Creating client for {}...", host.getHostname());
                WebXClient client;
                if ((client = host.createClient(clientInformation)) != null) {
                    logger.info("... client created.");

                    this.client = client;
                    this.host = host;
                    return true;

                } else {
                    logger.warn("... not connected to server {}. Client not created.", host.getHostname());
                    return false;
                }
            }

            return false;

        } else {
            return this.client.isRunning();
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

    public void write(byte[] payload) {
        this.client.queueInstruction(payload);
    }
}
