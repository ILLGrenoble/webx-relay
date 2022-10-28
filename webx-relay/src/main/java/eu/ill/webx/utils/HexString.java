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
