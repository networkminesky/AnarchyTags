package anarchy.minesky.tags.scoreboard;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.ScoreboardConfig;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.util.FoliaTasks;
import anarchy.minesky.tags.utils.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardManager {

    public static final String OBJECTIVE_ID = "atags_sb";
    public static final int MAX_LINES = 15;

    private static final String[] ENTRY_KEYS = buildEntryKeys();

    private final AnarchyTags plugin;
    private final Map<UUID, Integer> lastHash = new ConcurrentHashMap<>();
    private final Set<UUID> presentQueued = ConcurrentHashMap.newKeySet();

    public ScoreboardManager(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public static String entryKey(int slot) {
        return ENTRY_KEYS[slot];
    }

    public void refreshAll() {
        ScoreboardConfig config = plugin.config().getScoreboardConfig();
        if (!config.isEnabled()) {
            clearAll();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, player, () -> applyOnEntity(player));
    }

    /** Um CREATE por sessão. Um segundo CREATE do mesmo objective derruba o cliente. */
    public void present(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (ScoreboardPacketHandler.isActive(player)) {
            lastHash.remove(id);
            refresh(player);
            return;
        }
        if (!presentQueued.add(id)) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, player, () -> {
            presentQueued.remove(id);
            if (!player.isOnline() || ScoreboardPacketHandler.isActive(player)) {
                return;
            }
            applyOnEntity(player);
        });
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        presentQueued.remove(id);
        lastHash.remove(id);
        ScoreboardPacketHandler.discard(id);
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
        }
        lastHash.clear();
        ScoreboardPacketHandler.resetAll();
    }

    private void applyOnEntity(Player player) {
        ScoreboardConfig config = plugin.config().getScoreboardConfig();
        if (!config.isEnabled() || !TagsPlayerConfig.scoreboardEnabled(player, true)) {
            if (lastHash.remove(player.getUniqueId()) != null) {
                ScoreboardPacketHandler.clear(player);
            }
            return;
        }

        ResolvedLines resolved = buildVisibleLines(player, config);
        Component title = ComponentUtils.format(config.getTitle());
        int hash = resolved.contentHash() ^ config.getTitle().hashCode();

        Integer previous = lastHash.get(player.getUniqueId());
        if (previous != null && previous == hash) {
            return;
        }

        boolean sent = ScoreboardPacketHandler.apply(player, title, resolved.components(), hash);
        if (sent) {
            lastHash.put(player.getUniqueId(), hash);
        }
    }

    private ResolvedLines buildVisibleLines(Player viewer, ScoreboardConfig config) {
        List<String> raw = new ArrayList<>();
        List<Component> components = new ArrayList<>();
        for (String fixed : config.getFixedLines()) {
            String resolved = ScoreboardLineResolver.resolve(viewer, fixed);
            if (!resolved.isBlank()) {
                raw.add(resolved);
                components.add(ComponentUtils.format(resolved));
            }
        }
        for (ScoreboardConfig.ToggleLine toggle : config.getToggles()) {
            if (!TagsPlayerConfig.scoreboardToggleEnabled(viewer, toggle.id(), toggle.defaultEnabled())) {
                continue;
            }
            String resolved = ScoreboardLineResolver.resolve(viewer, toggle.line());
            if (!resolved.isBlank()) {
                raw.add(resolved);
                components.add(ComponentUtils.format(resolved));
            }
        }
        return new ResolvedLines(raw, components);
    }

    private static String[] buildEntryKeys() {
        String[] keys = new String[MAX_LINES];
        for (int i = 0; i < keys.length; i++) {
            // Apenas códigos de cor — sem texto visível (evita "tags" verde se a linha não for removida)
            keys[i] = "§" + Integer.toHexString(i % 16)
                    + "§" + Integer.toHexString((i + 5) % 16)
                    + "§" + Integer.toHexString((i + 11) % 16)
                    + "§r";
        }
        return keys;
    }

    private record ResolvedLines(List<String> raw, List<Component> components) {
        int contentHash() {
            return String.join("\n", raw).hashCode();
        }
    }
}
