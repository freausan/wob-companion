package xyz.wiseoldblock.companion;

import org.junit.jupiter.api.Test;
import xyz.wiseoldblock.companion.config.WOBCompanionConfig;

import static org.junit.jupiter.api.Assertions.*;

class WOBCompanionConfigTest {

    @Test
    void testDefaultValues() {
        WOBCompanionConfig config = new WOBCompanionConfig();
        assertTrue(config.isEnabled());
        assertEquals("https://wiseoldblock.xyz/api", config.getApiBaseUrl());
        assertEquals("", config.getApiKey());
        assertFalse(config.isDebugLogging());
    }

    @Test
    void testValidation() {
        WOBCompanionConfig config = new WOBCompanionConfig();
        config.setApiBaseUrl("");
        config.setApiKey(null);
        config.validate();

        assertEquals("https://wiseoldblock.xyz/api", config.getApiBaseUrl());
        assertEquals("", config.getApiKey());
    }
}
