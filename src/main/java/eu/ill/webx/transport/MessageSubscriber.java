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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * The message subscriber connects to the ZMQ message publisher of the WebX Router (or Engine if in standalone)
 * and forwards them immediately to a handler (managed in the WebXHost).
 * The host handler filters the messages by the sessionId (first 16 bytes). The session then filters the message by the
 * client index mask to determine exactly which clients require the message. The client queues the message awaiting for it
 * to be read by a client application.
 */
public class MessageSubscriber {

    /**
     * Interface used to handle the callback when a message arrives
     */
    public interface MessageHandler {
        /**
         * Called when a message arrives from the server
         * @param messageData the binary message data
         */
        void onMessage(byte[] messageData);
    }

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    private ZMQ.Socket socket;
    private Thread messageThread;
    private boolean running = false;
    private final MessageHandler messageHandler;

    /**
     * Constructor of the MessageSubscriber that takes a MessageHandler as a parameter.
     * @param messageHandler The message handler to consume messages.
     */
    MessageSubscriber(final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Connects to the server ZMQ publisher socket and start the thread to listen for new messages
     * @param context The ZMQ context
     * @param address The address of the publisher socket
     */
    synchronized void connect(ZContext context, String address) {
        if (!running) {
            this.socket = context.createSocket(SocketType.SUB);
            this.socket.setLinger(0);
            this.socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
            this.socket.connect(address);

            running = true;

            this.messageThread = new Thread(this::loop);
            this.messageThread.start();

            logger.debug("WebX Message Subscriber started");
        }
    }

    /**
     * Disconnects from the ZQM socket and stops the thread. The disconnect method
     * blocks until the thread has joined.
     */
    void disconnect() {
        if (this.running) {
            synchronized (this) {
                this.running = false;
            }

            try {
                this.messageThread.interrupt();
                this.messageThread.join();
                this.messageThread = null;

                this.socket.close();

                logger.debug("WebX Message Subscriber disconnected");

            } catch (InterruptedException exception) {
                logger.warn("Stop of WebX Subscriber thread interrupted");
            }
        }
    }

    /**
     * The main loop waiting for messages to be sent over the ZMQ socket. When a message arrives
     * it is sent to the message handler.
     */
    private void loop() {
        while (this.running) {
            try {
                byte[] messageData = socket.recv();
                this.messageHandler.onMessage(messageData);

            } catch (org.zeromq.ZMQException e) {
                if (this.running) {
                    logger.info("WebX Subscriber thread interrupted");
                }
            }
        }
    }
}
