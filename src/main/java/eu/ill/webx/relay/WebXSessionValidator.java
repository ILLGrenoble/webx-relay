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


import eu.ill.webx.exceptions.WebXCommunicationException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Separate thread to ping a session to ensure it is still running.
 */
public class WebXSessionValidator extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(WebXSessionValidator.class);
    private static final int PING_DELAY_MS = 15000;

    /**
     * Defines an interface to handle errors produced during the ping request
     */
    interface OnErrorHandler {
        /**
         * Called when an error occurs during the ping
         * @param error the error message
         */
        void onError(String error);
    }

    private final SessionId sessionId;
    private final Transport transport;
    private final OnErrorHandler onErrorHandler;

    private boolean running = false;

    /**
     * Constructor taking the session Id, transport layer and error handler (callback function when pinging fails)
     * @param sessionId The unique session Id (used for logging)
     * @param transport The transort layer (to send synchronous ping messages)
     * @param onErrorHandler The callback when a ping fails
     */
    WebXSessionValidator(final SessionId sessionId, final Transport transport, final OnErrorHandler onErrorHandler) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.onErrorHandler = onErrorHandler != null ? onErrorHandler : error -> {};
    }

    /**
     * Returns true when running
     * @return true when running
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Starts the session validator thread
     */
    @Override
    public void start() {
        if (!this.running) {
            this.running = true;
            super.start();
        }
    }

    /**
     * Interrupts the session validator thread
     */
    @Override
    public void interrupt() {
        if (this.running) {
            this.running = false;
            super.interrupt();
        }
    }

    /**
     * Main method called when the Thread executes. The thread will send a ping request to the WebX Engine and wait for a response. If no response
     * is received before the timeout value then the error callback is called.
     * the ping is sent to the WebX Engine every 15 seconds.
     */
    @Override
    public void run() {
        while (this.running) {
            try {
                Thread.sleep(PING_DELAY_MS);

                if (this.running) {
                    try {
                        logger.trace("Sending ping to session {}", this.sessionId.hexString());
                        SocketResponse response = this.transport.sendRequest("ping," + this.sessionId.hexString());

                        if (response.toString() == null) {
                            this.onErrorHandler.onError(String.format("Failed to ping webX Session %s", this.sessionId.hexString()));

                        } else {
                            String[] responseElements = response.toString().split(",");

                            if (responseElements[0].equals("pang")) {
                                this.onErrorHandler.onError(String.format("Failed to ping webX Session %s: %s", this.sessionId.hexString(), responseElements[2]));
                            }
                        }

                    } catch (WebXCommunicationException e) {
                        this.onErrorHandler.onError(String.format("Failed to communicate with the WebX Server when sending ping to session %s", this.sessionId.hexString()));

                    } catch (WebXDisconnectedException e) {
                        this.onErrorHandler.onError(String.format("Failed to get response from connector ping to session %s", this.sessionId.hexString()));
                    }
                }

            } catch (InterruptedException ignored) {
            }
        }
    }
}
