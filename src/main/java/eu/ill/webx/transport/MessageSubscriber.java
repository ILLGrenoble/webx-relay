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

public class MessageSubscriber {
    public interface MessageListener { void onMessage(byte[] messageData); }

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    private ZMQ.Socket socket;

    private Thread messageThread;
    private boolean running = false;

    private final MessageListener messageListener;

    public MessageSubscriber(final MessageSubscriber.MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public synchronized void start(ZContext context, String address) {
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

    public void stop() {
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
                logger.error("Stop of WebX Subscriber thread interrupted");
            }
        }
    }

    public void loop() {
        while (this.running) {
            try {
                byte[] messageData = socket.recv();
                this.messageListener.onMessage(messageData);

            } catch (org.zeromq.ZMQException e) {
                if (this.running) {
                    logger.info("WebX Subscriber thread interrupted");
                }
            }
        }
    }
}
