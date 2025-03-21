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

import java.util.concurrent.LinkedBlockingDeque;

public class InstructionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InstructionPublisher.class);

    private ZMQ.Socket socket;
    private final LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private Thread instructionThread;
    private boolean connected = false;

    InstructionPublisher() {
    }

    public synchronized void connect(ZContext context, String address) {
        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.PUB);
            this.socket.setLinger(0);
            this.socket.connect(address);

            this.connected = true;

            this.instructionThread = new Thread(this::instructionLoop);
            this.instructionThread.start();

            try {
                // Hackityhack Add a sleep to ensure that the socket is connected
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            logger.debug("WebX Instruction Publisher connected");
        }
    }

    public synchronized void disconnect() {
        if (this.connected) {
            try {
                this.connected = false;
                this.instructionQueue.clear();

                this.socket.close();
                this.socket = null;

                this.instructionThread.interrupt();
                this.instructionThread.join();
                this.instructionThread = null;

                logger.debug("WebX Instruction Publisher disconnected");

            } catch (InterruptedException exception) {
                logger.error("Stop of instruction publisher threads interrupted", exception);
            }
        }
    }


    public synchronized void queueInstruction(byte[] instructionData) {
        try {
            this.instructionQueue.put(instructionData);

        } catch (InterruptedException exception) {
            logger.error("Interrupted when adding instruction to instruction queue");
        }
    }


    private void instructionLoop() {
        while (this.connected) {
            try {
                final byte[] instructionData = this.instructionQueue.take();

                if (this.connected) {
                    this.socket.send(instructionData, 0);
                }

            } catch (InterruptedException exception) {
                if (this.connected) {
                    logger.info("Instruction loop thread interrupted");
                }
            }
        }
    }
}
