package eu.ill.webx.relay;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.WebXMessageListener;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

public class Relay implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Relay.class);
    private final WebXConnector connector;

    private Thread webXListenerThread;
    private Thread clientCommandThread;
    private LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private boolean running = false;

    private Session session;
    private RemoteEndpoint remoteEndpoint;
    private boolean useBinary = false;


    public Relay(Session session, WebXConnector connector) {
        if (session != null) {
            this.session = session;
            this.remoteEndpoint = session.getRemote();
        }
        this.connector = connector;
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

            this.webXListenerThread = new Thread(() -> this.webXListenerLoop());
            this.webXListenerThread.start();

            this.clientCommandThread = new Thread(() -> this.clientCommandLoop());
            this.clientCommandThread.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;
                if (this.webXListenerThread != null) {
                    this.webXListenerThread.interrupt();
                    this.webXListenerThread.join();
                    this.webXListenerThread = null;
                }

                if (this.clientCommandThread != null) {
                    this.clientCommandThread.interrupt();
                    this.clientCommandThread.join();
                    this.clientCommandThread = null;
                }

            } catch (InterruptedException e) {
                logger.error("Stop of relay message listener and client command threads interrupted");
            }
        }
    }

    @Override
    public void onMessage(byte[] messageData) {
        try {
            this.messageQueue.put(messageData);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueCommand(byte[] commandData) {
        try {
            this.instructionQueue.put(commandData);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding command to relay command queue");
        }
    }

    private void webXListenerLoop() {
        boolean useBinary = this.connector.getSerializer().getType().equals("binary");
        while (this.running) {
            try {
                byte[] messageData = this.messageQueue.take();
                if (useBinary) {
                    this.sendBinaryToRemote(messageData);

                } else {
                    String responseString = new String(messageData);
                    this.sendStringToRemote(responseString);
                }

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }

    private void clientCommandLoop() {
        boolean useBinary = this.connector.getSerializer().getType().equals("binary");
        while (this.running) {
            try {
                byte[] requestData = this.instructionQueue.take();

                if (requestData.length == 4) {
                    String messageString = new String(requestData);
                    if (messageString.equals("comm")) {
                        String serializerType = this.connector.getSerializer().getType();
                        this.sendStringToRemote(serializerType);
                    }

                } else {
                    byte[] responseData = this.connector.sendRequestData(requestData);
                    if (useBinary) {
                        this.sendBinaryToRemote(responseData);

                    } else {
                        String responseString = new String(responseData);
                        this.sendStringToRemote(responseString);
                    }
                }

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }

    public synchronized void sendStringToRemote(String data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendString(data);
            }

        } catch (IOException e) {
            logger.error("Failed to write data to web socket");
        }
    }

    public synchronized void sendBinaryToRemote(byte[] data) {
        try {
            if (this.remoteEndpoint != null) {
                this.remoteEndpoint.sendBytes(ByteBuffer.wrap(data));
            }

        } catch (IOException e) {
            logger.error("Failed to write data to web socket");
        }
    }
}
