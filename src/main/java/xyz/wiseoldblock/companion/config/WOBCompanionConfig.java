package xyz.wiseoldblock.companion.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration options for the WOB Companion mod.
 * Saved at config/wob_companion.json.
 */
public class WOBCompanionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("wob_companion");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "wob_companion.json");

    private boolean enabled = true;
    private String apiBaseUrl = "https://wiseoldblock.xyz/api";
    private String apiKey = "";
    private boolean debugLogging = false;

    public WOBCompanionConfig() {
    }

    public static WOBCompanionConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            WOBCompanionConfig def = new WOBCompanionConfig();
            def.save();
            return def;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            WOBCompanionConfig config = GSON.fromJson(reader, WOBCompanionConfig.class);
            if (config == null) {
                config = new WOBCompanionConfig();
            }
            config.validate();
            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to load WOB Companion config, using default settings: {}", e.getMessage());
            WOBCompanionConfig def = new WOBCompanionConfig();
            def.save();
            return def;
        }
    }

    public void save() {
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save WOB Companion config: {}", e.getMessage());
        }
    }

    public void validate() {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://wiseoldblock.xyz/api";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }
}
