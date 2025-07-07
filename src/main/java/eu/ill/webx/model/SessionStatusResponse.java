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
package eu.ill.webx.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the response from the session status request
 */
public class SessionStatusResponse {
    private static final Logger logger = LoggerFactory.getLogger(SessionStatusResponse.class);

    /**
     * Corresponding status of the response to the status command
     */
    public enum Status {
        /**
         * The response was empty (WebX Router does not support the status command)
         */
        EMPTY,
        /**
         * The response was an error (session Id doesn't exist for example)
         */
        ERROR,
        /**
         * The session is running
         */
        RUNNING,
        /**
         * The session is starting
         */
        STARTING,
    }

    private final String sessionId;
    private final Status status;
    private final SessionCreation.CreationStatus creationStatus;

    /**
     * Constructor of the SessionStatusResponse from the raw SocketResponse
     * @param response the raw socket response
     */
    public SessionStatusResponse(final SocketResponse response) {
        if (response.isEmpty()) {
            status = Status.EMPTY;
            sessionId = null;
            creationStatus = null;

        } else {
            String[] responseElements = response.toString().split(",");
            if (responseElements.length < 2) {
                status = Status.ERROR;
                sessionId = null;
                creationStatus = null;

            } else {
                this.sessionId = responseElements[0];
                String createStatusCode = responseElements[1];
                this.creationStatus = SessionCreation.CreationStatus.fromInteger(Integer.parseInt(createStatusCode));

                if (creationStatus.equals(SessionCreation.CreationStatus.RUNNING)) {
                    status = Status.RUNNING;

                } else if (creationStatus.equals(SessionCreation.CreationStatus.STARTING)) {
                    status = Status.STARTING;

                } else {
                    logger.error("Unknown session creation status: {}", createStatusCode);
                    status = Status.ERROR;
                }
            }
        }
    }

    /**
     * Returns the response status
     * @return the response status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the sessionId
     * @return the session Id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the CreationStatus
     * @return the creation status
     */
    public SessionCreation.CreationStatus getCreationStatus() {
        return creationStatus;
    }
}
