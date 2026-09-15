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
 * Configuration options for Wise Old Block Companion.
 * Saved at config/wise-old-block.json.
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("wob_companion");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", "wise-old-block.json");

    public static final String DEFAULT_BACKEND_URL = "https://api.wiseoldblock.xyz";

    private String backendUrl = DEFAULT_BACKEND_URL;
    private boolean enabled = true;
    private boolean notifyInChat = true;

    public ModConfig() {
    }

    public static ModConfig load() {
        return loadFrom(CONFIG_PATH);
    }

    public static ModConfig loadFrom(Path path) {
        if (!Files.exists(path)) {
            ModConfig def = new ModConfig();
            def.saveTo(path);
            return def;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            config.validate();
            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to load Wise Old Block config, falling back to default settings: {}", e.getMessage());
            ModConfig def = new ModConfig();
            def.saveTo(path);
            return def;
        }
    }

    public void save() {
        saveTo(CONFIG_PATH);
    }

    public void saveTo(Path path) {
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save Wise Old Block config: {}", e.getMessage());
        }
    }

    public void validate() {
        if (backendUrl == null || backendUrl.isBlank()) {
            backendUrl = DEFAULT_BACKEND_URL;
        } else {
            backendUrl = normalizeUrl(backendUrl);
        }
    }

    public static String normalizeUrl(String url) {
        if (url == null) {
            return DEFAULT_BACKEND_URL;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_BACKEND_URL;
        }

        // Add scheme if missing
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }

        // Strip trailing slashes
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = normalizeUrl(backendUrl);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNotifyInChat() {
        return notifyInChat;
    }

    public void setNotifyInChat(boolean notifyInChat) {
        this.notifyInChat = notifyInChat;
    }
}