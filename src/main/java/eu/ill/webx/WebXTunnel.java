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

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebXTunnel {

    private static final Logger logger = LoggerFactory.getLogger(WebXTunnel.class);

    private WebXHost host;
    private WebXClient client;

    public WebXTunnel() {
    }

    public void connect(final WebXConfiguration configuration) throws WebXConnectionException {
        this.connect(configuration, null);
    }

    public void connect(final WebXConfiguration configuration, final WebXClientInformation clientInformation) throws WebXConnectionException {
        if (this.client == null) {
            WebXHost host = WebXRelay.getInstance().onClientConnect(configuration);

            logger.debug("Creating client for {}...", host.getHostname());
            WebXClient client = clientInformation == null ? host.createClient() : host.createClient(clientInformation);

            logger.info("... client created.");
            this.client = client;
            this.host = host;
        }
    }

    public String getConnectionId() throws WebXClientException {
        if (this.client != null) {
            return this.client.getSessionId().hexString();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public void disconnect() {
        if (this.client != null) {
            this.client.stop();
            this.host.removeClient(client);

            WebXRelay.getInstance().onClientDisconnect(this.host);
        }
    }

    public void start() throws WebXClientException {
        if (this.client != null) {
            this.client.start();

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public boolean isRunning() {
        if (this.client != null) {
            return this.client.isRunning();

        } else {
            return false;
        }
    }

    public byte[] read() throws WebXClientException, WebXConnectionInterruptException {
        if (this.client != null) {
            try {
                return this.client.getMessage();

            } catch (WebXDisconnectedException exception) {
                this.disconnect();
                throw new WebXClientException("Client has been disconnected by the server");
            }

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }

    public void write(byte[] payload) throws WebXClientException {
        if (this.client != null) {
            this.client.queueInstruction(payload);

        } else {
            throw new WebXClientException("Client is not connected");
        }
    }
}
