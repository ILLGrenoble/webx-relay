package eu.ill.webx.relay;


import eu.ill.webx.WebXClientConfiguration;
import eu.ill.webx.WebXEngineConfiguration;
import eu.ill.webx.WebXHostConfiguration;
import eu.ill.webx.exceptions.WebXClientConnectionException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXHostConnectionException;
import eu.ill.webx.model.Message.ConnectionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class WebXParallelRelay {
    private static final Logger logger = LoggerFactory.getLogger(WebXParallelRelay.class);

    private final Map<String, WebXSyncHost> hosts = new HashMap<>();
    private final Lock hostsLock = new ReentrantLock();

    /**
     * Private constructor
     */
    private WebXParallelRelay() {
    }

    /**
     * Private static creator of a singleton instance
     */
    private static class Holder {
        private static final WebXParallelRelay INSTANCE = new WebXParallelRelay();
    }

    /**
     * Returns the singleton instance
     * @return the singleton WebXRelay instance
     */
    public static WebXParallelRelay getInstance() {
        return WebXParallelRelay.Holder.INSTANCE;
    }

    /**
     * Connects to a WebX Host (if a connection hasn't already been made). The host will obtain connection ports from the
     * client connector socket and connect all ZMQ sockets to the server (either the WebX Router or a standalone WebX Engine)
     * @param hostConfiguration The host configuration (hostname, client connector port, standalone)
     * @param clientConfiguration Configuration for the client (login parameters or session Id)
     * @param engineConfiguration Configuration for the WebX Engine (converted to environment variables by the WebX Router)
     * @return a WebXClient
     * @throws WebXConnectionException thrown if the connection fails
     */
    public WebXClient connectToHost(final WebXHostConfiguration hostConfiguration, final WebXClientConfiguration clientConfiguration, final WebXEngineConfiguration engineConfiguration) throws WebXConnectionException {
        final String hostname = hostConfiguration.getHostname();

        // Get a lock on the SyncHost without blocking access to other SyncHosts.
        WebXSyncHost syncHost = null;
        do {
            // Lock the relay and clean terminated hosts
            this.lockHostsAndClean();
            syncHost = this.hosts.computeIfAbsent(hostname, k -> new WebXSyncHost(hostConfiguration));

            // Lock first the host then release the relay to accept other requests
            boolean lockHeld = syncHost.tryLock();
            if (!lockHeld) {
                syncHost = null;
            }

            this.hostsLock.unlock();

        } while (syncHost == null);

        final WebXHost host = syncHost.getHost();
        try {
            // Connect the host (does nothing if host already connected)
            host.connect();

            // Connect the client (creating session if needed)
            logger.debug("Creating client for {}...", hostname);
            WebXClient client = host.onClientConnection(clientConfiguration, engineConfiguration);

            // Send the connection message to the client (client is running/fully connected if it has a valid client identifier)
            client.onMessage(new ConnectionMessage(client.getClientIdentifier() == null));
            logger.debug("... client created.");

            return client;

        } catch (WebXHostConnectionException exception) {
            // Disconnect from host and mark SyncHost as terminated (to be removed later to avoid thread lock on hostsLock)
            syncHost.terminate();

            // Fully disconnect host if it is connected
            host.disconnect();
            logger.warn("Failed to create WebX host at {}:{} : {}", hostname, hostConfiguration.getPort(), exception.getMessage());
            throw new WebXConnectionException(String.format("Failed to connect to host: %s", exception.getMessage()));

        } catch (WebXClientConnectionException error) {
            logger.warn("Client connection failed: {}", error.getMessage());
            // Cleanup after connection failure (in a separate thread due to synchronised)
            host.cleanupSessions();

            // TODO: Check if potentially thread blocking
            this.onClientDisconnected(syncHost);

            throw new WebXConnectionException(error.getMessage());

        } finally {
            // Unlock the host
            syncHost.unlock();
        }
    }


    /**
     * Called when a client disconnects so that we can perform cleanup operations (close the host connection if no clients are connected)
     * @param client the client to disconnect
     * @param hostname the hostname of the instance
     */
    public void disconnectFromHost(final WebXClient client, final String hostname) {

        // Get a lock on the SyncHost (if it exists) without blocking access to other SyncHosts.
        WebXSyncHost syncHost = null;
        boolean hostExists = false;
        do {
            // Lock the relay and clean terminated hosts
            this.lockHostsAndClean();
            syncHost = this.hosts.get(hostname);
            if (syncHost != null) {
                hostExists = true;

                // Lock first the host then release the relay to accept other requests
                boolean lockHeld = syncHost.tryLock();
                if (!lockHeld) {
                    syncHost = null;
                }
            }

            this.hostsLock.unlock();

        } while (syncHost == null && hostExists);

        if (!hostExists) {
            return;
        }

        try {
            WebXHost host = syncHost.getHost();

            // Disconnect the client
            logger.debug("Disconnecting client from {}...", hostname);
            host.onClientDisconnected(client);
            logger.debug("... client disconnected.");

        } catch(Exception e) {
            logger.warn("Error while disconnecting from {}: {}", hostname, e.getMessage());

        } finally {
            // Cleanup after client disconnect
            this.onClientDisconnected(syncHost);

            // Unlock the host
            syncHost.unlock();
        }

        // Cleanup terminated hosts
        this.lockHostsAndClean();
        logger.debug("Hosts remaining = {}", this.hosts.size());
        this.hostsLock.unlock();
    }

    /**
     * Checks if the underlying host has any clients attached to it. If no clients
     * remain then the host disconnects from the server and is removed from the hosts map.
     * @param syncHost The synchronised host which has had a client disconnected
     */
    private void onClientDisconnected(final WebXSyncHost syncHost) {
        // Lock the host (should already be the case)
        syncHost.lock();

        try {
            final WebXHost host = syncHost.getHost();

            // Disconnect from host and mark SyncHost as terminated (to be removed later to avoid thread lock on hostsLock)
            if (host.getClientCount() == 0) {
                syncHost.terminate();

                // Disconnect from host
                host.disconnect();
            }

        } finally {
            // Unlock the host
            syncHost.unlock();
        }
    }

    /**
     * Locks the relay (access to the hosts map) and cleans up any terminated SyncHosts (removes
     * them from the map).
     * NOTE: The hostsLock MUST be unlocked at some point after calling this!
     */
    private void lockHostsAndClean() {
        this.hostsLock.lock();
        List<String> terminatedHostnames = this.hosts.values().stream()
                .filter(WebXSyncHost::isTerminated)
                .map(syncHost -> syncHost.getHost().getHostname())
                .toList();

        for (String hostname : terminatedHostnames) {
            this.hosts.remove(hostname);
        }
    }

}
