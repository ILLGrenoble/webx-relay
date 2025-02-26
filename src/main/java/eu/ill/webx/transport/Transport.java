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
package eu.ill.webx.transport;

import eu.ill.webx.model.ConnectionData;
import eu.ill.webx.exceptions.WebXDisconnectedException;
import eu.ill.webx.model.SocketResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;

public class Transport {

    private static final Logger logger = LoggerFactory.getLogger(Transport.class);

    private ZContext context;
    private boolean connected = false;
    private boolean isStandalone;

    private ClientConnector connector;
    private MessageSubscriber messageSubscriber;
    private InstructionPublisher instructionPublisher;
    private SessionChannel sessionChannel;

    public Transport() {
    }

    public ClientConnector getConnector() {
        return connector;
    }

    public MessageSubscriber getMessageSubscriber() {
        return messageSubscriber;
    }

    public InstructionPublisher getInstructionPublisher() {
        return instructionPublisher;
    }

    public SessionChannel getSessionChannel() {
        return sessionChannel;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public synchronized void connect(String hostname, int port, int socketTimeoutMs, boolean isStandalone) throws WebXDisconnectedException {

        if (this.context == null) {
            this.isStandalone = isStandalone;
            this.connected = false;
            this.context = new ZContext();

            try {
                this.connector = new ClientConnector();
                ConnectionData connectionData = this.connector.connect(this.context, "tcp://" + hostname + ":" + port, socketTimeoutMs, isStandalone);

                this.messageSubscriber = new MessageSubscriber();
                this.messageSubscriber.start(this.context, "tcp://" + hostname + ":" + connectionData.getPublisherPort());

                this.instructionPublisher = new InstructionPublisher();
                this.instructionPublisher.connect(this.context, "tcp://" + hostname + ":" + connectionData.getCollectorPort());

                if (!isStandalone) {
                    this.sessionChannel = new SessionChannel();
                    this.sessionChannel.connect(this.context, "tcp://" + hostname + ":" + connectionData.getSessionPort(), socketTimeoutMs, connectionData.getServerPublicKey());
                }

                this.connected = true;

            } catch (WebXDisconnectedException e) {
                this.disconnect();
                throw e;

            } catch (Exception e) {
                this.disconnect();
                throw new WebXDisconnectedException();
            }

        }
    }

    public synchronized void disconnect() {
        if (this.context != null) {

            if (this.connector != null) {
                this.connector.disconnect();
                this.connector = null;
            }

            if (this.messageSubscriber != null) {
                this.messageSubscriber.stop();
                this.messageSubscriber = null;
            }

            if (this.instructionPublisher != null) {
                this.instructionPublisher.disconnect();
                this.instructionPublisher = null;
            }

            if (this.sessionChannel != null) {
                this.sessionChannel.disconnect();
                this.sessionChannel = null;
            }

            this.context.destroy();
            this.context = null;

            this.connected = false;
        }
    }

    public synchronized SocketResponse sendRequest(final String request) throws WebXDisconnectedException {
        if (!this.connected) {
             throw new WebXDisconnectedException();
        }

        if (this.isStandalone) {
            return this.connector.sendRequest(request);

        } else {
            return this.sessionChannel.sendRequest(request);
        }
    }
}
