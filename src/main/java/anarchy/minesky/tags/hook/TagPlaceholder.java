package anarchy.minesky.tags.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import anarchy.minesky.tags.AnarchyTags;

public final class TagPlaceholder extends PlaceholderExpansion {

    private final AnarchyTags plugin;

    public TagPlaceholder(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "anarchytags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Bruno C. (zBrunoC)";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }
        if (!params.equalsIgnoreCase("playtime")) {
            return null;
        }

        Player online = offlinePlayer.getPlayer();
        long ticks = online != null
                ? online.getStatistic(Statistic.PLAY_ONE_MINUTE)
                : offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);

        long totalSeconds = Math.max(0L, ticks / 20L);
        return PlaytimeFormatter.format(totalSeconds);
    }

    public final class PlaytimeFormatter {

        private static final long SECONDS_PER_MINUTE = 60L;
        private static final long SECONDS_PER_HOUR = 3_600L;
        private static final long SECONDS_PER_DAY = 86_400L;

        private PlaytimeFormatter() {}

        public static String format(long totalSeconds) {
            if (totalSeconds <= 0L) {
                return "0s";
            }

            if (totalSeconds < SECONDS_PER_MINUTE) {
                return totalSeconds + "s";
            }

            if (totalSeconds < SECONDS_PER_HOUR) {
                long minutes = totalSeconds / SECONDS_PER_MINUTE;
                long seconds = totalSeconds % SECONDS_PER_MINUTE;
                return minutes + "m " + seconds + "s";
            }

            if (totalSeconds < SECONDS_PER_DAY) {
                long hours = totalSeconds / SECONDS_PER_HOUR;
                long minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
                return hours + "h " + minutes + "m";
            }

            long days = totalSeconds / SECONDS_PER_DAY;
            long hours = (totalSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
            return days + "d " + hours + "h";
        }
    }
}
