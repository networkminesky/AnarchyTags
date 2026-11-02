package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.util.FoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NametagScheduler {

    private static final int TICKS = 20;

    private final AnarchyTags plugin;
    private ScheduledTask task;
    private int sortElapsedTicks;

    public NametagScheduler(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            plugin.getLogger().warning("Nametags desativados na config — scheduler não iniciado.");
            return;
        }

        sortElapsedTicks = 0;
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), TICKS, TICKS);
    }

    private void tick() {
        NametagConfig config = plugin.config().getNametagConfig();
        List<Nametag> tags = new ArrayList<>(plugin.getNametagManager().getAll());
        for (Nametag nametag : tags) {
            Player owner = nametag.getPlayer();
            if (owner == null || !owner.isOnline()) {
                continue;
            }
            FoliaTasks.runOnEntity(plugin, owner, nametag::followAllViewers);
        }

        sortElapsedTicks += TICKS;
        int sortInterval = Math.max(60, config.getSortingInterval() * 20);
        if (sortElapsedTicks >= sortInterval) {
            sortElapsedTicks = 0;
            plugin.getNametagManager().tabOrder().recomputeChangedAsync();
        }
        plugin.getNametagManager().tabOrder().tick(config.getSortingSyncPerTick());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
