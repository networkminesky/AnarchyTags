package anarchy.minesky.tags.command;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nick.NickHideMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EsconderNickCommand implements CommandExecutor, TabCompleter {

    private final AnarchyTags plugin;

    public EsconderNickCommand(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Somente jogadores.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("anarchytags.escondernick")) {
            player.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return true;
        }

        if (args.length >= 1) {
            NickHideMode mode = NickHideMode.fromId(args[0]);
            if (mode == NickHideMode.OFF && !isKnownModeArg(args[0])) {
                player.sendMessage(Component.text("Modo inválido. Use: normal, ofuscado, admin, invisivel", NamedTextColor.RED));
                return true;
            }
            if (mode == NickHideMode.FULL_HIDDEN && !player.hasPermission("anarchytags.escondernick.invisivel")) {
                player.sendMessage(Component.text("Modo invisível total só para admins.", NamedTextColor.RED));
                return true;
            }
            plugin.getNickHideManager().setMode(player, mode);
            player.sendMessage(describe(mode));
            return true;
        }

        NickHideMode next = plugin.getNickHideManager().cycleMode(player);
        player.sendMessage(describe(next));
        return true;
    }

    private static boolean isKnownModeArg(String raw) {
        if (raw == null) {
            return false;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "off", "normal", "nenhum", "obfuscated", "ofuscado", "k", "obf",
                    "admin", "admin_hint", "dica", "hint", "full", "invisivel", "hidden", "full_hidden" -> true;
            default -> false;
        };
    }

    private static Component describe(NickHideMode mode) {
        return switch (mode) {
            case OFF -> Component.text("Nick normal — skin restaurada.", NamedTextColor.YELLOW);
            case OBFUSCATED -> Component.text("Nick ofuscado (&k) — admins veem &8. Skin Steve.", NamedTextColor.GREEN);
            case ADMIN_HINT -> Component.text("Escondido — admins veem &8" + "seu nick.", NamedTextColor.GREEN);
            case FULL_HIDDEN -> Component.text("Totalmente invisível (sem tag).", NamedTextColor.GREEN);
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        if (!player.hasPermission("anarchytags.escondernick")) {
            return List.of();
        }
        List<String> modes = new ArrayList<>();
        modes.add("normal");
        modes.add("ofuscado");
        modes.add("admin");
        if (player.hasPermission("anarchytags.escondernick.invisivel")) {
            modes.add("invisivel");
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return modes.stream().filter(m -> m.startsWith(prefix)).toList();
    }
}
