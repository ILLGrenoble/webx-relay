package eu.ill.webx.relay;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.WebXMessageListener;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXRelay implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(WebXRelay.class);
    private final WebXConnector connector;
    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();

    private Thread webXListenerThread;
    private Thread clientInstructionThread;
    private WebXDataCommunicator dataCommunicator;
    private boolean running = false;

    private RemoteEndpoint remoteEndpoint;


    public WebXRelay(Session session, WebXConnector connector) {
        this.connector = connector;
        if (session != null) {
            this.remoteEndpoint = session.getRemote();
            this.dataCommunicator = new WebXDataCommunicator(this.remoteEndpoint);
        }
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

            // Add relay as a listener to webx messages
            this.connector.getMessageSubscriber().addListener(this);

            // Start WebX session via the router and get a session ID
            String sessionId = this.connector.getSessionChannel().startSession("username", "password");
            logger.info("Got session Id \"{}\"", sessionId);

            this.webXListenerThread = new Thread(this::webXListenerLoop);
            this.webXListenerThread.start();

            this.clientInstructionThread = new Thread(this::clientInstructionLoop);
            this.clientInstructionThread.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;

                // Remove relay from webx subscriber
                this.connector.getMessageSubscriber().removeListener(this);

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
                    this.dataCommunicator.sendData(messageData);

                } else {
                    // Keep socket alive
                    this.dataCommunicator.sendData(pollMessageBuffer.array());
                }

            } catch (InterruptedException exception) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }

    private void clientInstructionLoop() {
        while (this.running) {
            try {
                final byte[] instructionData = this.instructionQueue.take();
                this.connector.getInstructionPublisher().sendInstructionData(instructionData);

            } catch (InterruptedException exception) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }
}
