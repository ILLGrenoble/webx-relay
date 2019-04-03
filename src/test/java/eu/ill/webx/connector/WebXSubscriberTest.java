package eu.ill.webx.connector;

import com.sun.java.swing.plaf.windows.resources.windows;
import eu.ill.webx.connector.response.WebXWindowsResponse;
import eu.ill.webx.domain.display.WindowProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertNotNull;
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

    // Temporary
    @Test
    public void testSubscribe() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
