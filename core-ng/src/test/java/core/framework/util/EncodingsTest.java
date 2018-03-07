package core.framework.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class EncodingsTest {
    @Test
    void base64() {
        assertEquals("", Encodings.base64(""));
        // from http://en.wikipedia.org/wiki/Base64
        assertEquals("bGVhc3VyZS4=", Encodings.base64("leasure."));
    }

    @Test
    void decodeBase64() {
        // from http://en.wikipedia.org/wiki/Base64
        assertEquals("leasure.", new String(Encodings.decodeBase64("bGVhc3VyZS4="), Charsets.UTF_8));
    }

    @Test
    void base64URLSafe() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        String encodedMessage = Encodings.base64URLSafe(bytes);
        assertArrayEquals(bytes, Encodings.decodeBase64URLSafe(encodedMessage));
    }

    @Test
    void uriComponent() {
        assertEquals("%E2%9C%93", Encodings.uriComponent("✓"), "encode utf-8");
        assertEquals("a%20b", Encodings.uriComponent("a b"));
        assertEquals("a%2Bb", Encodings.uriComponent("a+b"));
        assertEquals("a%3Db", Encodings.uriComponent("a=b"));
        assertEquals("a%3Fb", Encodings.uriComponent("a?b"));
        assertEquals("a%2Fb", Encodings.uriComponent("a/b"));
        assertEquals("a%26b", Encodings.uriComponent("a&b"));
        assertEquals("a%25b", Encodings.uriComponent("a%b"));
    }

    @Test
    void decodeURIComponent() {
        assertEquals("✓", Encodings.decodeURIComponent("%E2%9C%93"), "decode utf-8");
        assertEquals("a b", Encodings.decodeURIComponent("a%20b"));
        assertEquals("a+b", Encodings.decodeURIComponent("a+b"));
        assertEquals("a=b", Encodings.decodeURIComponent("a=b"));
        assertEquals("a?b", Encodings.decodeURIComponent("a%3Fb"));
        assertEquals("a/b", Encodings.decodeURIComponent("a%2Fb"));
        assertEquals("a&b", Encodings.decodeURIComponent("a&b"));
        assertEquals("a%b", Encodings.decodeURIComponent("a%25b"));
    }
}
