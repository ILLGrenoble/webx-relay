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

public class WebXHostConfiguration {

    private final String hostname;
    private final Integer port;
    private boolean isStandalone;

    private Integer socketTimeoutMs = 15000;

    public WebXHostConfiguration(final String hostname, final Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

    public WebXHostConfiguration(final String hostname, final Integer port, boolean isStandalone) {
        this.hostname = hostname;
        this.port = port;
        this.isStandalone = isStandalone;
    }

    public WebXHostConfiguration(final String hostname, final Integer port, final Integer socketTimeoutMs) {
        this.hostname = hostname;
        this.port = port;
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public boolean isStandalone() {
        return isStandalone;
    }
}
