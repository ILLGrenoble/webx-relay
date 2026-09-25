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
import eu.ill.webx.model.*;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Separate thread to ping a session to ensure it is still running.
 */
public class WebXSessionValidator extends Thread {

    private enum State {
        PENDING,
        RUNNING,
        HANDLING_ERROR,
        TERMINATED,
    }

    private static final Logger logger = LoggerFactory.getLogger(WebXSessionValidator.class);
    private static final int CREATION_STATE_DELAY_MS = 500;
    private static final int PING_DELAY_MS = 5000;

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

    /**
     * Defines an interface to process session creation status data
     */
    interface OnCreationStatusUpdateHandler {
        /**
         * Called when we update the creation status
         * @param creationStatus the current creation status
         */
        void onCreationStatusUpdate(SessionCreation.CreationStatus creationStatus);
    }

    private final SessionId sessionId;
    private final Transport transport;
    private SessionCreation.CreationStatus creationStatus;
    private final OnCreationStatusUpdateHandler onCreationStatusUpdateHandler;
    private final OnErrorHandler onErrorHandler;
    private final PingResponseHandler pingResponseHandler;

    private State state = State.PENDING;

    /**
     * Constructor taking the session Id, transport layer and error handler (callback function when pinging fails)
     * @param sessionId The unique session Id (used for logging)
     * @param transport The transport layer (to send synchronous ping messages)
     * @param creationStatus The initial session creation status
     * @param onCreationStatusUpdateHandler The callback when we obtain a new status value
     * @param onErrorHandler The callback when communication fails
     * @param pingResponseHandler The ping response handler (handles ping response data)
     */
    WebXSessionValidator(final SessionId sessionId,
                         final Transport transport,
                         final SessionCreation.CreationStatus creationStatus,
                         final OnCreationStatusUpdateHandler onCreationStatusUpdateHandler,
                         final OnErrorHandler onErrorHandler,
                         final PingResponseHandler pingResponseHandler) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.creationStatus = creationStatus;
        this.onCreationStatusUpdateHandler = onCreationStatusUpdateHandler;
        this.onErrorHandler = onErrorHandler != null ? onErrorHandler : error -> {};
        this.pingResponseHandler = pingResponseHandler;
    }

    /**
     * Starts the session validator thread
     */
    @Override
    public void start() {
        if (this.state == State.PENDING) {
            this.state = State.RUNNING;
            super.start();
        }
    }

    /**
     * Interrupts the session validator thread
     */
    @Override
    public void interrupt() {
        if (this.state == State.RUNNING || this.state == State.HANDLING_ERROR) {
            this.state = State.TERMINATED;
            super.interrupt();
        }
    }

    /**
     * Main method called when the Thread executes to either ping an engine or update the session creation status
     * If no response is received before the timeout value then the error callback is called.
     */
    @Override
    public void run() {
        while (this.state == State.RUNNING) {
            try {
                if (this.creationStatus != SessionCreation.CreationStatus.RUNNING) {
                    this.updateCreationStatus();

                } else {
                    this.doPing();
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Sends a ping to the WebX Engine. The ping is sent to the WebX Engine every 15 seconds.
     * @throws InterruptedException if the sleep fails
     */
    private void doPing() throws InterruptedException {
        Date now = new Date();
        while (new Date().getTime() - now.getTime() < PING_DELAY_MS && this.state == State.RUNNING) {
            Thread.sleep(100);
        }

        if (this.state == State.RUNNING) {
            try {
                logger.trace("Sending ping to session {}", this.sessionId.hexString());
                SocketResponse response = this.transport.sendRequest("ping," + this.sessionId.hexString());

                if (response.toString() == null) {
                    this.onError(String.format("Failed to ping WebX Session %s", this.sessionId.hexString()));

                } else {
                    String[] responseElements = response.toString().split(",");

                    if (responseElements[0].equals("pang")) {
                        this.onError(String.format("Failed to ping WebX Session %s: %s", this.sessionId.hexString(), responseElements[2]));
                    } else {
                        this.pingResponseHandler.onPingResponse(new PingResponseData(PingResponseData.Source.SERVER, response.rttMs()));
                    }
                }

            } catch (WebXCommunicationException e) {
                this.onError(String.format("Failed to communicate with the WebX Server when sending ping to session %s", this.sessionId.hexString()));

            } catch (WebXDisconnectedException e) {
                this.onError(String.format("Failed to get response from connector ping to session %s", this.sessionId.hexString()));
            }
        }
    }

    /**
     * Requests the status of a WebX Session.
     * @throws InterruptedException if the sleep fails
     */
    private void updateCreationStatus() throws InterruptedException {
        Date now = new Date();
        while (new Date().getTime() - now.getTime() < CREATION_STATE_DELAY_MS && this.state == State.RUNNING) {
            Thread.sleep(100);
        }

        if (this.state == State.RUNNING) {
            try {
                logger.trace("Requesting status of session {}", this.sessionId.hexString());
                SessionStatusResponse response = new SessionStatusResponse(this.transport.sendRequest("status," + this.sessionId.hexString()));

                switch (response.getStatus()) {
                    case EMPTY -> {
                        // Empty response: probably due to the fact that the router cannot do async creation and the status command doesn't exist

                        // Have to assume the session is running
                        logger.info("Failed to get response from status command. Assuming router does not support async creation and that session {} is running", sessionId.hexString());
                        this.creationStatus = SessionCreation.CreationStatus.RUNNING;
                        this.onCreationStatusUpdateHandler.onCreationStatusUpdate(this.creationStatus);
                    }
                    case ERROR -> {
                        this.onError(String.format("Invalid response from WebX Server when requesting status of session %s: %s", this.sessionId.hexString(), response));
                    }
                    case RUNNING -> {
                        logger.info("Session {} is now running", sessionId);
                        this.creationStatus = response.getCreationStatus();
                        this.onCreationStatusUpdateHandler.onCreationStatusUpdate(creationStatus);
                    }
                    case STARTING -> {
                        this.creationStatus = response.getCreationStatus();
                        this.onCreationStatusUpdateHandler.onCreationStatusUpdate(creationStatus);
                    }
                }

            } catch (WebXCommunicationException e) {
                this.onError(String.format("Failed to communicate with the WebX Server when requesting status of session %s", this.sessionId.hexString()));

            } catch (WebXDisconnectedException e) {
                this.onError(String.format("Failed to get response from status request of session %s", this.sessionId.hexString()));
            }
        }
    }


    /**
     * Called when an error occurs during the communication
     * @param error The error message
     */
    private void onError(String error) {
        this.state = State.HANDLING_ERROR;
        this.onErrorHandler.onError(error);
    }
}
