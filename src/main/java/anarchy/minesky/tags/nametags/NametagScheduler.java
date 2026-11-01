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
    private static final int TAGS_PER_SECOND = 10;
    private static final int SPAWNS_PER_TAG = 6;

    private final AnarchyTags plugin;
    private ScheduledTask task;
    private int elapsedTicks;
    private int sortElapsedTicks;
    private int cursor;

    public NametagScheduler(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            plugin.getLogger().warning("Nametags desativados na config — scheduler não iniciado.");
            return;
        }

        elapsedTicks = 0;
        sortElapsedTicks = 0;
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), TICKS, TICKS);
    }

    private void tick() {
        NametagConfig config = plugin.config().getNametagConfig();
        List<Nametag> tags = new ArrayList<>(plugin.getNametagManager().getAll());

        elapsedTicks += TICKS;
        int textInterval = Math.max(60, config.getUpdateInterval() * 20);
        boolean refreshText = elapsedTicks >= textInterval;
        if (refreshText) {
            elapsedTicks = 0;
        }

        if (!tags.isEmpty()) {
            int size = tags.size();
            int start = Math.floorMod(cursor++, size);
            int processed = 0;
            for (int i = 0; i < size && processed < TAGS_PER_SECOND; i++) {
                Nametag nametag = tags.get((start + i) % size);
                Player owner = nametag.getPlayer();
                if (owner == null || !owner.isOnline()) {
                    continue;
                }
                processed++;
                final boolean doRefresh = refreshText;
                FoliaTasks.runOnEntity(plugin, owner, () -> {
                    nametag.updateVisibility(SPAWNS_PER_TAG);
                    if (doRefresh) {
                        nametag.refreshTexts(1);
                    }
                });
            }
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
