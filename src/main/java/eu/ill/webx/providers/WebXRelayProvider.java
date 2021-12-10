package eu.ill.webx.providers;

import com.google.inject.Provider;
import eu.ill.webx.Configuration;
import eu.ill.webx.relay.WebXRelay;

import javax.inject.Inject;

public class WebXRelayProvider implements Provider<WebXRelay> {

    private final Configuration configuration;

    @Inject
    public WebXRelayProvider(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public WebXRelay get() {
        final WebXRelay relay = new WebXRelay(configuration);
        relay.run();
        return relay;
    }
}
