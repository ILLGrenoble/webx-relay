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
package eu.ill.webx;

import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;


public class WebXHostConnectionChecker extends Thread {

    private static final int FAST_PING_MS = 1000;
    private static final int SLOW_PING_MS = 5000;

    public interface OnErrorHandler { void onError(String error); }

    private static final Logger logger = LoggerFactory.getLogger(WebXHostConnectionChecker.class);

    private final Transport transport;
    private boolean running = true;
    private int pingInterval = SLOW_PING_MS;
    private boolean pingReceived = false;

    private final OnErrorHandler onErrorHandler;


    public WebXHostConnectionChecker(final Transport transport, final OnErrorHandler onErrorHandler) {
        this.transport = transport;
        this.onErrorHandler = onErrorHandler != null ? onErrorHandler : error -> {};
    }

    public void interrupt() {
        running = false;
        super.interrupt();
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                // Ping on session channel to ensure all is ok (ensures encryption keys are valid too)
                logger.trace("Sending router ping to {}", this.transport.getHostname());
                this.transport.sendRequest("ping");

                this.pingReceived = true;

            } catch (WebXDisconnectedException e) {
                logger.error("Failed to get response from connector ping at {}", this.transport.getHostname());

                this.onErrorHandler.onError(e.getMessage());
            }

            try {
                Thread.sleep(this.pingInterval);

            } catch (InterruptedException ignored) {
            }
        }
    }

    public boolean waitForPing() {
        // Wait for a ping to ensure comms have been set up
        long startTime = new Date().getTime();
        long delay = 0;
        this.pingInterval = FAST_PING_MS;
        while (delay < 5000 && !this.pingReceived) {
            try {
                Thread.sleep(100);
                delay = new Date().getTime() - startTime;

            } catch (InterruptedException ignored) {
            }
        }
        this.pingInterval = SLOW_PING_MS;

        return this.pingReceived;
    }
}
