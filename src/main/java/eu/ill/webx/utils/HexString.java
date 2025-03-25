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
package eu.ill.webx.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Utility class to convert binary bytearrays into hexadecimal strings and vice versa.
 */
public class HexString {
    private static final Logger logger = LoggerFactory.getLogger(HexString.class);

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    /**
     * Default constructor
     */
    public HexString() {
    }

    /**
     * Converts byte data to a hex string using full length of byte data.
     * The data is formatted for easier debugging.
     * @param data the input binary data
     * @return hexadecimal representation of the data
     */
    public static String toDebugString(byte[] data) {
        return HexString.toDebugString(data, data.length);
    }

    /**
     * Converts byte data to hex string using a fixed maximum length of data
     * The data is formatted for easier debugging.
     * @param data the input binary data
     * @param length The maximum length of the string
     * @return hexadecimal representation of the data
     */
    public static String toDebugString(byte[] data, int length) {
        length = Math.min(length, data.length);

        byte[] hexChars = new byte[length * 3 - 1];
        for (int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if (j < length - 1) {
                hexChars[j * 3 + 2] = ' ';
            }
        }
        return "[" + new String(hexChars, StandardCharsets.UTF_8) + (data.length > length ? " ...]" : "]");
    }

    /**
     * Converts byte data to hex string
     * @param data the input binary data
     * @return hexadecimal representation of the data
     */
    public static String fromByteArray(byte[] data) {
        return fromByteArray(data, data.length);
    }

    /**
     * Converts byte data to hex string using a maximum length of the data
     * @param data the input binary data
     * @return hexadecimal representation of the data
     * @param length The maximum length of the string
     */
    public static String fromByteArray(byte[] data, int length) {
        byte[] hexChars = new byte[length * 2];
        for (int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Converts from a hexadecimal string to a byte array. Validation is made of the expected byte length.
     * @param hexString the input hex string
     * @param expectedBytesLength the expected number of bytes
     * @return the byte array equivalent to the hex string
     */
    public static byte[] toByteArray(String hexString, int expectedBytesLength) {
        if (expectedBytesLength != hexString.length() / 2) {
            logger.error("Received invalid HexString of expected length {}: {}", expectedBytesLength, hexString);
        }

        int length = hexString.length();
        byte[] data = new byte[length / 2];
        int index = 0;
        for (int i = 0; i < length; i += 2) {
            data[index++] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)  + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
