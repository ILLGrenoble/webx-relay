package eu.ill.webx.providers;

import com.google.inject.Provider;
import eu.ill.webx.connector.WebXConnectorConfiguration;
import eu.ill.webx.connector.WebXConnector;

import javax.inject.Inject;

public class WebXConnectorProvider implements Provider<WebXConnector> {

    private final WebXConnectorConfiguration configuration;

    @Inject
    public WebXConnectorProvider(final WebXConnectorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public WebXConnector get() {
        final WebXConnector connector = new WebXConnector();
        connector.connect(configuration.getWebXHost(), configuration.getWebXPort());
        return connector;
    }
}
