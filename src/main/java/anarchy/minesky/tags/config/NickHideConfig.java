package anarchy.minesky.tags.config;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.hook.LuckPermsHook;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NickHideConfig {

    static final long UNLIMITED_MILLIS = Long.MAX_VALUE / 4;

    private final AnarchyTags plugin;

    private long defaultDurationMillis;
    private long warnRemainingMillis;
    private Map<String, Long> groupDurations = Map.of();

    public NickHideConfig(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void load() {
        load(plugin.getConfig());
    }

    public void load(FileConfiguration config) {
        defaultDurationMillis = minutesToMillis(config.getInt("nick-hide.duration-minutes", 20));
        warnRemainingMillis = Math.max(0, config.getInt("nick-hide.warn-remaining-seconds", 90)) * 1000L;

        Map<String, Long> groups = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("nick-hide.group-durations");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                groups.put(key.toLowerCase(Locale.ROOT), minutesToMillis(section.getInt(key)));
            }
        }
        groupDurations = Map.copyOf(groups);
    }

    public long durationMillisFor(Player player) {
        if (player == null) {
            return defaultDurationMillis;
        }
        Set<String> groups = LuckPermsHook.inheritedGroupNames(player);
        long best = -1L;
        for (String group : groups) {
            Long mapped = groupDurations.get(group);
            if (mapped != null) {
                best = Math.max(best, mapped);
            }
        }
        return best >= 0 ? best : defaultDurationMillis;
    }

    public long getWarnRemainingMillis() {
        return warnRemainingMillis;
    }

    public boolean isUnlimited(long durationMillis) {
        return durationMillis >= UNLIMITED_MILLIS;
    }

    private static long minutesToMillis(int minutes) {
        if (minutes < 0) {
            return UNLIMITED_MILLIS;
        }
        return minutes * 60_000L;
    }
}
