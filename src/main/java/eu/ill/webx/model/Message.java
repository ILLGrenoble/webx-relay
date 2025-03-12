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

public class Message implements Comparable<Message> {
    public final static int MESSAGE_HEADER_LENGTH = 48;
    private final static int TYPE_OFFSET = 32;
    private final byte[] data;
    private final Type type;
    private final Long timestamp;
    private final Integer priority;

    protected Message(final Type type, final Integer priority) {
        this.timestamp = new Date().getTime();
        this.data = null;
        this.type = type;
        this.priority = priority;
    }

    protected Message(final Type type, final Integer priority, byte[] message) {
        this.timestamp = new Date().getTime();
        this.data = message;
        this.type = type;
        this.priority = priority;
    }

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

    public byte[] getData() {
        return data;
    }

    public String getStringData() {
        return new String(data, StandardCharsets.UTF_8);
    }

    public Type getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Integer getPriority() {
        return priority;
    }

    @Override
    public int compareTo(Message msg) {
        // Order primarily by priority
        int priorityComparison = this.getPriority().compareTo(msg.getPriority());
        if (priorityComparison == 0) {
            // Otherwise, by timestamp
            return this.getTimestamp().compareTo(msg.getTimestamp());
        }

        return priorityComparison;
    }

    public static enum Type {
        INTERRUPT,
        CLOSE,
        MOUSE,
        CURSOR,
        DISCONNECT,
        OTHER
    }

    public static class InterruptMessage extends Message {
        public InterruptMessage(String message) {
            super(Type.INTERRUPT, 0, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class CloseMessage extends Message {
        public CloseMessage() {
            super(Type.CLOSE, 0);
        }
    }
}
