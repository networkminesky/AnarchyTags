package anarchy.minesky.tags.utils.handlers;

import anarchy.minesky.tags.debug.PacketMetrics;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NametagHandler {

    private record Sent(String team, int hash) {}

    /** viewer -> target -> último time enviado */
    private static final Map<UUID, Map<UUID, Sent>> byViewer = new ConcurrentHashMap<>();
    /** target -> viewer -> último time enviado */
    private static final Map<UUID, Map<UUID, Sent>> byTarget = new ConcurrentHashMap<>();

    private NametagHandler() {}

    public static boolean matches(UUID viewerId, UUID targetId, String team, int hash) {
        Map<UUID, Sent> teams = byViewer.get(viewerId);
        if (teams == null) {
            return false;
        }
        Sent sent = teams.get(targetId);
        return sent != null && sent.team.equals(team) && sent.hash == hash;
    }

    public static boolean ensure(Player target, Player viewer, String team, Component prefix, Component suffix, int hash) {
        if (target == null || viewer == null || !viewer.isOnline() || team == null || team.isBlank()) {
            return false;
        }

        UUID viewerId = viewer.getUniqueId();
        UUID targetId = target.getUniqueId();
        Map<UUID, Sent> viewerTeams = byViewer.computeIfAbsent(viewerId, id -> new ConcurrentHashMap<>());
        Sent current = viewerTeams.get(targetId);
        if (current != null && current.team.equals(team) && current.hash == hash) {
            PacketMetrics.recordTeamEnsureSkipped();
            return false;
        }

        Component safePrefix = prefix == null ? Component.empty() : prefix;
        Component safeSuffix = suffix == null ? Component.empty() : suffix;
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                safePrefix,
                safeSuffix,
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.ALWAYS,
                null,
                WrapperPlayServerTeams.OptionData.NONE
        );

        if (current != null && !current.team.equals(team)) {
            send(new WrapperPlayServerTeams(
                    current.team,
                    WrapperPlayServerTeams.TeamMode.REMOVE,
                    (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                    target.getName()
            ), viewer);
        }
        if (current == null || !current.team.equals(team)) {
            send(new WrapperPlayServerTeams(
                    team,
                    WrapperPlayServerTeams.TeamMode.CREATE,
                    info,
                    target.getName()
            ), viewer);
        } else {
            send(new WrapperPlayServerTeams(team, WrapperPlayServerTeams.TeamMode.UPDATE, info), viewer);
        }

        Sent sent = new Sent(team, hash);
        viewerTeams.put(targetId, sent);
        byTarget.computeIfAbsent(targetId, id -> new ConcurrentHashMap<>()).put(viewerId, sent);
        PacketMetrics.recordTeamEnsureSent();
        return true;
    }

    public static int trackedAssignmentCount() {
        int total = 0;
        for (Map<UUID, Sent> map : byViewer.values()) {
            total += map.size();
        }
        return total;
    }

    public static int assignmentsForViewer(UUID viewerId) {
        Map<UUID, Sent> map = byViewer.get(viewerId);
        return map == null ? 0 : map.size();
    }

    public static int assignmentsForTarget(UUID targetId) {
        Map<UUID, Sent> map = byTarget.get(targetId);
        return map == null ? 0 : map.size();
    }

    public static void clearTarget(Player target) {
        Map<UUID, Sent> viewers = byTarget.remove(target.getUniqueId());
        if (viewers == null || viewers.isEmpty()) {
            return;
        }
        String name = target.getName();
        UUID targetId = target.getUniqueId();
        for (Map.Entry<UUID, Sent> entry : viewers.entrySet()) {
            Map<UUID, Sent> viewerTeams = byViewer.get(entry.getKey());
            if (viewerTeams != null) {
                viewerTeams.remove(targetId);
            }
            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            send(new WrapperPlayServerTeams(
                    entry.getValue().team,
                    WrapperPlayServerTeams.TeamMode.REMOVE,
                    (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                    name
            ), viewer);
        }
    }

    public static void clearViewer(Player viewer) {
        Map<UUID, Sent> teams = byViewer.remove(viewer.getUniqueId());
        if (teams == null) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        for (UUID targetId : teams.keySet()) {
            Map<UUID, Sent> viewers = byTarget.get(targetId);
            if (viewers != null) {
                viewers.remove(viewerId);
            }
        }
    }

    public static void reset() {
        byViewer.clear();
        byTarget.clear();
    }

    private static void send(PacketWrapper<?> packet, Player viewer) {
        PacketMetrics.recordTeamPacket();
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
