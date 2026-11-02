package anarchy.minesky.tags.nick;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class NickHideStorage {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File file;
    private YamlConfiguration yaml;

    NickHideStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.file = new File(dataFolder, "nick-hide.yml");
    }

    /** Somente testes unitários (mesmo pacote). */
    NickHideStorage(File dataFolder) {
        this.plugin = null;
        this.dataFolder = dataFolder;
        this.file = new File(dataFolder, "nick-hide.yml");
    }

    void load() {
        if (!file.exists()) {
            yaml = new YamlConfiguration();
            return;
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    NickHideMode readMode(UUID playerId) {
        if (yaml == null || playerId == null) {
            return NickHideMode.OFF;
        }
        String raw = yaml.getString("players." + playerId + ".mode", null);
        if (raw == null) {
            raw = yaml.getString("players." + playerId, "off");
        }
        return NickHideMode.fromId(raw);
    }

    boolean readNickLineEnabled(UUID playerId) {
        if (yaml == null || playerId == null) {
            return true;
        }
        return yaml.getBoolean("players." + playerId + ".nick-line", true);
    }

    void writeMode(UUID playerId, NickHideMode mode) {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        String base = "players." + playerId;
        if (mode == null || mode == NickHideMode.OFF) {
            yaml.set(base + ".mode", null);
            if (!yaml.contains(base + ".nick-line")) {
                yaml.set(base, null);
            }
        } else {
            yaml.set(base + ".mode", mode.configId());
        }
        saveQuietly();
    }

    void writeNickLineEnabled(UUID playerId, boolean enabled) {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        String base = "players." + playerId;
        if (enabled) {
            yaml.set(base + ".nick-line", null);
            if (!yaml.contains(base + ".mode")) {
                yaml.set(base, null);
            }
        } else {
            yaml.set(base + ".nick-line", false);
        }
        saveQuietly();
    }

    private void saveQuietly() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Não foi possível salvar nick-hide.yml: " + e.getMessage());
            }
        }
    }
}
