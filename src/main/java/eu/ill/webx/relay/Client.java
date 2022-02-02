package eu.ill.webx.relay;

import eu.ill.webx.model.DisconnectedException;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.InstructionPublisher;
import eu.ill.webx.transport.SessionChannel;
import eu.ill.webx.transport.Transport;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final Session session;
    private InstructionPublisher instructionPublisher;
    private SessionChannel sessionChannel;

    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();

    private Thread webXListenerThread;
    private Thread clientInstructionThread;
    private Thread connectionCheckThread;

    private boolean running = false;

    private String webXSessionId;
    private byte[] webXRawSessionId;

    public Client(final Session session) {
        this.session = session;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public Session getSession() {
        return session;
    }

    public String getWebXSessionId() {
        return webXSessionId;
    }

    public synchronized boolean start(Transport transport, boolean standalone, String username, String password, int width, int height, String keyboard) {
        if (!running) {
            String sessionIdString = this.startSession(transport, standalone,  username,  password, width, height, keyboard);
            if (sessionIdString != null) {
                this.webXSessionId = sessionIdString;
                logger.info("Got session Id \"{}\"", sessionIdString);
                this.webXRawSessionId = sessionIdToByteArray(sessionIdString);

                running = true;

                // Add relay as a listener to webx messages
                this.instructionPublisher = transport.getInstructionPublisher();

                this.webXListenerThread = new Thread(this::webXListenerLoop);
                this.webXListenerThread.start();

                this.clientInstructionThread = new Thread(this::clientInstructionLoop);
                this.clientInstructionThread.start();

                if (!standalone) {
                    this.sessionChannel = transport.getSessionChannel();
                    // Start connection checker
                    this.connectionCheckThread = new Thread(this::connectionCheck);
                    this.connectionCheckThread.start();
                }
            }
        }
        return running;
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;

                // Remove relay from webx subscriber
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

                if (this.connectionCheckThread != null) {
                    this.connectionCheckThread.interrupt();
                    this.connectionCheckThread.join();
                    this.connectionCheckThread = null;
                }

            } catch (InterruptedException exception) {
                logger.error("Stop of relay message listener and client instruction threads interrupted", exception);
            }
        }
    }

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
                    this.session.close();
                }

            } catch (DisconnectedException e) {
                logger.error("Failed to get response from connector ping from session {}", this.webXSessionId);

                this.session.close();
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
