package eu.ill.webx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final List<WebXHost> hosts = new ArrayList<>();

    public WebXRelay() {
    }

    public synchronized WebXTunnel onClientConnect(final WebXConfiguration configuration, final WebXClientInformation clientInformation) {
        WebXHost host = this.createHost(configuration);
        if (host != null) {
            logger.debug("Creating client for {}...", host.getHostname());

            WebXTunnel tunnel = new WebXTunnel(this, host);
            if (tunnel.connect(clientInformation)) {
                return tunnel;
            }
        }

        return null;
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

    private WebXHost createHost(final WebXConfiguration configuration) {
        WebXHost host = this.getHost(configuration);
        if (host == null) {
            // Create host
            host = new WebXHost(configuration);

            // Test connection to the webx server
            if (host.start()) {
                this.hosts.add(host);

            } else {
                host.stop();
                host = null;
                logger.error("Failed to create WebX host at {}:{}", configuration.getHostname(), configuration.getPort());
            }
        }
        return host;
    }

    private WebXHost getHost(WebXConfiguration configuration) {
        // Find host that is already running
        Optional<WebXHost> hostOptional = this.hosts.stream()
                .filter(aHost -> aHost.getHostname().equals(configuration.getHostname()) && aHost.getPort() == configuration.getPort())
                .findFirst();

        return hostOptional.orElse(null);
    }
}
