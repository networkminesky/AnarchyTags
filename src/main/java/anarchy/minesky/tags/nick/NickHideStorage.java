package anarchy.minesky.tags.nick;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class NickHideStorage {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    NickHideStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nick-hide.yml");
    }

    void load() {
        if (!file.exists()) {
            yaml = new YamlConfiguration();
            return;
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    NickHideMode read(UUID playerId) {
        if (yaml == null || playerId == null) {
            return NickHideMode.OFF;
        }
        String raw = yaml.getString("players." + playerId, "off");
        return NickHideMode.fromId(raw);
    }

    void write(UUID playerId, NickHideMode mode) {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        if (mode == null || mode == NickHideMode.OFF) {
            yaml.set("players." + playerId, null);
        } else {
            yaml.set("players." + playerId, mode.configId());
        }
        saveQuietly();
    }

    private void saveQuietly() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Não foi possível salvar nick-hide.yml: " + e.getMessage());
        }
    }
}
