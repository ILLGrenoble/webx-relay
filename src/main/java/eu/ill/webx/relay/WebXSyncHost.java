package eu.ill.webx.relay;


import eu.ill.webx.WebXHostConfiguration;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebXSyncHost {

    private final WebXHost host;
    private final Lock lock = new ReentrantLock();
    private boolean terminated = false;

    public WebXSyncHost(WebXHostConfiguration hostConfiguration) {
        this.host = new WebXHost(hostConfiguration, this::onSessionError);
    }

    public WebXHost getHost() {
        return host;
    }

    public void lock() {
        lock.lock();
    }

    public boolean tryLock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() {
        this.terminated = true;
    }

    /**
     * Checks if the underlying host has any clients attached to it. If no clients
     * remain then the host disconnects from the server and is removed from the hosts map.
     */
    public void checkClientCountAndDisconnectIfTerminated() {
        try {
            // Lock the host (if not already the case)
            this.lock();

            if (this.terminated) {
                return;
            }

            // Disconnect from host and mark SyncHost as terminated (to be removed later to avoid thread lock on hostsLock)
            if (host.getClientCount() == 0) {
                // Disconnect from host
                host.disconnect();

                // Set as terminated
                this.terminated = true;
            }

        } finally {
            // Unlock the host
            this.unlock();
        }
    }

    /**
     * Callback when a session is in error. This will close all clients for the session in this host and remove the session.
     * This SyncHost is noted as terminated to be cleanup up by the relay
     * @param session the session that is in error
     */
    private void onSessionError(final WebXSession session) {
        try {
            this.lock();

            this.host.closeAndRemoveSession(session);
            this.checkClientCountAndDisconnectIfTerminated();

        } finally {
            this.unlock();
        }
    }
}
