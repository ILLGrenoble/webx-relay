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

import eu.ill.webx.model.ClientIdentifier;
import eu.ill.webx.model.Message;
import eu.ill.webx.model.SessionId;
import eu.ill.webx.transport.InstructionPublisher;
import eu.ill.webx.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WebXSession {

    private static final Logger logger = LoggerFactory.getLogger(WebXSession.class);

    private final SessionId sessionId;
    private final InstructionPublisher instructionPublisher;

    private final List<WebXClient> clients = new ArrayList<>();

    private final WebXSessionValidator sessionValidator;

    private boolean running = false;

    public WebXSession(final SessionId sessionId, final Transport transport) {
        this.sessionId = sessionId;
        this.instructionPublisher = transport.getInstructionPublisher();
        this.sessionValidator = new WebXSessionValidator(this.sessionId, transport, (error -> {
            logger.error("Session validation error: {}", error);
            this.sendMessageToClients(new Message.InterruptMessage("Failed to ping WebX Session"));
        }));
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public synchronized void start() {
        if (!running) {
            running = true;

            this.sessionValidator.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;
                this.sessionValidator.interrupt();
                this.sessionValidator.join();

                logger.debug("Session {} stopped", this.sessionId.hexString());

            } catch (InterruptedException exception) {
                logger.error("Stop of relay message listener and client instruction threads interrupted", exception);
            }
        }
    }

    public synchronized WebXClient createClient(final ClientIdentifier clientIdentifier) {
        final WebXClient client = new WebXClient(clientIdentifier, this);
        this.clients.add(client);
        return client;
    }

    public synchronized void disconnectClient(final WebXClient client) {
        client.disconnect();
        this.clients.remove(client);
    }

    public synchronized void disconnectAllClients() {
        for (WebXClient client : this.clients) {
            client.disconnect();
        }
        this.clients.clear();
    }

    public synchronized int getClientCount() {
        return this.clients.size();
    }

    public synchronized void sendInstruction(byte[] instructionData) {
        this.instructionPublisher.queueInstruction(instructionData);
    }

    public synchronized void onMessage(byte[] messageData) {
        List<WebXClient> indexAssociatedClients = this.clients.stream()
                .filter(webXClient -> webXClient.matchesMessageIndexMask(messageData))
                .toList();

        for (WebXClient client : indexAssociatedClients) {
            client.onMessage(messageData);
        }
    }

    private void sendMessageToClients(final Message message) {
        for (WebXClient client : this.clients) {
            client.onMessage(message);
        }
    }

}
