package eu.ill.webx.transport;

import eu.ill.webx.model.MessageListener;
import eu.ill.webx.utils.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;

public class MessageSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    private ZMQ.Socket socket;

    private Thread thread;
    private boolean running = false;

    private final List<MessageListener> listeners = new ArrayList<>();

    public MessageSubscriber() {
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

            try {
                // Add a sleep to ensure that the socket is listening
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }

            logger.debug("WebX Message Subscriber started");
        }
    }

    public void stop() {
        if (this.running) {
            synchronized (this) {
                this.running = false;
            }

            try {
                this.thread.interrupt();
                this.thread.join();
                this.thread = null;

                this.socket.close();

                logger.debug("WebX Message Subscriber disconnected");

            } catch (InterruptedException exception) {
                logger.error("Stop of WebX Subscriber thread interrupted");
            }
        }
    }

    public void loop() {
        while (this.running) {
            try {
                byte[] messageData = socket.recv();
                logger.trace("Got message of length {}: {}", messageData.length, HexString.toString(messageData, 32));
                this.notifyListeners(messageData);

            } catch (org.zeromq.ZMQException e) {
                if (this.running) {
                    logger.info("WebX Subscriber thread interrupted");
                }
            }
        }
    }

    synchronized public void addListener(MessageListener listener) {
        this.listeners.add(listener);
    }

    synchronized public void removeListener(MessageListener listener) {
        this.listeners.remove(listener);
    }

    synchronized private void notifyListeners(byte[] messageData) {
        this.listeners.forEach(listener -> listener.onMessage(messageData));
    }
}
