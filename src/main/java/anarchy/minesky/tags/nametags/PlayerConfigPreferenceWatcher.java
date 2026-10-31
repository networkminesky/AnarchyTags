package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detecta mudanças nas opções do /config (fundo/sombra) e atualiza as tags visíveis sem reload.
 */
public final class PlayerConfigPreferenceWatcher {

    private record Snapshot(boolean background, boolean shadow) {}

    private final AnarchyTags plugin;
    private final NametagManager nametagManager;
    private final Map<UUID, Snapshot> lastSnapshot = new ConcurrentHashMap<>();
    private ScheduledTask task;

    public PlayerConfigPreferenceWatcher(AnarchyTags plugin, NametagManager nametagManager) {
        this.plugin = plugin;
        this.nametagManager = nametagManager;
    }

    public void start() {
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> {
            NametagConfig config = plugin.config().getNametagConfig();
            boolean serverBackground = !"transparent".equalsIgnoreCase(config.getBackground());

            for (Player player : Bukkit.getOnlinePlayers()) {
                Snapshot current = new Snapshot(
                        TagsPlayerConfig.nametagBackgroundEnabled(player, serverBackground),
                        TagsPlayerConfig.nametagTextShadowEnabled(player, config.hasTextShadow())
                );

                Snapshot previous = lastSnapshot.put(player.getUniqueId(), current);
                if (previous != null && !previous.equals(current)) {
                    nametagManager.refreshViewFor(player);
                }
            }

            lastSnapshot.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }, 5L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastSnapshot.clear();
    }

    public void seed(Player player) {
        if (player == null) {
            return;
        }
        NametagConfig config = plugin.config().getNametagConfig();
        boolean serverBackground = !"transparent".equalsIgnoreCase(config.getBackground());
        lastSnapshot.put(player.getUniqueId(), new Snapshot(
                TagsPlayerConfig.nametagBackgroundEnabled(player, serverBackground),
                TagsPlayerConfig.nametagTextShadowEnabled(player, config.hasTextShadow())
        ));
    }

    public void forget(Player player) {
        if (player != null) {
            lastSnapshot.remove(player.getUniqueId());
        }
    }
}
