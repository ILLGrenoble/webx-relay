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

    private final ZContext context;
    private final String   webXServerAddress;
    private final int      webXServerPort;

    private ZMQ.Socket         socket;
    private Thread         thread;
    private boolean        running = false;

    private final List<WebXMessageListener> listeners = new ArrayList<>();

    public WebXMessageSubscriber(ZContext context, String webXServerAddress, int webXServerPort) {
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

                logger.info("WebX Message Subscriber stopped");

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
