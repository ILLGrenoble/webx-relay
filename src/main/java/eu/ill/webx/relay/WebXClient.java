package eu.ill.webx.relay;

import eu.ill.webx.connector.*;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXClient implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WebXClient.class);

    private final Session session;
    private WebXMessageSubscriber messageSubscriber;
    private WebXInstructionPublisher instructionPublisher;

    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();

    private Thread webXListenerThread;
    private Thread clientInstructionThread;

    private boolean running = false;

    public WebXClient(Session session) {
        this.session = session;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public Session getSession() {
        return session;
    }

    public synchronized boolean start(WebXConnector connector) {
        if (!running) {
            running = true;

            try {
                // Add relay as a listener to webx messages
                this.messageSubscriber = connector.getMessageSubscriber();
                this.instructionPublisher = connector.getInstructionPublisher();
                this.messageSubscriber.addListener(this);

                // Start WebX session via the router and get a session ID
                String sessionId = connector.getSessionChannel().startSession("username", "password");
                logger.info("Got session Id \"{}\"", sessionId);

                this.webXListenerThread = new Thread(this::webXListenerLoop);
                this.webXListenerThread.start();

                this.clientInstructionThread = new Thread(this::clientInstructionLoop);
                this.clientInstructionThread.start();

            } catch (DisconnectedException e) {
                logger.error("WebX Server is disconnected");
                this.stop();
            }
        }
        return running;
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;

                // Remove relay from webx subscriber
                this.messageSubscriber.removeListener(this);
                this.messageSubscriber = null;
                this.instructionPublisher = null;

                if (this.webXListenerThread != null) {
                    this.webXListenerThread.interrupt();
                    this.webXListenerThread.join();
                    this.webXListenerThread = null;
                }

                if (this.clientInstructionThread != null) {
                    this.clientInstructionThread.interrupt();
                    this.clientInstructionThread.join();
                    this.clientInstructionThread = null;
                }

            } catch (InterruptedException exception) {
                logger.error("Stop of relay message listener and client instruction threads interrupted", exception);
            }
        }
    }

    @Override
    public void onMessage(byte[] messageData) {
        try {
            this.messageQueue.put(messageData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueInstruction(byte[] instructionData) {
        try {
            this.instructionQueue.put(instructionData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding instruction to instruction queue");
        }
    }

    private void webXListenerLoop() {
        // Create a POLL message (messageType 8)
        ByteBuffer pollMessageBuffer = ByteBuffer.allocate(16).order(LITTLE_ENDIAN)
                .putInt(8)  // messageType
                .putInt(0)  // messageId
                .putInt(16) // messageLength
                .putInt(0);  // padding

        while (this.running) {
            try {
                byte[] messageData = this.messageQueue.poll(5000, TimeUnit.MILLISECONDS);
                if (messageData != null) {
                    this.sendData(messageData);

                } else {
                    // Keep socket alive
                    this.sendData(pollMessageBuffer.array());
                }

            } catch (InterruptedException exception) {
                if (this.running) {
                    logger.info("Client message listener thread interrupted");
                }
            }
        }
    }

    public synchronized void sendData(byte[] data) {
        try {
            this.session.getRemote().sendBytes(ByteBuffer.wrap(data));

        } catch (IOException exception) {
            logger.error("Failed to write binary data to web socket", exception);
        }
    }

    private void clientInstructionLoop() {
        while (this.running) {
            try {
                final byte[] instructionData = this.instructionQueue.take();
                this.instructionPublisher.sendInstructionData(instructionData);

            } catch (InterruptedException exception) {
                if (this.running) {
                    logger.info("Client instruction listener thread interrupted");
                }
            }
        }
    }
}
