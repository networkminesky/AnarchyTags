package anarchy.minesky.tags.debug;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.hook.LuckPermsHook;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.sorting.TabOrder;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TagsDebugReport {

    private TagsDebugReport() {}

    public static void send(AnarchyTags plugin, CommandSender sender, Player focus) {
        NametagManager manager = plugin.getNametagManager();
        TabOrder tab = manager.tabOrder();

        int online = Bukkit.getOnlinePlayers().size();
        int nametags = manager.getAll().size();
        int viewerReady = countViewerReady(manager);
        int packetsReady = countPacketsReady(manager);

        sender.sendMessage(Component.text("—— AnarchyTags Debug ——", NamedTextColor.AQUA));
        sender.sendMessage(line("Online", online + " | nametags=" + nametags
                + " | viewerReady=" + viewerReady + " | packetsReady=" + packetsReady));

        sender.sendMessage(Component.text("Pacotes (desde último reset ou boot):", NamedTextColor.GOLD));
        sender.sendMessage(line("Total", String.valueOf(PacketMetrics.totalPackets())));
        sender.sendMessage(line("Teams", PacketMetrics.teamPackets()
                + " (enviados=" + PacketMetrics.teamEnsureSentCount()
                + ", ignorados=" + PacketMetrics.teamEnsureSkippedCount() + ")"));
        sender.sendMessage(line("Display", "spawn=" + PacketMetrics.displaySpawn()
                + " meta=" + PacketMetrics.displayMetadata()
                + " destroy=" + PacketMetrics.displayDestroy()
                + " mount=" + PacketMetrics.displayMount()
                + " tp=" + PacketMetrics.displayTeleport()));

        sender.sendMessage(line("Voice chat", VoiceChatBridge.debugLine(focus != null ? focus : (sender instanceof Player p ? p : null))));

        sender.sendMessage(Component.text("Tab / sorting:", NamedTextColor.GOLD));
        sender.sendMessage(line("Fila pending", String.valueOf(tab.pendingSize())));
        sender.sendMessage(line("Full sync", tab.fullSyncCount() + " | cache times=" + tab.sortCacheSize()));
        sender.sendMessage(line("Times ativos", NametagHandler.trackedAssignmentCount()
                + " (pares viewer→alvo)"));

        int activeDisplays = 0;
        int activeViewers = 0;
        for (Nametag tag : manager.getAll()) {
            activeViewers += tag.getViewerCount();
            activeDisplays += tag.getSpawnedViewerCount();
        }
        sender.sendMessage(line("Tags 3D agora", "viewers=" + activeViewers + " | displays spawnados=" + activeDisplays));

        if (focus == null && sender instanceof Player p) {
            focus = p;
        }

        if (focus != null) {
            sendPlayerSection(plugin, sender, focus, manager, tab);
        } else {
            sender.sendMessage(Component.text("Jogadores (resumo):", NamedTextColor.GOLD));
            for (Player player : Bukkit.getOnlinePlayers()) {
                Nametag tag = manager.get(player);
                String sort = tab.sortTeamFor(player.getUniqueId());
                int weight = LuckPermsHook.effectiveSortWeight(player.getUniqueId());
                int viewers = tag == null ? 0 : tag.getViewerCount();
                sender.sendMessage(Component.text(
                        " • " + player.getName()
                                + " | peso=" + weight
                                + " | viewers=" + viewers
                                + " | time=" + abbreviate(sort, 28),
                        NamedTextColor.GRAY
                ));
            }
        }

        sender.sendMessage(Component.text("Use /atags debug reset para zerar contadores.", NamedTextColor.DARK_GRAY));
    }

    private static void sendPlayerSection(
            AnarchyTags plugin,
            CommandSender sender,
            Player focus,
            NametagManager manager,
            TabOrder tab
    ) {
        UUID id = focus.getUniqueId();
        sender.sendMessage(Component.text("Jogador: " + focus.getName(), NamedTextColor.YELLOW));

        sender.sendMessage(line("Estado",
                "viewerReady=" + manager.isViewerReady(focus)
                        + " | packetsReady=" + manager.isPacketsReady(focus)
                        + " | esconderNick=" + plugin.getNickHideManager().getMode(focus).configId()));

        long waitMs = manager.millisUntilPacketsReady(focus);
        if (waitMs > 0) {
            sender.sendMessage(line("Pacotes em", waitMs + "ms"));
        }

        sender.sendMessage(line("LuckPerms",
                "peso=" + LuckPermsHook.effectiveSortWeight(id)
                        + " | primary=" + LuckPermsHook.primaryGroup(id)));

        String sortTeam = tab.sortTeamFor(id);
        sender.sendMessage(line("Tab team", sortTeam == null ? "(ainda não calculado)" : sortTeam));

        sender.sendMessage(line("Voice", VoiceChatBridge.debugLine(focus)));

        Nametag own = manager.get(focus);
        if (own == null) {
            sender.sendMessage(line("Nametag", "não registrada"));
        } else {
            sender.sendMessage(line("Sua tag 3D",
                    "viewers=" + own.getViewerCount()
                            + " | spawned=" + own.getSpawnedViewerCount()
                            + " | entityId=" + own.getDisplayEntityId()));
        }

        int seeing = NametagHandler.assignmentsForViewer(id);
        int shownTo = NametagHandler.assignmentsForTarget(id);
        sender.sendMessage(line("Scoreboard", "vê times de " + seeing + " jogadores | outros veem seu time: " + shownTo));

        sender.sendMessage(Component.text("Tags que você vê (spawn ativo):", NamedTextColor.GOLD));
        List<String> visible = new ArrayList<>();
        for (Nametag tag : manager.getAll()) {
            Player owner = tag.getPlayer();
            if (owner == null || owner.getUniqueId().equals(id)) {
                continue;
            }
            if (tag.isSpawnedForViewer(focus)) {
                visible.add(owner.getName());
            }
        }
        if (visible.isEmpty()) {
            sender.sendMessage(Component.text(" (nenhuma)", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text(" " + String.join(", ", visible), NamedTextColor.WHITE));
        }
    }

    private static int countViewerReady(NametagManager manager) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (manager.isViewerReady(player)) {
                count++;
            }
        }
        return count;
    }

    private static int countPacketsReady(NametagManager manager) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (manager.isPacketsReady(player)) {
                count++;
            }
        }
        return count;
    }

    private static Component line(String label, String value) {
        return Component.text(" " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "-";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }
}
