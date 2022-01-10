package eu.ill.webx.relay;

import eu.ill.webx.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebXRelay {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);

    private final Configuration configuration;

    private final List<Host> hosts = new ArrayList<>();

    public WebXRelay(Configuration configuration) {
        this.configuration = configuration;
    }

    public Host onClientConnect(String hostname, Integer port) {
        Host host = this.getHost(hostname, port);
        if (host == null) {
            // Create host
            host = new Host(hostname, port, configuration);

            // Test connection to the webx server
            if (host.start()) {
                this.hosts.add(host);

            } else {
                host = null;
                logger.error("Failed to create WebX host at {}:{}", hostname, port);
            }
        }

        return host;
    }

    public void onClientDisconnect(Host host) {
        if (this.hosts.contains(host)) {
            if (host.getClientCount() == 0) {
                // Disconnect from host
                host.stop();

                // Remove from list
                this.hosts.remove(host);
            }
        }
    }

    private Host getHost(String hostname, int port) {
        // Find host that is already running
        Optional<Host> hostOptional = this.hosts.stream()
                .filter(aHost -> aHost.getHostname().equals(hostname) && aHost.getPort() == port)
                .findFirst();

        return hostOptional.orElse(null);
    }
}
