package eu.ill.webxdemo.ws;

import eu.ill.webxdemo.Configuration;
import eu.ill.webx.relay.WebXRelay;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WebSocketTunnelServlet extends WebSocketServlet {

    private final WebXRelay relay;
    private final Configuration configuration;

    @Inject
    public WebSocketTunnelServlet(final WebXRelay relay,
                                  final Configuration configuration) {
        this.relay = relay;
        this.configuration = configuration;
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {

        // Remove permessage-deflate extension to otherwise messages above 8192B are cut and an unnecessary copy of data is performed
        factory.getExtensionFactory().unregister("permessage-deflate");

        // Register WebSocket implementation
        factory.setCreator((request, response) -> new WebSocketTunnelListener(this.relay, this.configuration));
    }
}
