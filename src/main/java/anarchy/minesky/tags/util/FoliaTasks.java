package anarchy.minesky.tags.util;

import anarchy.minesky.tags.AnarchyTags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FoliaTasks {

    private FoliaTasks() {}

    public static void runAsync(AnarchyTags plugin, Runnable action) {
        if (plugin == null || action == null) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, task -> action.run());
    }

    public static void runOnEntity(AnarchyTags plugin, Player player, Runnable action) {
        if (plugin == null || player == null || !player.isOnline() || action == null) {
            return;
        }
        player.getScheduler().run(plugin, task -> action.run(), null);
    }

    public static void runOnEntityDelayed(AnarchyTags plugin, Player player, Runnable action, long delayTicks) {
        if (plugin == null || player == null || !player.isOnline() || action == null) {
            return;
        }
        player.getScheduler().runDelayed(plugin, task -> action.run(), null, delayTicks);
    }
}
