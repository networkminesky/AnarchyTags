package anarchy.minesky.tags.listener;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.nametags.PlayerConfigPreferenceWatcher;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
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

    /**
     * Disparado quando o jogador começa a ver outro jogador (após RTP, chunk load, etc.)
     * Essencial para o Folia não perder pacotes de entidades montadas.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrackEntity(PlayerTrackEntityEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }

        Nametag tag = nametagManager.get(target);
        if (tag == null) {
            return;
        }

        // Executa na thread da região do TARGET com delay de 2 ticks
        // para dar tempo do cliente do viewer criar a entidade do player (veículo)
        FoliaTasks.runOnEntityDelayed(plugin, target, () -> {
            if (!target.isOnline() || !viewer.isOnline()) {
                return;
            }
            if (!target.getWorld().equals(viewer.getWorld())) {
                return;
            }
            long maxDistSq = (long) tag.getVisibilityDistance() * tag.getVisibilityDistance();
            if (!tag.canShowTo(viewer, maxDistSq)) {
                return;
            }
            // Força reset para garantir que monte limpo
            tag.hide(viewer);
            tag.show(viewer);
        }, 2L);
    }

    /**
     * Disparado quando o jogador deixa de ver o outro (descarregou chunk / teleporte).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUntrackEntity(PlayerUntrackEntityEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        Player viewer = event.getPlayer();
        Nametag tag = nametagManager.get(target);
        if (tag != null) {
            tag.hide(viewer);
        }
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
        nametagManager.afterPlayerTeleported(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.config().getNametagConfig().isEnabled()) {
            return;
        }
        nametagManager.afterPlayerTeleported(event.getPlayer());
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