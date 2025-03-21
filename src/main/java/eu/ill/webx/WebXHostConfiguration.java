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

/**
 * Provides host configuration parameters
 */
public class WebXHostConfiguration {

    private final String hostname;
    private final Integer port;
    private final boolean isStandalone;

    private Integer socketTimeoutMs = 15000;

    /**
     * Standard host connection parameters with hostname and port
     * @param hostname the name of the host
     * @param port the port to connect to
     */
    public WebXHostConfiguration(final String hostname, final Integer port) {
        this.hostname = hostname;
        this.port = port;
        this.isStandalone = false;
    }

    /**
     * Connection parameters including the standalone flag: Standalone server is not running with a router so connection is direct to a
     * running WebX-Engine
     * @param hostname the name of the host
     * @param port the port to connect to
     * @param isStandalone True if the host is running a standalone WebX Engine
     */
    public WebXHostConfiguration(final String hostname, final Integer port, boolean isStandalone) {
        this.hostname = hostname;
        this.port = port;
        this.isStandalone = isStandalone;
    }

    /**
     * Connection parameters including override for the default socket timeout
     * running WebX-Engine
     * @param hostname the name of the host
     * @param port the port to connect to
     * @param socketTimeoutMs The timeout in milliseconds for a timeout exception to be thrown when doing blocking ZMQ requests
     */
    public WebXHostConfiguration(final String hostname, final Integer port, final Integer socketTimeoutMs) {
        this.hostname = hostname;
        this.port = port;
        this.socketTimeoutMs = socketTimeoutMs;
        this.isStandalone = false;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @return The connector port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @return The socket timeout in milliseconds
     */
    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    /**
     * @return true if the relay is running in standalone mode
     */
    public boolean isStandalone() {
        return isStandalone;
    }
}
