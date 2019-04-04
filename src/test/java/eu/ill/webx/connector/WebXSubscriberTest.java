package eu.ill.webx.connector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class WebXSubscriberTest {

    private static final Logger logger = LoggerFactory.getLogger(WebXSubscriberTest.class);

    @BeforeClass
    static public void connect() {
        WebXConnector.instance().connect("localhost", 5555);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    static public void disconnect() {
        WebXConnector.instance().disconnect();
    }

    @Test
    public void testSubscriber() {
        assertTrue(WebXConnector.instance().getSubscriber().isRunning());
    }

}
