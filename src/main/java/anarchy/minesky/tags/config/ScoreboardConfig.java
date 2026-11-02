package anarchy.minesky.tags.config;

import anarchy.minesky.tags.AnarchyTags;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoreboardConfig {

    public record ToggleLine(String id, String label, String line, boolean defaultEnabled) {}

    private final AnarchyTags plugin;

    private boolean enabled;
    private int updateIntervalSeconds;
    private String title;
    private List<String> fixedLines;
    private List<ToggleLine> toggles;

    public ScoreboardConfig(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void load() {
        load(plugin.getConfig());
    }

    public void load(FileConfiguration config) {
        enabled = config.getBoolean("scoreboard.enabled", true);
        updateIntervalSeconds = Math.max(1, config.getInt("scoreboard.update-interval-seconds", 2));
        title = config.getString("scoreboard.title", "&#a3feffᴍɪɴᴇsᴋʏ.ᴄᴏᴍ.ʙʀ");
        fixedLines = config.getStringList("scoreboard.fixed-lines");
        if (fixedLines.isEmpty()) {
            fixedLines = List.of("&f<head:%player%> %displayname%");
        }

        toggles = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("scoreboard.toggles");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                String line = entry.getString("line", "");
                if (line.isBlank()) {
                    continue;
                }
                String label = entry.getString("label", id);
                boolean defaultEnabled = entry.getBoolean("default", true);
                toggles.add(new ToggleLine(id, label, line, defaultEnabled));
            }
        }
        if (toggles.isEmpty()) {
            toggles = defaultToggles();
        }
    }

    private static List<ToggleLine> defaultToggles() {
        List<ToggleLine> list = new ArrayList<>();
        list.add(new ToggleLine("money", "Saldo ($)", "&#9dff00&l$ &f%anarchyeconomy_formatted%", true));
        list.add(new ToggleLine("shards", "Shards", "&#e436ff⚗ &f%anarchyeconomy_shards_formatted%", true));
        list.add(new ToggleLine("kills", "Kills", "&fKills: &7%statistic_player_kills%", false));
        list.add(new ToggleLine("deaths", "Mortes", "&fMortes: &7%statistic_deaths%", false));
        list.add(new ToggleLine("playtime", "Tempo jogado", "&fPlaytime: &7%statistic_time_played%", false));
        list.add(new ToggleLine("ping", "Ping",
                "&f<mineskin:4d44c0c505744069a3b09d5ff5fb3960> &f%ping%ms", true));
        return list;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getFixedLines() {
        return fixedLines;
    }

    public List<ToggleLine> getToggles() {
        return toggles;
    }

    public Map<String, ToggleLine> togglesById() {
        Map<String, ToggleLine> map = new LinkedHashMap<>();
        for (ToggleLine toggle : toggles) {
            map.put(toggle.id(), toggle);
        }
        return Collections.unmodifiableMap(map);
    }
}
