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
 * Encapsulates connection data returned by the client connector and used to connect all the sockets.
 * @param publisherPort the publisher port
 * @param subscriberPort the subscriber port
 * @param sessionPort the session port (WebX Router only)
 * @param serverPublicKey the session public key of the router (WebX Router only)
 * */
public record ConnectionData(int publisherPort, int subscriberPort, int sessionPort, String serverPublicKey) {

    /**
     * Returned from a standalone WebX Engine with the ports of the publisher and subscriber sockets
     * @param publisherPort the publisher port
     * @param subscriberPort the subscriber port
     */
    public ConnectionData(int publisherPort, int subscriberPort) {
        this(publisherPort, subscriberPort, 0, null);
    }

}
