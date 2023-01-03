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

import java.nio.charset.StandardCharsets;

public class HexString {
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String toString(byte[] data) {
        return HexString.toString(data, data.length);
    }

    public static String toString(byte[] data, int length) {
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
}
