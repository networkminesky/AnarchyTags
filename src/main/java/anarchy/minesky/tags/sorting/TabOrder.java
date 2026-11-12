package anarchy.minesky.tags.sorting;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.hook.LuckPermsHook;
import anarchy.minesky.tags.nick.NickHideMode;
import anarchy.minesky.tags.util.FoliaTasks;
import anarchy.minesky.tags.debug.PacketMetrics;
import anarchy.minesky.tags.utils.ComponentUtils;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import anarchy.minesky.tags.utils.helpers.PlaceholderHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Ordem do tab via scoreboard teams. Nomes de time limitados a 16 caracteres (protocolo Minecraft).
 */
public final class TabOrder {

    private static final int TEAM_MAX = 16;
    private static final int WEIGHT_WIDTH = 7;
    private static final int NAME_WIDTH = 5;
    private static final int UUID_SUFFIX = 4;

    private final AnarchyTags plugin;
    private final ConcurrentHashMap<UUID, String> sortTeams = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Pair> pending = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<UUID> changedTargets = new ConcurrentLinkedQueue<>();

    public TabOrder(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void reset() {
        sortTeams.clear();
        pending.clear();
        changedTargets.clear();
    }

    public void forget(UUID playerId) {
        sortTeams.remove(playerId);
    }

    public void precomputeAsync(UUID uuid, String playerName) {
        if (uuid == null) {
            return;
        }
        FoliaTasks.runAsync(plugin, () -> {
            String next = computeSortTeam(uuid, playerName);
            String previous = sortTeams.put(uuid, next);
            if (previous != null && !previous.equals(next)) {
                changedTargets.add(uuid);
            }
        });
    }

    public void recompute(Player player) {
        if (player != null) {
            precomputeAsync(player.getUniqueId(), player.getName());
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!plugin.getNametagManager().isPacketsReady(viewer)) {
                    continue;
                }
                pending.add(new Pair(viewer.getUniqueId(), player.getUniqueId()));
            }
        }
    }

    public int pendingSize() {
        return pending.size();
    }

    public int fullSyncCount() {
        return 0;
    }

    public int sortCacheSize() {
        return sortTeams.size();
    }

    public String sortTeamFor(UUID playerId) {
        return sortTeams.get(playerId);
    }

    public void beginFullSync(Player viewer) {
        if (viewer == null || !plugin.config().getNametagConfig().isSortingEnabled()) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        for (Player target : Bukkit.getOnlinePlayers()) {
            pending.add(new Pair(viewerId, target.getUniqueId()));
        }
    }

    public void onPlayerJoinedTab(Player target) {
        if (target == null || !plugin.config().getNametagConfig().isSortingEnabled()) {
            return;
        }
        precomputeAsync(target.getUniqueId(), target.getName());
        UUID targetId = target.getUniqueId();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getNametagManager().isViewerReady(viewer)) {
                continue;
            }
            if (!plugin.getNametagManager().isPacketsReady(viewer)) {
                continue;
            }
            pending.add(new Pair(viewer.getUniqueId(), targetId));
        }
        if (plugin.getNametagManager().isPacketsReady(target)) {
            pending.add(new Pair(targetId, targetId));
        }
    }

    public void recomputeChangedAsync() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            return;
        }
        FoliaTasks.runAsync(plugin, () -> {
            for (Player player : online) {
                UUID id = player.getUniqueId();
                String next = computeSortTeam(id, player.getName());
                String previous = sortTeams.put(id, next);
                if (previous != null && !previous.equals(next)) {
                    changedTargets.add(id);
                }
            }
        });
    }

    public int tick(int budget) {
        if (budget <= 0 || !plugin.config().getNametagConfig().isSortingEnabled()) {
            return 0;
        }

        UUID changed;
        while ((changed = changedTargets.poll()) != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!plugin.getNametagManager().isViewerReady(viewer)) {
                    continue;
                }
                if (!plugin.getNametagManager().isPacketsReady(viewer)) {
                    continue;
                }
                pending.add(new Pair(viewer.getUniqueId(), changed));
            }
        }

        int sent = 0;
        while (sent < budget) {
            Pair pair = pending.poll();
            if (pair == null) {
                break;
            }
            if (applyPair(pair)) {
                sent++;
            }
        }
        return sent;
    }

    private boolean applyPair(Pair pair) {
        if (!plugin.getNametagManager().isPacketsReady(pair.viewer)) {
            return false;
        }
        Player viewer = Bukkit.getPlayer(pair.viewer);
        Player target = Bukkit.getPlayer(pair.target);
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }
        return apply(viewer, target);
    }

    private boolean apply(Player viewer, Player target) {
        UUID targetId = target.getUniqueId();
        String team = sortTeams.get(targetId);
        if (team == null) {
            team = computeSortTeam(targetId, target.getName());
            sortTeams.put(targetId, team);
            precomputeAsync(targetId, target.getName());
        }

        NametagConfig config = plugin.config().getNametagConfig();
        Component prefix = Component.empty();
        Component suffix = Component.empty();
        boolean obfuscated = plugin.getNickHideManager().getMode(target) == NickHideMode.OBFUSCATED;
        if (config.isSortingApplyPrefix() && !obfuscated) {
            prefix = ComponentUtils.format(PlaceholderHelper.apply(viewer, target, config.getSortingPrefix()));
            suffix = ComponentUtils.format(PlaceholderHelper.apply(viewer, target, config.getSortingSuffix()));
        }

        int hash = Objects.hash(team, prefix, suffix);
        if (NametagHandler.matches(viewer.getUniqueId(), targetId, team, hash)) {
            PacketMetrics.recordTeamEnsureSkipped();
            return false;
        }

        return NametagHandler.ensure(target, viewer, team, prefix, suffix, hash);
    }

    private String computeSortTeam(UUID uuid, String playerName) {
        NametagConfig config = plugin.config().getNametagConfig();
        if (!config.isSortingEnabled()) {
            return uniqueSuffix(uuid, TEAM_MAX);
        }

        StringBuilder team = new StringBuilder(TEAM_MAX);
        for (String type : config.getSortingTypes()) {
            appendRule(uuid, playerName, type, team);
        }
        while (team.length() < WEIGHT_WIDTH + NAME_WIDTH) {
            team.append('-');
        }
        String suffix = uniqueSuffix(uuid, UUID_SUFFIX);
        if (team.length() + suffix.length() > TEAM_MAX) {
            team.setLength(TEAM_MAX - suffix.length());
        }
        team.append(suffix);
        return team.toString();
    }

    private static String uniqueSuffix(UUID uuid, int length) {
        String id = uuid.toString().replace("-", "");
        return id.substring(0, Math.min(length, id.length()));
    }

    private void appendRule(UUID uuid, String playerName, String raw, StringBuilder team) {
        if (raw == null || raw.isBlank() || team.length() >= WEIGHT_WIDTH + NAME_WIDTH) {
            return;
        }
        int colon = raw.indexOf(':');
        String kind = (colon < 0 ? raw : raw.substring(0, colon)).trim().toUpperCase(Locale.ROOT);
        String arg = colon < 0 ? "" : raw.substring(colon + 1).trim();
        switch (kind) {
            case "GROUPS" -> {
                if (team.length() < 2) {
                    team.append(encodeGroups(uuid, arg));
                }
            }
            case "PLACEHOLDER_HIGH_TO_LOW" -> appendWeight(uuid, team, true);
            case "PLACEHOLDER_LOW_TO_HIGH" -> appendWeight(uuid, team, false);
            case "PLACEHOLDER_A_TO_Z" -> appendName(playerName, team, true);
            case "PLACEHOLDER_Z_TO_A" -> appendName(playerName, team, false);
            default -> {
            }
        }
    }

    private void appendWeight(UUID uuid, StringBuilder team, boolean highFirst) {
        if (team.length() >= WEIGHT_WIDTH) {
            return;
        }
        String encoded = encodeNumber(uuid, highFirst);
        int space = WEIGHT_WIDTH - team.length();
        team.append(encoded, 0, Math.min(space, encoded.length()));
    }

    private void appendName(String playerName, StringBuilder team, boolean aToZ) {
        int room = WEIGHT_WIDTH + NAME_WIDTH - team.length();
        if (room <= 0) {
            return;
        }
        String encoded = fixedText(playerName, Math.min(NAME_WIDTH, room), aToZ);
        team.append(encoded);
    }

    private String encodeGroups(UUID uuid, String arg) {
        String primary = LuckPermsHook.primaryGroup(uuid).toLowerCase(Locale.ROOT);
        String[] groups = arg.split(",");
        int index = groups.length;
        if (!primary.isEmpty()) {
            for (int i = 0; i < groups.length; i++) {
                if (groups[i].trim().equalsIgnoreCase(primary)) {
                    index = i;
                    break;
                }
            }
        }
        return String.format("%02d", Math.min(index, 99));
    }

    private String encodeNumber(UUID uuid, boolean highFirst) {
        long value = LuckPermsHook.effectiveSortWeight(uuid);
        long shifted = Math.clamp(value + 50_000L, 0L, 9_999_999L);
        if (highFirst) {
            shifted = 9_999_999L - shifted;
        }
        return String.format("%0" + WEIGHT_WIDTH + "d", shifted);
    }

    private static String fixedText(String raw, int width, boolean aToZ) {
        StringBuilder builder = new StringBuilder(width);
        String lower = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length() && builder.length() < width; i++) {
            char c = lower.charAt(i);
            if (c >= 'a' && c <= 'z') {
                builder.append(aToZ ? c : (char) ('z' - (c - 'a')));
            } else if (c >= '0' && c <= '9') {
                builder.append(aToZ ? c : (char) ('9' - (c - '0')));
            }
        }
        while (builder.length() < width) {
            builder.append('-');
        }
        return builder.toString();
    }

    private record Pair(UUID viewer, UUID target) {}
}
