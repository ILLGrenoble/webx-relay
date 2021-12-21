package eu.ill.webx.relay;

import eu.ill.webx.transport.*;
import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.model.MessageListener;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class Client implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final Session session;
    private MessageSubscriber messageSubscriber;
    private InstructionPublisher instructionPublisher;

    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();

    private Thread webXListenerThread;
    private Thread clientInstructionThread;

    private boolean running = false;

    public Client(Session session) {
        this.session = session;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public Session getSession() {
        return session;
    }

    public synchronized boolean start(Transport transport) {
        if (!running) {
            try {
                // Start WebX session via the router and get a session ID
                String response = transport.getSessionChannel().startSession("caunt", "password");
                String[] responseData = response.split(",");
                int responseCode = Integer.parseInt(responseData[0]);
                String responseValue = responseData[1];
                if (responseCode == 0) {
                    String sessionIdString = responseValue;

                    logger.info("Got session Id \"{}\"", sessionIdString);
                    byte[] sessionId = sessionIdToByteArray(sessionIdString);

                    // Put sessionId as first message to send to the client
                    this.onMessage(sessionId);

                    running = true;

                    // Add relay as a listener to webx messages
                    this.messageSubscriber = transport.getMessageSubscriber();
                    this.instructionPublisher = transport.getInstructionPublisher();
                    this.messageSubscriber.addListener(this);

                    this.webXListenerThread = new Thread(this::webXListenerLoop);
                    this.webXListenerThread.start();

                    this.clientInstructionThread = new Thread(this::clientInstructionLoop);
                    this.clientInstructionThread.start();

                } else {
                    logger.error("Couldn't create WebX session: {}", responseValue);
                }

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
            logger.trace("Got client message of length {}", messageData.length);
            this.messageQueue.put(messageData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueInstruction(byte[] instructionData) {
        try {
            logger.trace("Got instruction of length {}", instructionData.length);
            this.instructionQueue.put(instructionData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding instruction to instruction queue");
        }
    }

    private void webXListenerLoop() {
        // Create a POLL message (messageType 8)
        ByteBuffer pollMessageBuffer = ByteBuffer.allocate(32).order(LITTLE_ENDIAN)
                .putInt(0)  // dummy sessionId (1/4)
                .putInt(0)  // dummy sessionId (2/4)
                .putInt(0)  // dummy sessionId (3/4)
                .putInt(0)  // dummy sessionId (4/4)
                .putInt(8)  // messageType
                .putInt(0)  // messageId
                .putInt(16) // messageLength
                .putInt(0);  // padding

        while (this.running) {
            try {
                byte[] messageData = this.messageQueue.poll(5000, TimeUnit.MILLISECONDS);
                if (messageData != null) {
                    logger.trace("Sending client message of length {}", messageData.length);
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

    private byte[] sessionIdToByteArray(String sessionId) {
        if (sessionId.length() != 32) {
            logger.error("Received invalid UUID for session Id: {}", sessionId);
        }

        int length = sessionId.length();
        byte[] data = new byte[length / 2];
        int index = 0;
        for (int i = 0; i < length; i += 2) {
            data[index++] = (byte) ((Character.digit(sessionId.charAt(i), 16) << 4)  + Character.digit(sessionId.charAt(i + 1), 16));
        }
        return data;
    }
}
