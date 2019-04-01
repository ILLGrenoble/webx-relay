package eu.ill.webx.ws;

import com.google.inject.Singleton;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@Singleton
public class WebSocketTunnelServlet extends WebSocketServlet {



    @Override
    public void configure(final WebSocketServletFactory factory) {

        // Remove permessage-deflate extension to otherwise messages above 8192B are cut and an unnecessary copy of data is performed
        factory.getExtensionFactory().unregister("permessage-deflate");

        // Register WebSocket implementation
        factory.setCreator((request, response) -> {
            return new WebSocketTunnelListener();
        });
    }
}
