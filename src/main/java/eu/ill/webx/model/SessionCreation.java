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

/**
 * The SessionCreation is used as a means of storing the response from a creation command. The creation
 * command may be asynchronous and the session creation may take a few seconds.
 * @param sessionId The id of the session
 * @param status The creation status
 */
public record SessionCreation(SessionId sessionId, CreationStatus status) {

    /**
     * Represents the state of the session creation in the WebX Router
     */
    public enum CreationStatus {
        /**
         * Session is starting
         */
        STARTING,
        /**
         * Session is running
         */
        RUNNING,
        /**
         * Session status is unknown
         */
        UNKNOWN;

        /**
         * Converts an integer value from the WebX Router into a status
         * @param x the raw response from the server
         * @return a CreationStatus object
         */
        public static CreationStatus fromInteger(int x) {
            return switch (x) {
                case 0 -> STARTING;
                case 1 -> RUNNING;
                default -> UNKNOWN;
            };
        }
    }

}
