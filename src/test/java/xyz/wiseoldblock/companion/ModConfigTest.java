package xyz.wiseoldblock.companion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.wiseoldblock.companion.config.ModConfig;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {

    @Test
    void testDefaults() {
        ModConfig config = new ModConfig();
        assertEquals("https://api.wiseoldblock.xyz", config.getBackendUrl());
        assertTrue(config.isEnabled());
        assertTrue(config.isNotifyInChat());
    }

    @Test
    void testUrlNormalization() {
        assertEquals("https://api.wiseoldblock.xyz", ModConfig.normalizeUrl("api.wiseoldblock.xyz"));
        assertEquals("https://api.wiseoldblock.xyz", ModConfig.normalizeUrl("api.wiseoldblock.xyz/"));
        assertEquals("http://localhost:3001", ModConfig.normalizeUrl("http://localhost:3001"));
        assertEquals("http://localhost:3001", ModConfig.normalizeUrl("http://localhost:3001/"));
        assertEquals("https://custom.domain.com/api", ModConfig.normalizeUrl("custom.domain.com/api///"));
        assertEquals("https://api.wiseoldblock.xyz", ModConfig.normalizeUrl(""));
        assertEquals("https://api.wiseoldblock.xyz", ModConfig.normalizeUrl(null));
    }

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) {
        Path configFile = tempDir.resolve("config/wise-old-block.json");
        ModConfig original = new ModConfig();
        original.setBackendUrl("http://localhost:8080");
        original.setEnabled(false);
        original.setNotifyInChat(false);
        original.saveTo(configFile);

        ModConfig loaded = ModConfig.loadFrom(configFile);
        assertEquals("http://localhost:8080", loaded.getBackendUrl());
        assertFalse(loaded.isEnabled());
        assertFalse(loaded.isNotifyInChat());
    }
}