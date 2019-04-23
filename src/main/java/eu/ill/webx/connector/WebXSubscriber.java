package eu.ill.webx.connector;

import eu.ill.webx.transport.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;

public class WebXSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(WebXSubscriber.class);


	private Serializer serializer;

	private ZContext context;
	private ZMQ.Socket socket;
	private String webXServerAddress;
	private int webXServerPort;

	private Thread thread;
	private boolean running = false;

	private List<WebXMessageListener> listeners = new ArrayList<>();

	public WebXSubscriber(Serializer serializer, ZContext context, String webXServerAddress, int webXServerPort) {
		this.serializer = serializer;
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

				// Debug message if needed
//				Message message = serializer.deserializeMessage(messageData);

				this.notifyListeners(messageData);

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

	synchronized private void notifyListeners(byte[] messageData) {
		this.listeners.forEach(listener -> listener.onMessage(messageData));
	}
}
