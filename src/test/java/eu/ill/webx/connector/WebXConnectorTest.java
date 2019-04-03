package eu.ill.webx.connector;

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

public class WebXConnectorTest {

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
    public void testConnection() {
        assertTrue(WebXConnector.instance().isConnected());

        logger.info("Publisher port = " + WebXConnector.instance().getWebXPublisherPort());
        logger.info("Collector port = " + WebXConnector.instance().getWebXCollectorPort());
        logger.info("Screen size = " + WebXConnector.instance().getScreenSize());
    }

    @Test
    public void testWindowsRequest() {
        WebXRequest windowsRequest = new WebXRequest(WebXRequest.Type.Windows);

        WebXWindowsResponse windowsResponse = (WebXWindowsResponse)WebXConnector.instance().sendRequest(windowsRequest);
        assertNotNull(windowsResponse);

        List<WindowProperties> windows = windowsResponse.getWindows();
        assertTrue(windows.size() > 0);
        windows.forEach(window -> {
            logger.info(window.toString());
        });
    }

}
