package eu.ill.webx;

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.InstructionPublisher;
import eu.ill.webx.transport.SessionChannel;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXClient {

    private static final Logger logger = LoggerFactory.getLogger(WebXClient.class);
    // Create a POLL message (messageType 8)
    private static final ByteBuffer pollMessageBuffer = ByteBuffer.allocate(32).order(LITTLE_ENDIAN)
            .putInt(0)  // dummy sessionId (1/4)
            .putInt(0)  // dummy sessionId (2/4)
            .putInt(0)  // dummy sessionId (3/4)
            .putInt(0)  // dummy sessionId (4/4)
            .putInt(8)  // message meta data: {Type, empty, relayQueueSize, empty}
            .putInt(0)  // messageId
            .putInt(16) // messageLength
            .putInt(0);  // padding

    private InstructionPublisher instructionPublisher;
    private SessionChannel sessionChannel;

    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final PriorityBlockingQueue<Message> messageQueue = new PriorityBlockingQueue<>();

    private Thread clientInstructionThread;
    private Thread connectionCheckThread;

    private boolean connected = false;
    private boolean running = false;

    private String webXSessionId;
    private byte[] webXRawSessionId;

    public WebXClient() {
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public String getWebXSessionId() {
        return webXSessionId;
    }

    public synchronized boolean connect(Transport transport, boolean standalone, String username, String password, int width, int height, String keyboard) {
        if (!connected) {
            String sessionIdString = this.startSession(transport, standalone,  username,  password, width, height, keyboard);
            if (sessionIdString != null) {
                this.webXSessionId = sessionIdString;
                logger.info("Got session Id \"{}\"", sessionIdString);
                this.webXRawSessionId = sessionIdToByteArray(sessionIdString);

                this.instructionPublisher = transport.getInstructionPublisher();
                if (!standalone) {
                    this.sessionChannel = transport.getSessionChannel();
                }

                connected = true;
            }
        }
        return connected;
    }

    public synchronized void start() {
        if (!running) {
            running = true;

            // Add relay as a listener to webx messages

            this.clientInstructionThread = new Thread(this::clientInstructionLoop);
            this.clientInstructionThread.start();

            if (this.sessionChannel != null) {
                // Start connection checker
                this.connectionCheckThread = new Thread(this::connectionCheck);
                this.connectionCheckThread.start();
            }
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;

                this.messageQueue.add(new Message.CloseMessage());

                // Remove relay from webx subscriber
                this.instructionPublisher = null;

                if (this.clientInstructionThread != null) {
                    this.clientInstructionThread.interrupt();
                    this.clientInstructionThread.join();
                    this.clientInstructionThread = null;
                }

                if (this.connectionCheckThread != null) {
                    this.connectionCheckThread.interrupt();
                    this.connectionCheckThread.join();
                    this.connectionCheckThread = null;
                }

                logger.debug("Client stopped");

            } catch (InterruptedException exception) {
                logger.error("Stop of relay message listener and client instruction threads interrupted", exception);
            }
        }
    }

    public void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {}", messageData.length);
        Message message = new Message(messageData);
        this.messageQueue.add(message);
    }

    public void queueInstruction(byte[] instructionData) {
        try {
            logger.trace("Got instruction of length {}", instructionData.length);
            this.instructionQueue.put(instructionData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding instruction to instruction queue");
        }
    }

    public byte[] getMessage() throws WebXClientException, WebXConnectionInterruptException {
        if (this.running) {
            try {
                // Get next message, wait 5 seconds and return poll message if nothing from the server
                Message message = this.messageQueue.poll(5000, TimeUnit.MILLISECONDS);

                if (message != null) {
                    if (message.getType().equals(Message.Type.INTERRUPT)) {
                        throw new WebXConnectionInterruptException(message.getStringData());

                    } else if (message.getType().equals(Message.Type.CLOSE)) {
                        return null;
                    }

                    byte[] messageData = message.getData();

                    if (messageData == null) {
                        // connection closed
                        return null;

                    } else {
                        logger.trace("Read client message of length {}", messageData.length);

                        if (messageData.length < 32) {
                            throw new WebXClientException("Invalid message received from the server");
                        }

                        // Add queue size to message metadata
                        byte queueSize = (byte)Math.min(messageQueue.size(), 255); // get queue size (limit to 255)
                        ByteBuffer messageMetadataWrapper = ByteBuffer.wrap(messageData, 18, 1).order(LITTLE_ENDIAN);
                        messageMetadataWrapper.put(queueSize);

                        return messageData;
                    }


                } else {
                    // Keep socket alive
                    return pollMessageBuffer.array();
                }

            } catch (InterruptedException exception) {
                throw new WebXClientException("Client message listener thread interrupted");

            }
        } else {
            throw new WebXClientException("WebXClient is not running");
        }
    }

    public void close() {
        this.messageQueue.add(new Message.CloseMessage());
    }

    private void clientInstructionLoop() {
        while (this.running) {
            try {
                final byte[] instructionData = this.instructionQueue.take();

                // Set the sessionId at the beginning
                System.arraycopy(this.webXRawSessionId, 0, instructionData, 0, 16);

                this.instructionPublisher.sendInstructionData(instructionData);

            } catch (InterruptedException exception) {
                if (this.running) {
                    logger.info("Client instruction listener thread interrupted");
                }
            }
        }
    }

    private void connectionCheck() {
        while (this.running) {
            // Use a variable sleep: if connection is ok, wait 5s, otherwise try every second to reconnect
            try {
                logger.trace("Sending ping to session {}", this.webXSessionId);
                SocketResponse response = this.sessionChannel.sendRequest("ping," + this.webXSessionId);

                String[] responseElements = response.toString().split(",");

                if (responseElements[0].equals("pang")) {
                    logger.error("Failed to ping webX Session {}: {}", this.webXSessionId, responseElements[2]);
                    this.messageQueue.add(new Message.InterruptMessage("Failed to ping WebX Session"));
                }

            } catch (DisconnectedException e) {
                logger.error("Failed to get response from connector ping to session {}", this.webXSessionId);
                this.messageQueue.add(new Message.InterruptMessage("Failed to get response from connection ping to WebX Session"));
            }

            try {
                Thread.sleep(15000);

            } catch (InterruptedException ignored) {
            }
        }
    }

    private String startSession(Transport transport, boolean standalone, String username, String password, int width, int height, String keyboard) {
        if (standalone) {
            return "00000000000000000000000000000000";

        } else {
            try {
                // Start WebX session via the router and get a session ID
                String response = transport.getSessionChannel().startSession(username, password, width, height, keyboard);
                String[] responseData = response.split(",");
                int responseCode = Integer.parseInt(responseData[0]);
                String responseValue = responseData[1];
                if (responseCode == 0) {
                    return responseValue;

                } else {
                    logger.error("Couldn't create WebX session: {}", responseValue);
                }

            } catch (DisconnectedException e) {
                logger.error("WebX Server is disconnected");
                this.stop();
            }
        }

        return null;
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
