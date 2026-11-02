package anarchy.minesky.tags.scoreboard;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.ScoreboardConfig;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.util.FoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardUpdateTask {

    private record PrefSnapshot(boolean enabled, Map<String, Boolean> toggles) {}

    private final AnarchyTags plugin;
    private final ScoreboardManager scoreboardManager;
    private final Map<UUID, PrefSnapshot> lastPrefs = new ConcurrentHashMap<>();
    private ScheduledTask task;
    private ScheduledTask prefTask;

    public ScoreboardUpdateTask(AnarchyTags plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    public void start() {
        ScoreboardConfig config = plugin.config().getScoreboardConfig();
        if (!config.isEnabled()) {
            return;
        }
        long period = Math.max(20L, config.getUpdateIntervalSeconds() * 20L);
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tick(), period, period);
        prefTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tickPreferences(), 40L, 40L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (prefTask != null) {
            prefTask.cancel();
            prefTask = null;
        }
        lastPrefs.clear();
    }

    public void seed(Player player) {
        if (player == null) {
            return;
        }
        lastPrefs.put(player.getUniqueId(), snapshot(player));
        // Depois do burst de tags/times, um único CREATE. Repetir esse pacote gera "Network protocol error".
        FoliaTasks.runOnEntityDelayed(plugin, player, () -> {
            if (player.isOnline()) {
                scoreboardManager.present(player);
            }
        }, 20L);
    }

    public void forget(Player player) {
        if (player != null) {
            lastPrefs.remove(player.getUniqueId());
        }
    }

    private void tick() {
        ScoreboardConfig config = plugin.config().getScoreboardConfig();
        if (!config.isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            lastPrefs.put(player.getUniqueId(), snapshot(player));
            scoreboardManager.refresh(player);
        }
        lastPrefs.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private void tickPreferences() {
        ScoreboardConfig config = plugin.config().getScoreboardConfig();
        if (!config.isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PrefSnapshot current = snapshot(player);
            PrefSnapshot previous = lastPrefs.put(player.getUniqueId(), current);
            if (previous != null && !previous.equals(current)) {
                scoreboardManager.refresh(player);
            }
        }
        lastPrefs.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    private PrefSnapshot snapshot(Player player) {
        Map<String, Boolean> toggles = new ConcurrentHashMap<>();
        for (ScoreboardConfig.ToggleLine toggle : plugin.config().getScoreboardConfig().getToggles()) {
            toggles.put(toggle.id(), TagsPlayerConfig.scoreboardToggleEnabled(player, toggle.id(), toggle.defaultEnabled()));
        }
        return new PrefSnapshot(TagsPlayerConfig.scoreboardEnabled(player, true), toggles);
    }
}
