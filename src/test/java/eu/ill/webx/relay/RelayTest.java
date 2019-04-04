package eu.ill.webx.relay;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.WebXConnectorTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class RelayTest {

    private static final Logger logger = LoggerFactory.getLogger(WebXConnectorTest.class);

    @BeforeClass
    static public void connect() {
        WebXConnector.instance().connect("localhost", 5555);
    }

    @AfterClass
    static public void disconnect() {
        WebXConnector.instance().disconnect();
    }

    @Test
    public void testRelayStartSTop() {
        Relay relay = new Relay(null);
        relay.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        relay.stop();
        assertFalse(relay.isRunning());
        assertNull(relay.getWebXListenerThread());
    }

    @Test
    public void testRelaySubscriber() {
        Relay relay = new Relay(null);
        relay.start();
        WebXConnector.instance().getSubscriber().addListener(relay);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
