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
        } else {
            yaml.set(base + ".mode", mode.configId());
        }
        pruneIfEmpty(base);
        saveQuietly();
    }

    void writeNickLineEnabled(UUID playerId, boolean enabled) {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        String base = "players." + playerId;
        if (enabled) {
            yaml.set(base + ".nick-line", null);
        } else {
            yaml.set(base + ".nick-line", false);
        }
        pruneIfEmpty(base);
        saveQuietly();
    }

    QuotaSnapshot readQuota(UUID playerId) {
        if (yaml == null || playerId == null) {
            return QuotaSnapshot.empty();
        }
        String base = "players." + playerId;
        return new QuotaSnapshot(
                yaml.getString(base + ".quota-day", ""),
                yaml.getLong(base + ".used-millis", 0L),
                yaml.getBoolean(base + ".warn-90", false)
        );
    }

    void writeQuota(UUID playerId, QuotaSnapshot snapshot) {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        if (playerId == null) {
            return;
        }
        String base = "players." + playerId;
        if (snapshot == null || snapshot.isEmpty()) {
            yaml.set(base + ".quota-day", null);
            yaml.set(base + ".used-millis", null);
            yaml.set(base + ".warn-90", null);
        } else {
            yaml.set(base + ".quota-day", snapshot.day());
            yaml.set(base + ".used-millis", snapshot.usedMillis());
            yaml.set(base + ".warn-90", snapshot.warned() ? true : null);
        }
        pruneIfEmpty(base);
        saveQuietly();
    }

    private void pruneIfEmpty(String base) {
        if (yaml.getConfigurationSection(base) == null) {
            return;
        }
        if (!yaml.contains(base + ".mode")
                && !yaml.contains(base + ".nick-line")
                && !yaml.contains(base + ".quota-day")
                && !yaml.contains(base + ".used-millis")
                && !yaml.contains(base + ".warn-90")) {
            yaml.set(base, null);
        }
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

    record QuotaSnapshot(String day, long usedMillis, boolean warned) {
        static QuotaSnapshot empty() {
            return new QuotaSnapshot("", 0L, false);
        }

        boolean isEmpty() {
            return (day == null || day.isBlank()) && usedMillis <= 0 && !warned;
        }
    }
}
