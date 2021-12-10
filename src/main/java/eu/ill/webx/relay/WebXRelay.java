package eu.ill.webx.relay;

import eu.ill.webx.Configuration;
import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final Transport transport;

    private final Configuration configuration;

    // Todo: change to map when we have xsession IDs
    private final List<Client> clients = new ArrayList();

    private Thread thread;

    public WebXRelay(Configuration configuration) {
        this.configuration = configuration;

        this.transport = new Transport();

        // Todo: make the main filtering of messages from the server occur here: redirect to correct clients
    }

    public void run() {
        // Start connection checker
        this.thread = new Thread(this::connectionCheck);
        this.thread.start();
    }

    public synchronized boolean addClient(Client client) {
        if (this.transport.isConnected()) {
            this.clients.add(client);

            return client.start(this.transport);
        }

        return false;
    }

    public synchronized void removeClient(Client client) {
        client.stop();
        this.clients.remove(client);
    }

    private void connectionCheck() {
        boolean running = true;
        while (running) {
            if (this.transport.isConnected()) {
                try {
                    // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                    this.transport.getSessionChannel().sendRequest("ping");

                } catch (DisconnectedException e) {
                    logger.error("Failed to get response from connector ping");

                    this.transport.disconnect();
                    this.disconnectClients();
                }

            } else {
                try {
                    logger.info("Connecting to WebX server...");
                    this.transport.connect(this.configuration);
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
        for (Client client : clients) {
            client.getSession().close();
        }

        logger.info("Disconnected all clients");
    }
}
