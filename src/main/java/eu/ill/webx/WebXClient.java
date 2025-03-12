/*
 * WebX Relay
 * Copyright (C) 2023 Institut Laue-Langevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ill.webx;

import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SocketResponse;
import eu.ill.webx.transport.InstructionPublisher;
import eu.ill.webx.transport.SessionChannel;
import eu.ill.webx.transport.Transport;
import eu.ill.webx.utils.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXClient {

    private static final Logger logger = LoggerFactory.getLogger(WebXClient.class);

    private InstructionPublisher instructionPublisher;
    private SessionChannel sessionChannel;

    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private final PriorityBlockingQueue<Message> messageQueue = new PriorityBlockingQueue<>();

    private Thread clientInstructionThread;
    private Thread connectionCheckThread;

    private boolean connected = false;
    private boolean running = false;

    private final ByteBuffer instructionPrefix = ByteBuffer.allocate(20).order(LITTLE_ENDIAN);

    private SessionId sessionId;

    private long clientIndex;
    private int clientId = 0;

    public WebXClient() {
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public synchronized void connect(Transport transport) throws WebXConnectionException {
        if (!connected) {
            // Standalone
            String sessionIdString = "00000000000000000000000000000000";
            logger.info("Using standalone session Id \"{}\"", sessionIdString);

            this.setSessionId(sessionIdString);

            // Get the client Id
            this.connectClient(transport);

            this.instructionPublisher = transport.getInstructionPublisher();
            connected = true;
        }
    }

    public synchronized void connect(Transport transport, WebXClientInformation clientInformation) throws WebXConnectionException {
        if (!connected) {
            // Check if connection is via sessionId (to existing session) or user authentication (potentially new session)
            String sessionIdString;
            if (clientInformation.getSessionId() != null) {
                logger.info("Connecting to existing WebX session using sessionId \"{}\"", clientInformation.getSessionId());
                sessionIdString = clientInformation.getSessionId();

            } else {
                logger.info("Connecting to WebX using password authentication");
                sessionIdString = this.startSession(transport, clientInformation);
                logger.info("Authentication successful. Got session Id \"{}\"", sessionIdString);
            }

            this.setSessionId(sessionIdString);

            // Get the client Id
            this.connectClient(transport);

            this.instructionPublisher = transport.getInstructionPublisher();
            this.sessionChannel = transport.getSessionChannel();

            connected = true;
        }
    }

    public synchronized void disconnect(Transport transport) {
        if (connected) {
            this.disconnectClient(transport);

            connected = false;
        }
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

                logger.debug("Client {} stopped", this.getClientIdString());

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

    public byte[] getMessage() throws WebXClientException, WebXConnectionInterruptException, WebXDisconnectedException {
        if (this.running) {
            try {
                // Get next message, wait for anything
                Message message = this.messageQueue.take();

                if (message.getType().equals(Message.Type.INTERRUPT)) {
                    throw new WebXConnectionInterruptException(message.getStringData());

                } else if (message.getType().equals(Message.Type.DISCONNECT)) {
                    logger.warn("Received disconnect message from WebX server for client {}", this.getClientIdString());
                    this.connected = false;
                    throw new WebXDisconnectedException("Disconnect message received from the server");

                } else if (message.getType().equals(Message.Type.CLOSE)) {
                    return null;
                }

                byte[] messageData = message.getData();

                if (messageData == null) {
                    // connection closed
                    return null;

                } else {
                    logger.trace("Read client message of length {}", messageData.length);

                    if (messageData.length < Message.MESSAGE_HEADER_LENGTH) {
                        throw new WebXClientException("Invalid message received from the server");
                    }

                    // Add queue size to message metadata
                    byte queueSize = (byte)Math.min(messageQueue.size(), 255); // get queue size (limit to 255)
                    ByteBuffer messageMetadataWrapper = ByteBuffer.wrap(messageData, 18, 1).order(LITTLE_ENDIAN);
                    messageMetadataWrapper.put(queueSize);

                    return messageData;
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

    public boolean matchesMessageIndexMask(final byte[] messageData) {
        if (messageData.length < 24) {
            return false;
        }

        // Test for bitwise and is not zero on the client Index Mask (8 bytes at offset of 16)
        ByteBuffer messageMetadataWrapper = ByteBuffer.wrap(messageData, 16, 8).order(LITTLE_ENDIAN);
        long clientIndexMask = messageMetadataWrapper.getLong();

//        logger.info("Got mask {} for index {}", String.format("%016x", clientIndexMask), String.format("%016x", this.clientIndex));

        return (clientIndexMask & this.clientIndex) != 0;
    }

    private void setSessionId(final String sessionIdString) {
        this.sessionId = new SessionId(sessionIdString);

        // Set the sessionId in the instruction prefix
        this.instructionPrefix.put(0, sessionId.bytes(), 0, 16);
    }

    private synchronized void connectClient(Transport transport) throws WebXConnectionException {
        try {
            final String request = String.format("connect,%s", this.sessionId.hexString());
            String response = transport.sendRequest(request).toString();
            if (response == null) {
                this.stop();
                throw new WebXConnectionException("WebX Server returned a null connection response");

            } else if (response.isEmpty()) {
                this.stop();
                throw new WebXConnectionException(String.format("WebX Server refused connection with sessionId %s", this.sessionId.hexString()));
            }

            final String[] responseElements = response.split(",");

            if (responseElements.length != 2) {
                this.stop();
                throw new WebXConnectionException("WebX Server returned an invalid connection response");
            }

            String clientIdString = responseElements[0];
            final String clientIndexString = responseElements[1];

            this.clientId = Integer.parseUnsignedInt(responseElements[0], 16);
            this.clientIndex = Long.parseUnsignedLong(responseElements[1], 16);

            // Add the clientId to the instruction prefix
            ByteBuffer clientIdBuffer = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
            clientIdBuffer.putInt(clientId);
            this.instructionPrefix.put(16, clientIdBuffer.array(), 0, 4);

            logger.info("Client connected to WebX session \"{}\":  Got client Id \"{}\" and index \"{}\"", this.sessionId.hexString(), clientIdString, clientIndexString);

        } catch (NumberFormatException exception) {
            logger.error("Cannot connect client: Failed to parse client id and index");
            this.stop();
            throw new WebXConnectionException("Failed to parse client id and index");

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot connect client: WebX Server is disconnected");
            this.stop();
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    private synchronized void disconnectClient(Transport transport) {
        if (this.clientId != 0) {
            try {
                final String request = String.format("disconnect,%s,%s", this.sessionId.hexString(), this.getClientIdString());
                SocketResponse response = transport.sendRequest(request);
                if (response == null) {
                    logger.error("Failed to get response from WebX server");
                }

            } catch (WebXDisconnectedException e) {
                logger.warn("Cannot disconnect client {}: WebX Server is disconnected", this.getClientIdString());
            }
        }
    }

    private void clientInstructionLoop() {
        while (this.running) {
            try {
                final byte[] instructionData = this.instructionQueue.take();

                // Set the sessionId and clientId at the beginning
                System.arraycopy(this.instructionPrefix.array(), 0, instructionData, 0, 20);

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
                logger.trace("Sending ping to session {}", this.sessionId.hexString());
                SocketResponse response = this.sessionChannel.sendRequest("ping," + this.sessionId.hexString());

                String[] responseElements = response.toString().split(",");

                if (responseElements[0].equals("pang")) {
                    logger.error("Failed to ping webX Session {}: {}", this.sessionId.hexString(), responseElements[2]);
                    this.messageQueue.add(new Message.InterruptMessage("Failed to ping WebX Session"));
                }

            } catch (WebXDisconnectedException e) {
                logger.error("Failed to get response from connector ping to session {}", this.sessionId.hexString());
                this.messageQueue.add(new Message.InterruptMessage("Failed to get response from connection ping to WebX Session"));
            }

            try {
                Thread.sleep(15000);

            } catch (InterruptedException ignored) {
            }
        }
    }

    private String startSession(Transport transport, WebXClientInformation clientInformation) throws WebXConnectionException {
        try {
            // Start WebX session via the router and get a session ID
            String response = transport.getSessionChannel().startSession(clientInformation);
            String[] responseData = response.split(",");
            int responseCode = Integer.parseInt(responseData[0]);
            String responseValue = responseData[1];
            if (responseCode == 0) {
                return responseValue;

            } else {
                logger.error("Couldn't create WebX session: {}", responseValue);
                throw new WebXConnectionException("Couldn't create WebX session: session response invalid");
            }

        } catch (WebXDisconnectedException e) {
            logger.error("Cannot start session: WebX Server is disconnected");
            this.stop();
            throw new WebXConnectionException("WebX Server disconnected when creating WebX session");
        }
    }

    private String getClientIdString() {
        return this.clientId == 0 ? "<unconnected>" : String.format("%08x", clientId);
    }
}
