package eu.ill.webx;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.ws.WebSocketTunnelServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import static com.google.inject.Guice.createInjector;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public class Application {


    @Parameter(names = {"--port"})
    private Integer port = 8080;

    @Parameter(names = {"--webxhost"})
    private String webXHost = "localhost";

    @Parameter(names = {"--webxport"})
    private Integer webXPort = 5555;


    public static void main(String... argv) throws Exception {
        final Application application = new Application();
        JCommander.newBuilder()
                .addObject(application)
                .build()
                .parse(argv);
        application.run();
    }

    private void run() throws Exception {
        final Server server = new Server(port);
        final ServletContextHandler context = new ServletContextHandler(server, "/", NO_SESSIONS);

        // Start WebXConnector (and message listener)
        WebXConnector.instance().connect(webXHost, webXPort);

        context.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return createInjector(new ServletModule() {
                    @Override
                    public void configureServlets() {
                        serve("/").with(WebSocketTunnelServlet.class);
                    }
                });
            }
        });
        context.addFilter(GuiceFilter.class, "/*", null);
        server.start();
    }
}