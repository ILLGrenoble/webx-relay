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

public class ConnectionData {

    private final int publisherPort;
    private final int subscriberPort;
    private final int sessionPort;
    private final String serverPublicKey;

    public ConnectionData(int publisherPort, int subscriberPort) {
        this.publisherPort = publisherPort;
        this.subscriberPort = subscriberPort;
        this.sessionPort = 0;
        this.serverPublicKey = null;
    }

    public ConnectionData(int publisherPort, int subscriberPort, int sessionPort, String serverPublicKey) {
        this.publisherPort = publisherPort;
        this.subscriberPort = subscriberPort;
        this.sessionPort = sessionPort;
        this.serverPublicKey = serverPublicKey;
    }

    public int getPublisherPort() {
        return publisherPort;
    }

    public int getSubscriberPort() {
        return subscriberPort;
    }

    public int getSessionPort() {
        return sessionPort;
    }

    public String getServerPublicKey() {
        return serverPublicKey;
    }
}
