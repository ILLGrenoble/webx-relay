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
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.PriorityBlockingQueue;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXClient {

    private static final Logger logger = LoggerFactory.getLogger(WebXClient.class);

    private final ClientIdentifier clientIdentifier;
    private final WebXSession session;

    private final PriorityBlockingQueue<Message> messageQueue = new PriorityBlockingQueue<>();

    private boolean connected = true;

    private final ByteBuffer instructionPrefix = ByteBuffer.allocate(20).order(LITTLE_ENDIAN);


    public WebXClient(final ClientIdentifier clientIdentifier, final WebXSession session) {
        this.clientIdentifier = clientIdentifier;
        this.session = session;

        // Add the clientId to the instruction prefix
        ByteBuffer clientIdBuffer = ByteBuffer.allocate(4).order(LITTLE_ENDIAN);
        clientIdBuffer.putInt(clientIdentifier.clientId());
        this.instructionPrefix.put(16, clientIdBuffer.array(), 0, 4);
    }

    public ClientIdentifier getClientIdentifier() {
        return clientIdentifier;
    }

    public SessionId getSessionId() {
        return this.session.getSessionId();
    }

    public void disconnect() {
        this.connected = false;
        this.onMessage(new Message.CloseMessage());
    }

    public boolean isConnected() {
        return connected;
    }

    public void onMessage(byte[] messageData) {
        logger.trace("Got client message of length {}", messageData.length);
        Message message = new Message(messageData);
        this.messageQueue.add(message);
    }

    public void onMessage(Message message) {
        this.messageQueue.add(message);
    }

    public void sendInstruction(byte[] instructionData) {
        logger.trace("Got instruction of length {}", instructionData.length);

        // Set the sessionId and clientId at the beginning
        System.arraycopy(this.instructionPrefix.array(), 0, instructionData, 0, 20);

        this.session.sendInstruction(instructionData);
    }

    public byte[] getMessage() throws WebXClientException, WebXConnectionInterruptException, WebXDisconnectedException {
        if (this.connected) {
            try {
                // Get next message, wait for anything
                Message message = this.messageQueue.take();

                if (message.getType().equals(Message.Type.INTERRUPT)) {
                    throw new WebXConnectionInterruptException(message.getStringData());

                } else if (message.getType().equals(Message.Type.DISCONNECT)) {
                    logger.info("Client (Id \"{}\" and index \"{}\") received disconnect message from WebX session \"{}\"", this.getClientIdentifier().clientIdString(), this.getClientIdentifier().clientIndexString(), this.getSessionId().hexString());

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

                    return messageData;
                }

            } catch (InterruptedException exception) {
                throw new WebXClientException("Client message listener thread interrupted");

            }
        } else {
            throw new WebXClientException("WebXClient is not connected");
        }
    }

    public boolean matchesMessageIndexMask(final byte[] messageData) {
        if (messageData.length < 24) {
            return false;
        }

        // Test for bitwise and is not zero on the client Index Mask (8 bytes at offset of 16)
        ByteBuffer messageMetadataWrapper = ByteBuffer.wrap(messageData, 16, 8).order(LITTLE_ENDIAN);
        long clientIndexMask = messageMetadataWrapper.getLong();

        return (clientIndexMask & this.clientIdentifier.clientIndex()) != 0;
    }
}
