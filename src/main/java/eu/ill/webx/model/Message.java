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
package eu.ill.webx.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Encapsulates the raw binary message data from a WebX Engine.
 * The priority of the message is calculated from the type of message (mouse movement is considered priority to
 * improve the feedback from user interactions).
 */
public class Message implements Comparable<Message> {

    /**
     * Enum defining the type of the message
     */
    public enum Type {
        /**
         * An interrupt message (used internally)
         */
        INTERRUPT,

        /**
         * A Close message (used internally)
         */
        CLOSE,

        /**
         * A mouse movement message
         */
        MOUSE,

        /**
         * A cursor message
         */
        CURSOR,

        /**
         * A disconnect message
         */
        DISCONNECT,

        /**
         * Any other message
         */
        OTHER
    }

    private final static int TYPE_OFFSET = 32;
    private final byte[] data;
    private final Type type;
    private final Long timestamp;
    private final Integer priority;

    /**
     * The public constructor of a Message taking raw message data. The message header is analysed to determine the
     * type and therefore the priority.
     * @param data the binary data
     */
    public Message(byte[] data) {
        this.timestamp = new Date().getTime();
        this.data = data;
        int type = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(TYPE_OFFSET);
        if (type == 6) {
            this.type = Type.MOUSE;
            this.priority = 1;

        } else if (type == 7) {
            this.type = Type.CURSOR;
            this.priority = 2;

        } else if (type == 9) {
            this.type = Type.DISCONNECT;
            this.priority = 1;

        } else {
            this.type = Type.OTHER;
            this.priority = 3;
        }
    }

    /**
     * Constructor taking a message type and a priority.
     * These messages are created and handled internally by the relay.
     * @param type the type of message
     * @param priority the priority of the message
     */
    private Message(final Type type, final Integer priority) {
        this.timestamp = new Date().getTime();
        this.data = null;
        this.type = type;
        this.priority = priority;
    }

    /**
     * Constructor taking a message type, a priority and raw data.
     * These messages are created and handled internally by the relay.
     * @param type the type of message
     * @param priority the priority of the message
     * @param message The raw data
     */
    private Message(final Type type, final Integer priority, byte[] message) {
        this.timestamp = new Date().getTime();
        this.data = message;
        this.type = type;
        this.priority = priority;
    }

    /**
     * Returns the raw data
     * @return the raw data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the data as a string
     * @return the data as a string
     */
    public String getStringData() {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Returns the message type
     * @return the message type
     */
    public Type getType() {
        return type;
    }

    /**
     * Comparison function. Lower numbers are considered more important.
     * If two messages with identical priorities, the timestamp is used (older message more important).
     * @param msg the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(Message msg) {
        // Order primarily by priority
        int priorityComparison = this.priority.compareTo(msg.priority);
        if (priorityComparison == 0) {
            // Otherwise, by timestamp
            return this.timestamp.compareTo(msg.timestamp);
        }

        return priorityComparison;
    }

    /**
     * Creates a Interrupt message (used internally)
     */
    public static class InterruptMessage extends Message {
        /**
         * Constructor with a message on why the interrupt is generated
         * @param message The interrupt message
         */
        public InterruptMessage(String message) {
            super(Type.INTERRUPT, 0, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Creates a Close message (used internally)
     */
    public static class CloseMessage extends Message {
        /**
         * Default constructor
         */
        public CloseMessage() {
            super(Type.CLOSE, 0);
        }
    }
}
