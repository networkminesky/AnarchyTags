package anarchy.minesky.tags.config;

import anarchy.minesky.tags.AnarchyTags;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AnarchyTagsConfig {

    private final AnarchyTags plugin;
    private final NametagConfig nametagConfig;
    private final ScoreboardConfig scoreboardConfig;

    public AnarchyTagsConfig(AnarchyTags plugin) {
        this.plugin = plugin;
        this.nametagConfig = new NametagConfig(plugin);
        this.scoreboardConfig = new ScoreboardConfig(plugin);
    }

    public void load() {
        nametagConfig.load();
        scoreboardConfig.load();
    }

    public void reload() throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        nametagConfig.load(config);
        scoreboardConfig.load(config);
    }

    public NametagConfig getNametagConfig() {
        return nametagConfig;
    }

    public ScoreboardConfig getScoreboardConfig() {
        return scoreboardConfig;
    }
}
