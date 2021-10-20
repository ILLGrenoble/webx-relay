package eu.ill.webx.transport.serializer;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXMessageBuffer {

    private ByteBuffer buffer;
    private Header header;
    private int bufferLength;
    private int writeOffset;
    private int readOffset;


    public WebXMessageBuffer(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
        this.buffer.order(LITTLE_ENDIAN);
        this.bufferLength = bytes.length;
        this.header = new Header(bytes);
        this.writeOffset = 16;
        this.readOffset = 16;
    }

    public Header getHeader() {
        return header;
    }

    public long getLong() {
        int offset = getNextReadOffset(8);
        return this.buffer.getLong(offset);
    }

    public int getInt() {
        int offset = getNextReadOffset(4);
        return this.buffer.getInt(offset);
    }


    public String getString(int length) {
        return new String(this.buffer.array(), this.readOffset, length);
    }

    private int getNextWriteOffset(int sizeOfData) {
        // Ensure alignment
        int padding = (writeOffset % sizeOfData) > 0 ? sizeOfData - (writeOffset % sizeOfData) : 0;
        int position = writeOffset + padding;

        writeOffset += sizeOfData + padding;

        return position;
    }

    private int getNextReadOffset(int sizeOfData) {
        // Ensure alignment
        int padding = (readOffset % sizeOfData) > 0 ? sizeOfData - (readOffset % sizeOfData) : 0;
        int position = readOffset + padding;

        readOffset += sizeOfData + padding;

        return position;
    }

    static class Header {
        private int messageTypeId;
        private int messageId;
        private int bufferLength;

        public Header(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(LITTLE_ENDIAN);
            this.messageTypeId = buffer.getInt();
            this.messageId = buffer.getInt();
            this.bufferLength = buffer.getInt();
        }

        public int getMessageTypeId() {
            return messageTypeId;
        }

        public int getMessageId() {
            return messageId;
        }

        public int getBufferLength() {
            return bufferLength;
        }
    }
}
