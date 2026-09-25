/*
 * WebX Relay
 * Copyright (C) 2023 Institut Laue-Langevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ill.webx.relay;


import eu.ill.webx.WebXHostConfiguration;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The SyncHost encapsulates a Host and a Reentrant Lock to ensure synchronised access to the host from the WebXRelay.
 * It helps ensure that connections and disconnections occur sequentially for a given host (including cleanup
 * operations that occur as a result of a session error).
 */
public class WebXSyncHost {

    private final WebXHost host;
    private final Lock lock = new ReentrantLock();
    private boolean terminated = false;

    /**
     * The constructor of the SyncHost. This will construct the underlying WebXHost object.
     * @param hostConfiguration The host configuration
     */
    public WebXSyncHost(WebXHostConfiguration hostConfiguration) {
        this.host = new WebXHost(hostConfiguration, this::onSessionError);
    }

    /**
     * Returns the host
     * @return the host
     */
    public WebXHost getHost() {
        return host;
    }

    /**
     * Acquires the lock to the host, blocking until it is available
     */
    public void lock() {
        lock.lock();
    }

    /**
     * Acquires the lock to the host and returns true or returns false if the lock isn't acquired
     * @return true if the lock is acquired
     */
    public boolean tryLock() {
        return lock.tryLock();
    }

    /**
     * Releases the lock
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * Returns true if the host has terminated (fully disconnected)
     * @return true if the host has terminated
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Marks the host as terminated, ready for removal from the hosts map in the relay
     */
    public void terminate() {
        this.terminated = true;
    }

    /**
     * Checks if the underlying host has any clients attached to it. If no clients
     * remain then the host disconnects from the server and is marked as terminated, ready for removal from the hosts map in the relay
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
