package eu.ill.webx.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Message implements Comparable<Message> {
    private final static int TYPE_OFFSET = 16;
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
