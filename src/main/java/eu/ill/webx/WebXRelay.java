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

import eu.ill.webx.exceptions.WebXConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final List<WebXHost> hosts = new ArrayList<>();

    private WebXRelay(){
    }

    private static class Holder {
        private static final WebXRelay INSTANCE = new WebXRelay();
    }

    public static WebXRelay getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized WebXHost onClientConnect(final WebXConfiguration configuration) throws WebXConnectionException {
        WebXHost host = this.getHost(configuration);
        if (host == null) {
            // Create host
            host = new WebXHost(configuration);

            // Test connection to the webx server
            try {
                host.start();
                this.hosts.add(host);

            } catch (WebXConnectionException exception) {
                host.stop();
                logger.error("Failed to create WebX host at {}:{} : {}", configuration.getHostname(), configuration.getPort(), exception.getMessage());

                throw exception;
            }
        }
        return host;
    }

    public synchronized void onClientDisconnect(WebXHost host) {
        if (this.hosts.contains(host)) {
            if (host.getClientCount() == 0) {
                // Remove from list
                this.hosts.remove(host);

                // Disconnect from host
                host.stop();
            }
        }
    }

    private WebXHost getHost(WebXConfiguration configuration) {
        // Find host that is already running
        Optional<WebXHost> hostOptional = this.hosts.stream()
                .filter(aHost -> aHost.getHostname().equals(configuration.getHostname()) && aHost.getPort() == configuration.getPort())
                .findFirst();

        return hostOptional.orElse(null);
    }
}
