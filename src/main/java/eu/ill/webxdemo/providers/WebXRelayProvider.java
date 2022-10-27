package eu.ill.webxdemo.providers;

import com.google.inject.Provider;
import eu.ill.webx.relay.WebXRelay;

public class WebXRelayProvider implements Provider<WebXRelay> {

    @Override
    public WebXRelay get() {
        final WebXRelay relay = new WebXRelay();
        return relay;
    }
}
