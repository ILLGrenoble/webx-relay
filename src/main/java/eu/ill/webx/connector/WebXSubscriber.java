package eu.ill.webx.connector;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ill.webx.connector.listener.WebXMessageListener;
import eu.ill.webx.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebXSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(WebXSubscriber.class);

	private ObjectMapper objectMapper = new ObjectMapper();

	private ZContext context;
	private ZMQ.Socket socket;
	private String webXServerAddress;
	private int webXServerPort;

	private Thread thread;
	private boolean running = false;

	private List<WebXMessageListener> listeners = new ArrayList<>();

	public WebXSubscriber(ZContext context, String webXServerAddress, int webXServerPort) {
		this.context = context;
		this.webXServerAddress = webXServerAddress;
		this.webXServerPort = webXServerPort;
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void start() {
		if (!running) {
			this.socket = context.createSocket(SocketType.SUB);
			this.socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
			String fullAddress = "tcp://" + webXServerAddress + ":" + webXServerPort;
			socket.connect(fullAddress);

			running = true;

			this.thread = new Thread(() -> this.loop());
			this.thread.start();

			logger.info("WebX Subscriber started");
		}
	}

	public synchronized void stop() {
		if (this.running) {
			try {
				this.running = false;

				this.thread.interrupt();
				this.thread.join();
				this.thread = null;

				logger.info("WebX Subscriber stopped");

			} catch (InterruptedException e) {
				logger.error("Stop of WebX Subscriber thread interrupted");
			}
		}
	}

	public void loop() {
		while (this.running) {
			try {
				byte[] messageData = socket.recv();

				Message message = objectMapper.readValue(messageData, Message.class);
				this.notifyListeners(message);

			} catch (JsonParseException e) {
				logger.error("Error parsing JSON message");

			} catch (JsonMappingException e) {
				logger.error("Error mapping JSON message");

			} catch (IOException e) {
				logger.error("Unable to convert message to JSON");

			} catch (org.zeromq.ZMQException e) {
				logger.info("WebX Subscriber thread interrupted");
			}
		}
	}

	synchronized public void addListener(WebXMessageListener listener) {
		this.listeners.add(listener);
	}

	synchronized public void removeListener(WebXMessageListener listener) {
		this.listeners.remove(listener);
	}

	synchronized private void notifyListeners(Message message) {
		this.listeners.forEach(listener -> listener.onMessage(message));
	}
}
