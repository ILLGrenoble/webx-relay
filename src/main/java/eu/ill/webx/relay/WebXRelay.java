package eu.ill.webx.relay;

import eu.ill.webx.connector.DisconnectedException;
import eu.ill.webx.connector.WebXConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final WebXConnector connector;

    private final String webXServerAddress;
    private final int webXServerPort;

    // Todo: change to map when we have xsession IDs
    private final List<WebXClient> clients = new ArrayList();

    private Thread thread;

    public WebXRelay(String webXServerAddress, int webXServerPort) {
        this.webXServerAddress = webXServerAddress;
        this.webXServerPort = webXServerPort;

        this.connector = new WebXConnector();

        // Todo: make the main filtering of messages from the server occur here: redirect to correct clients
    }


    public WebXConnector getConnector() {
        return connector;
    }

    public void run() {
        // Start connection checker
        this.thread = new Thread(this::connectionCheck);
        this.thread.start();
    }

    public synchronized boolean addClient(WebXClient client) {
        if (this.connector.isConnected()) {
            this.clients.add(client);

            return client.start(this.connector);
        }

        return false;
    }

    public synchronized void removeClient(WebXClient client) {
        client.stop();
        this.clients.remove(client);
    }

    private void connectionCheck() {
        boolean running = true;
        while (running) {
            if (this.connector.isConnected()) {
                try {
                    // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                    this.connector.getSessionChannel().sendRequest("ping");

                } catch (DisconnectedException e) {
                    logger.error("Failed to get response from connector ping");

                    this.connector.disconnect();
                    this.disconnectClients();
                }

            } else {
                try {
                    logger.info("Connecting to WebX server...");
                    this.connector.connect(this.webXServerAddress, this.webXServerPort);
                    logger.info("... connected");

                } catch (DisconnectedException e) {
                    // Failed to connect again
                }
            }

            try {
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                logger.warn("WebXRelay sleep connection check sleep interrupted");
            }
        }
    }

    private void disconnectClients() {
        for (WebXClient client : clients) {
            client.getSession().close();
        }

        logger.info("Disconnected all clients");
    }
}
