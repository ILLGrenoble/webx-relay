package eu.ill.webxdemo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import eu.ill.webxdemo.providers.WebXRelayProvider;
import eu.ill.webx.relay.WebXRelay;
import eu.ill.webxdemo.services.AuthService;
import eu.ill.webxdemo.services.ConfigurationService;
import eu.ill.webxdemo.ws.WebSocketTunnelServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.inject.Singleton;

import static com.google.inject.Guice.createInjector;
import static org.eclipse.jetty.servlet.ServletContextHandler.NO_SESSIONS;

public class Application {

    @Parameter(names = {"--port"})
    private Integer port = 8080;

    @Parameter(names = {"--standalone-host"})
    private String standaloneHost = null;

    @Parameter(names = {"--standalone-port"})
    private Integer standalonePort = 5555;

    @Parameter(names = {"--width"})
    private int defaultScreenWidth = 1440;

    @Parameter(names = {"--height"})
    private int defaultScreenHeight = 900;

    @Parameter(names = {"--keyboard"})
    private String defaultKeyboardLayout = "gb";

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

        ServletHolder servletHolder = context.addServlet(ServletContainer.class, "/api/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages", "eu.ill.webxdemo.controllers");
        servletHolder.setInitParameter("jersey.config.server.wadl.disableWadl", "true");

        context.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return createInjector(new ServletModule() {
                    @Override
                    public void configureServlets() {
                        Configuration configuration = new Configuration(standaloneHost, standalonePort, defaultScreenWidth, defaultScreenHeight, defaultKeyboardLayout);
                        ConfigurationService.instance().setConfiguration(configuration);
                        bind(Configuration.class).toInstance(configuration);
                        bind(WebXRelay.class).toProvider(WebXRelayProvider.class).in(Singleton.class);
                        serve("/ws").with(WebSocketTunnelServlet.class);
                    }
                });
            }
        });
        context.addFilter(GuiceFilter.class, "/*", null);
        AuthService.instance().start();
        server.start();
    }
}
