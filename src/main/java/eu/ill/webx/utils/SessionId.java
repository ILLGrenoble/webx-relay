package eu.ill.webx.utils;


import java.nio.ByteBuffer;

public class SessionId {
    private final long lower;
    private final long upper;
    byte[] sessionId;

    String hexString;

    public SessionId(String sessionIdString) {
        this.hexString = sessionIdString;
        this.sessionId = HexString.toByteArray(sessionIdString, 16);

        ByteBuffer sessionIdBuffer = ByteBuffer.wrap(sessionId);
        this.upper = sessionIdBuffer.getLong();
        this.lower = sessionIdBuffer.getLong();
    }

    public SessionId(byte[] sessionId) {
        this.sessionId = sessionId;

        ByteBuffer sessionIdBuffer = ByteBuffer.wrap(sessionId);
        this.upper = sessionIdBuffer.getLong();
        this.lower = sessionIdBuffer.getLong();
    }

    public String hexString() {
        if (hexString == null) {
            this.hexString = HexString.fromByteArray(sessionId);
        }
        return hexString;
    }

    public byte[] bytes() {
        return sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SessionId sessionId = (SessionId) o;
        return lower == sessionId.lower && upper == sessionId.upper;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(lower);
        result = 31 * result + Long.hashCode(upper);
        return result;
    }
}
