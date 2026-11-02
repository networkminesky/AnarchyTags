package anarchy.minesky.tags.scoreboard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sidebar via PacketEvents — Bukkit Scoreboard API não funciona no Folia. */
public final class ScoreboardPacketHandler {

    /** 1 = sidebar no protocolo Minecraft. */
    private static final int SIDEBAR_SLOT = 1;
    private static final ScoreFormat BLANK_NUMBERS = ScoreFormat.blankScore();

    private record SentState(int contentHash, int titleHash, int lineCount, boolean active) {}

    private static final Map<UUID, SentState> byViewer = new ConcurrentHashMap<>();

    private ScoreboardPacketHandler() {}

    public static void clear(Player viewer) {
        if (viewer == null) {
            return;
        }
        SentState state = byViewer.remove(viewer.getUniqueId());
        if (state == null || !state.active()) {
            return;
        }
        for (int slot = 0; slot < state.lineCount(); slot++) {
            removeEntry(viewer, ScoreboardManager.entryKey(slot));
        }
        send(new WrapperPlayServerScoreboardObjective(
                ScoreboardManager.OBJECTIVE_ID,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
                Component.empty(),
                null
        ), viewer);
        send(new WrapperPlayServerDisplayScoreboard(SIDEBAR_SLOT, ""), viewer);
    }

    public static boolean isActive(Player viewer) {
        if (viewer == null) {
            return false;
        }
        SentState state = byViewer.get(viewer.getUniqueId());
        return state != null && state.active();
    }

    /** Descarta o cache desta sessão. O cliente novo não tem a sidebar. */
    public static void discard(UUID viewerId) {
        if (viewerId != null) {
            byViewer.remove(viewerId);
        }
    }

    public static void resetAll() {
        byViewer.clear();
    }

    /**
     * @return true se enviou pacotes (conteúdo ou estado mudou)
     */
    public static boolean apply(Player viewer, Component title, List<Component> lines, int contentHash) {
        if (viewer == null || !viewer.isOnline()) {
            return false;
        }

        UUID id = viewer.getUniqueId();
        SentState previous = byViewer.get(id);
        if (previous != null && previous.active() && previous.contentHash() == contentHash) {
            return false;
        }

        int titleHash = title.hashCode();
        int count = Math.min(lines.size(), ScoreboardManager.MAX_LINES);
        boolean updating = previous != null && previous.active();

        if (!updating) {
            send(new WrapperPlayServerScoreboardObjective(
                    ScoreboardManager.OBJECTIVE_ID,
                    WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
                    title,
                    WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    BLANK_NUMBERS
            ), viewer);
            send(new WrapperPlayServerDisplayScoreboard(SIDEBAR_SLOT, ScoreboardManager.OBJECTIVE_ID), viewer);
        } else if (previous.titleHash() != titleHash) {
            send(new WrapperPlayServerScoreboardObjective(
                    ScoreboardManager.OBJECTIVE_ID,
                    WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
                    title,
                    WrapperPlayServerScoreboardObjective.RenderType.INTEGER,
                    BLANK_NUMBERS
            ), viewer);
        }

        for (int slot = 0; slot < count; slot++) {
            send(new WrapperPlayServerUpdateScore(
                    ScoreboardManager.entryKey(slot),
                    WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                    ScoreboardManager.OBJECTIVE_ID,
                    ScoreboardManager.MAX_LINES - slot,
                    lines.get(slot),
                    BLANK_NUMBERS
            ), viewer);
        }

        if (updating && previous.lineCount() > count) {
            for (int slot = count; slot < previous.lineCount(); slot++) {
                removeEntry(viewer, ScoreboardManager.entryKey(slot));
            }
        }

        byViewer.put(id, new SentState(contentHash, titleHash, count, true));
        return true;
    }

    private static void removeEntry(Player viewer, String entry) {
        // 1.20.3+ ignora Action.REMOVE_ITEM em UPDATE_SCORE; a remoção é RESET_SCORE.
        send(new WrapperPlayServerResetScore(entry, ScoreboardManager.OBJECTIVE_ID), viewer);
    }

    private static void send(PacketWrapper<?> packet, Player viewer) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
