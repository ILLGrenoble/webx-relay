package eu.ill.webx.relay;

import eu.ill.webx.connector.listener.WebXMessageListener;
import eu.ill.webx.connector.message.WebXMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;

public class Relay implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Relay.class);

    private Thread webXListenerThread;
    private LinkedBlockingDeque<WebXMessage> webXMessageQueue = new LinkedBlockingDeque<>();
    private boolean running = false;

    public Relay() {
    }

    public Thread getWebXListenerThread() {
        return webXListenerThread;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start() {
        if (!running) {
            running = true;

            this.webXListenerThread = new Thread(() -> this.loop());
            this.webXListenerThread.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;
                this.webXListenerThread.interrupt();
                this.webXListenerThread.join();
                this.webXListenerThread = null;

            } catch (InterruptedException e) {
                logger.error("Stop of relay message listener thread interrupted");
            }
        }
    }

    @Override
    public void onMessage(WebXMessage message) {
        try {
            this.webXMessageQueue.put(message);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    private void loop() {
        while (this.running) {
            try {
                WebXMessage message = this.webXMessageQueue.take();

                // Send message to client through web socket
                logger.info(message.toString());

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }
}
