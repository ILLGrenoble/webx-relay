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

    private int publisherPort;
    private int collectorPort;
    private int sessionPort;
    private String serverPublicKey;

    public ConnectionData(int publisherPort, int collectorPort) {
        this.publisherPort = publisherPort;
        this.collectorPort = collectorPort;
    }

    public ConnectionData(int publisherPort, int collectorPort, int sessionPort, String serverPublicKey) {
        this.publisherPort = publisherPort;
        this.collectorPort = collectorPort;
        this.sessionPort = sessionPort;
        this.serverPublicKey = serverPublicKey;
    }

    public int getPublisherPort() {
        return publisherPort;
    }

    public void setPublisherPort(int publisherPort) {
        this.publisherPort = publisherPort;
    }

    public int getCollectorPort() {
        return collectorPort;
    }

    public void setCollectorPort(int collectorPort) {
        this.collectorPort = collectorPort;
    }

    public int getSessionPort() {
        return sessionPort;
    }

    public void setSessionPort(int sessionPort) {
        this.sessionPort = sessionPort;
    }

    public String getServerPublicKey() {
        return serverPublicKey;
    }

    public void setServerPublicKey(String serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }
}
