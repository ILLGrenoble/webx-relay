package eu.ill.webx.transport.serializer;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class WebXInstructionBuffer {

    private final ByteBuffer buffer;
    private final Header     header;
    private       int        readOffset;

    public WebXInstructionBuffer(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
        this.buffer.order(LITTLE_ENDIAN);
        this.header = new Header(bytes);
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

    private int getNextReadOffset(int sizeOfData) {
        // Ensure alignment
        int padding = (readOffset % sizeOfData) > 0 ? sizeOfData - (readOffset % sizeOfData) : 0;
        int position = readOffset + padding;

        readOffset += sizeOfData + padding;

        return position;
    }

    static class Header {
        private int     instructionTypeId;
        private int     messageId;
        private int     bufferLength;
        private boolean synchronous;

        public Header(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(LITTLE_ENDIAN);
            this.instructionTypeId = buffer.getInt();
            this.synchronous = (0x80000000 & this.instructionTypeId) != 0;
            this.messageId = buffer.getInt();
            this.bufferLength = buffer.getInt();
        }

        public int getInstructionTypeId() {
            return instructionTypeId;
        }

        public int getMessageId() {
            return messageId;
        }

        public int getBufferLength() {
            return bufferLength;
        }

        public boolean isSynchronous() {
            return synchronous;
        }
    }
}
