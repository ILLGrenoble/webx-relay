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

import eu.ill.webx.model.MessageListener;
import eu.ill.webx.utils.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;

public class MessageSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    private ZMQ.Socket socket;

    private Thread thread;
    private boolean running = false;

    private final List<MessageListener> listeners = new ArrayList<>();

    public MessageSubscriber() {
    }

    public synchronized void start(ZContext context, String address) {
        if (!running) {
            this.socket = context.createSocket(SocketType.SUB);
            this.socket.setLinger(0);
            this.socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
            this.socket.connect(address);

            running = true;

            this.thread = new Thread(this::loop);
            this.thread.start();

            logger.debug("WebX Message Subscriber started");
        }
    }

    public void stop() {
        if (this.running) {
            synchronized (this) {
                this.running = false;
            }

            try {
                this.thread.interrupt();
                this.thread.join();
                this.thread = null;

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
                logger.trace("Got message of length {}: {}", messageData.length, HexString.toDebugString(messageData, 32));
                this.notifyListeners(messageData);

            } catch (org.zeromq.ZMQException e) {
                if (this.running) {
                    logger.info("WebX Subscriber thread interrupted");
                }
            }
        }
    }

    synchronized public void addListener(MessageListener listener) {
        this.listeners.add(listener);
    }

    synchronized public void removeListener(MessageListener listener) {
        this.listeners.remove(listener);
    }

    synchronized private void notifyListeners(byte[] messageData) {
        this.listeners.forEach(listener -> listener.onMessage(messageData));
    }
}
