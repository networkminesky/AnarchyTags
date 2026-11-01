package anarchy.minesky.tags.listener;

import anarchy.minesky.api.vanish.events.PlayerVanishStateChangeEvent;
import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class VanishListener implements Listener {

    private final AnarchyTags plugin;
    private final NametagManager nametagManager;

    public VanishListener(AnarchyTags plugin) {
        this.plugin = plugin;
        this.nametagManager = plugin.getNametagManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanishStateChange(PlayerVanishStateChangeEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }

        Player player = Bukkit.getPlayer(event.getUUID());
        if (player == null || !player.isOnline()) {
            return;
        }

        player.getScheduler().run(plugin, task -> {
            Nametag nametag = nametagManager.get(player);
            if (nametag != null) {
                nametag.updateVisibility(48);
            }
        }, null);
    }
}
