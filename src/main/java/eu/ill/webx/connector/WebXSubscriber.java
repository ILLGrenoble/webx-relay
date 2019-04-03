package eu.ill.webx.connector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.connector.message.WebXWindowsMessage;
import eu.ill.webx.connector.response.WebXWindowsResponse;
import eu.ill.webx.domain.display.WindowProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.List;

public class WebXSubscriber implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(WebXSubscriber.class);

	private ObjectMapper objectMapper = new ObjectMapper();

	private ZContext context;
	private ZMQ.Socket socket;
	private String webXServerAddress;
	private int webXServerPort;
	private boolean running = false;

	public WebXSubscriber(ZContext context, String webXServerAddress, int webXServerPort) {
		this.context = context;
		this.webXServerAddress = webXServerAddress;
		this.webXServerPort = webXServerPort;
	}

	public boolean isRunning() {
		return running;
	}

	@Override
	public void run() {
		this.socket = context.createSocket(SocketType.SUB);
		this.socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
		String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
		socket.connect(fullAddress);

		this.running = true;
		while (this.running) {
			logger.info("Waiting for message...");
			try {
				byte[] messageData = socket.recv();
				WebXWindowsMessage message = objectMapper.readValue(messageData, WebXWindowsMessage.class);
				logger.info("Got message");

				List<WindowProperties> windows = message.getWindows();
				windows.forEach(window -> {
					logger.info(window.toString());
				});

			} catch (JsonParseException e) {
				logger.error("Error parsing JSON message");

			} catch (JsonMappingException e) {
				logger.error("Error mapping JSON message");

			} catch (IOException e) {
				logger.error("Unable to convert message to JSON");

			} catch (org.zeromq.ZMQException e) {
				logger.error("Subscriber interrupted");
			}
		}
	}

	public void stop() {
		this.running = false;
	}
}
