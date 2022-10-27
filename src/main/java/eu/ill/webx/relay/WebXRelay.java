package eu.ill.webx.relay;

import eu.ill.webx.WebXConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final List<Host> hosts = new ArrayList<>();

    public WebXRelay() {
    }

    public synchronized Host onClientConnect(final WebXConfiguration configuration) {
        Host host = this.getHost(configuration);
        if (host == null) {
            // Create host
            host = new Host(configuration);

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

    public synchronized void onClientDisconnect(Host host) {
        if (this.hosts.contains(host)) {
            if (host.getClientCount() == 0) {
                // Remove from list
                this.hosts.remove(host);

                // Disconnect from host
                host.stop();
            }
        }
    }

    private Host getHost(WebXConfiguration configuration) {
        // Find host that is already running
        Optional<Host> hostOptional = this.hosts.stream()
                .filter(aHost -> aHost.getHostname().equals(configuration.getHostname()) && aHost.getPort() == configuration.getPort())
                .findFirst();

        return hostOptional.orElse(null);
    }
}
