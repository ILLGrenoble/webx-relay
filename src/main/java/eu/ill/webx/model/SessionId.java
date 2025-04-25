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


import eu.ill.webx.utils.HexString;

import java.nio.ByteBuffer;

/**
 * Encapsulates the binary session Id value. This is used for filtering of messages from a WebX Engine.
 * The Session Id is 16 bytes. We store this also as two long values that are used for equality comparison.
 */
public class SessionId {
    private final long lower;
    private final long upper;
    private final byte[] sessionId;

    private String hexString;

    /**
     * Constructor with a hex string representation of the binary session Id. the binary equivalent
     * and upper and lower long values are created.
     * @param sessionIdString The hex representation of the session Id
     */
    public SessionId(String sessionIdString) {
        this.hexString = sessionIdString;
        this.sessionId = HexString.toByteArray(sessionIdString, 16);

        ByteBuffer sessionIdBuffer = ByteBuffer.wrap(sessionId);
        this.upper = sessionIdBuffer.getLong();
        this.lower = sessionIdBuffer.getLong();
    }

    /**
     * Constructor taking raw binary session Id data. The lower and upper long values are calculator.
     * @param sessionId the binary session Id
     */
    public SessionId(byte[] sessionId) {
        this.sessionId = sessionId;

        ByteBuffer sessionIdBuffer = ByteBuffer.wrap(sessionId);
        this.upper = sessionIdBuffer.getLong();
        this.lower = sessionIdBuffer.getLong();
    }

    /**
     * Returns the hex representation of the session Id
     * @return the hex representation of the session Id
     */
    public String hexString() {
        if (hexString == null) {
            this.hexString = HexString.fromByteArray(sessionId);
        }
        return hexString;
    }

    /**
     * Returns the raw binary session Id
     * @return the raw binary session Id
     */
    public byte[] bytes() {
        return sessionId;
    }

    /**
     * Equality comparison comparing the upper and lower long values of the session Id
     * @param o the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SessionId sessionId = (SessionId) o;
        return lower == sessionId.lower && upper == sessionId.upper;
    }

    /**
     * Returns the hashcode of the session Id generated from the lower and upper long values
     * @return the hashcode of the session Id
     */
    @Override
    public int hashCode() {
        int result = Long.hashCode(lower);
        result = 31 * result + Long.hashCode(upper);
        return result;
    }
}
