package anarchy.minesky.tags.utils.handlers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NametagHandler {

    /** viewer -> targets cujo nametag vanilla já foi escondido */
    private static final Map<UUID, Map<Integer, Boolean>> hiddenVanilla = new ConcurrentHashMap<>();

    private NametagHandler() {}

    public static void hide(Player target, Player viewer) {
        int entityId = target.getEntityId();
        hiddenVanilla
                .computeIfAbsent(viewer.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(entityId, Boolean.TRUE);

        String name = teamName(target);
        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(),
                Component.empty(),
                Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.ALWAYS,
                null,
                WrapperPlayServerTeams.OptionData.NONE
        );

        WrapperPlayServerTeams.TeamMode mode = WrapperPlayServerTeams.TeamMode.CREATE;
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                name,
                mode,
                teamInfo,
                target.getName()
        );
        sendPacket(packet, viewer);

        WrapperPlayServerTeams update = new WrapperPlayServerTeams(
                name,
                WrapperPlayServerTeams.TeamMode.UPDATE,
                teamInfo
        );
        sendPacket(update, viewer);
    }

    public static void show(Player target, Player viewer) {
        Map<Integer, Boolean> map = hiddenVanilla.get(viewer.getUniqueId());
        if (map != null) {
            map.remove(target.getEntityId());
        }

        String name = teamName(target);
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
                name,
                WrapperPlayServerTeams.TeamMode.REMOVE,
                (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                target.getName()
        );
        sendPacket(packet, viewer);
    }

    public static void clearViewer(Player viewer) {
        hiddenVanilla.remove(viewer.getUniqueId());
    }

    private static String teamName(Player target) {
        return "anarchytags_" + target.getEntityId();
    }

    private static void sendPacket(PacketWrapper<?> packet, Player viewer) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
