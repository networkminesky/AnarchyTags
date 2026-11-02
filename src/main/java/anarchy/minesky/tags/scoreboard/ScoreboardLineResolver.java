package anarchy.minesky.tags.scoreboard;

import anarchy.minesky.tags.utils.helpers.PlaceholderHelper;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class ScoreboardLineResolver {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private ScoreboardLineResolver() {}

    public static String resolve(Player viewer, String template) {
        if (template == null || template.isBlank()) {
            return "";
        }
        String line = template
                .replace("%player%", viewer.getName())
                .replace("%ping%", String.valueOf(viewer.getPing()))
                .replace("%displayname%", LEGACY.serialize(viewer.displayName()));
        return PlaceholderHelper.applyTarget(viewer, line);
    }
}
