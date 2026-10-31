package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

public class NametagScheduler {

    private final AnarchyTags plugin;
    private ScheduledTask task;

    public NametagScheduler(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            plugin.getLogger().warning("Nametags desativados na config — scheduler não iniciado.");
            return;
        }

        long intervalTicks = plugin.config().getNametagConfig().getUpdateInterval() * 20L;
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> {
            for (Nametag nametag : plugin.getNametagManager().getAll()) {
                nametag.updateVisibilityForAll();
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
