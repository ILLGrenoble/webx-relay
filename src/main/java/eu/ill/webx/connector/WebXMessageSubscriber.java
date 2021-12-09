package eu.ill.webx.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;

public class WebXMessageSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(WebXMessageSubscriber.class);

    private ZMQ.Socket socket;

    private Thread thread;
    private boolean running = false;

    private final List<WebXMessageListener> listeners = new ArrayList<>();

    public WebXMessageSubscriber() {
    }

    public synchronized void start(ZContext context, String address) {
        if (!running) {
            this.socket = context.createSocket(SocketType.SUB);
            this.socket.setLinger(0);
            this.socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
            this.socket.connect(address);

            running = true;

            this.thread = new Thread(this::loop);
            this.thread.start();

            logger.info("WebX Message Subscriber started");
        }
    }

    public synchronized void stop() {
        if (this.running) {
            try {
                this.running = false;

                this.thread.interrupt();
                this.thread.join();
                this.thread = null;

                this.socket.close();

                logger.info("WebX Message Subscriber disconnected");

            } catch (InterruptedException exception) {
                logger.error("Stop of WebX Subscriber thread interrupted");
            }
        }
    }

    public void loop() {
        while (this.running) {
            try {
                byte[] messageData = socket.recv();
                this.notifyListeners(messageData);

            } catch (org.zeromq.ZMQException e) {
                if (this.running) {
                    logger.info("WebX Subscriber thread interrupted");
                }
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
