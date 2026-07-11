package com.CredentialCoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CoderTest {

    @Test
    public void encodeAndDecodeRoundTrip() throws Exception {
        String input = "daily-stock-report";

        String encoded = Coder.Encode(input);

        assertTrue(encoded.startsWith("AES:"));
        assertFalse(encoded.contains(input));
        assertEquals(input, Coder.decode(encoded));
    }

    @Test
    public void decodeRejectsValuesWithoutEncryptedPayload() throws Exception {
        assertEquals("unable to Decryptplain-text", Coder.decode("plain-text"));
    }
}
