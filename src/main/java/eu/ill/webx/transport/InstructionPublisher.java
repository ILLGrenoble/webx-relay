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

public class InstructionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(InstructionPublisher.class);

    private ZMQ.Socket socket;

    public InstructionPublisher() {
    }

    public void connect(ZContext context, String address) {
        if (this.socket == null) {
            this.socket = context.createSocket(SocketType.PUB);
            this.socket.setLinger(0);
            this.socket.connect(address);

            try {
                // Hackityhack Add a sleep to ensure that the socket is connected
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            logger.debug("WebX Instruction Publisher connected");
        }
    }

    public void disconnect() {
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;

            logger.debug("WebX Instruction Publisher disconnected");
        }
    }

    public synchronized void sendInstructionData(byte[] requestData) {
        this.socket.send(requestData, 0);
    }
}
