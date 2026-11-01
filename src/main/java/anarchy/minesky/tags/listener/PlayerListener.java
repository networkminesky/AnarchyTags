package anarchy.minesky.tags.listener;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.nametags.PlayerConfigPreferenceWatcher;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import anarchy.minesky.tags.util.FoliaTasks;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final AnarchyTags plugin;
    private final NametagManager nametagManager;
    private final PlayerConfigPreferenceWatcher preferenceWatcher;

    public PlayerListener(AnarchyTags plugin) {
        this.plugin = plugin;
        this.nametagManager = plugin.getNametagManager();
        this.preferenceWatcher = plugin.getPreferenceWatcher();
    }

    @EventHandler
    public void onPlayerLoad(PlayerClientLoadedWorldEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        VoiceChatBridge.onPlayerJoin(player);
        nametagManager.scheduleJoin(player);
        preferenceWatcher.seed(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        nametagManager.clearViewerReady(event.getPlayer());
        if (plugin.config().getNametagConfig().isEnabled()) {
            nametagManager.remove(event.getPlayer());
        }
        NametagHandler.clearViewer(event.getPlayer());
        preferenceWatcher.forget(event.getPlayer());
        plugin.getNickHideManager().onQuit(event.getPlayer());
        VoiceChatBridge.clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        Nametag nametag = nametagManager.get(event.getEntity());
        if (nametag != null) {
            FoliaTasks.runOnEntity(plugin, event.getEntity(), nametag::hideForAll);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        Nametag nametag = nametagManager.get(event.getPlayer());
        if (nametag == null) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, event.getPlayer(), () -> {
            nametag.syncPositionForViewers();
            nametag.updateVisibility(8);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        Nametag nametag = nametagManager.get(event.getPlayer());
        if (nametag == null) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, event.getPlayer(), () -> {
            nametag.hideForAll();
            nametag.updateVisibilityForAll();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        Nametag nametag = nametagManager.get(event.getPlayer());
        if (nametag == null) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, event.getPlayer(), () -> {
            if (event.getNewGameMode() == GameMode.SPECTATOR) {
                nametag.hideForAll();
            } else if (event.getPlayer().getPreviousGameMode() == GameMode.SPECTATOR) {
                nametag.updateVisibilityForAll();
            }
        });
    }
}
