package anarchy.minesky.tags.command;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nick.NickHideManager;
import anarchy.minesky.tags.nick.NickHideMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

        if (args.length == 0) {
            if (!player.hasPermission(NickHideManager.PERM_OBFUSCATED)) {
                player.sendMessage(Component.text("Sem permissão para ofuscar nick.", NamedTextColor.RED));
                return true;
            }
            if (plugin.getNickHideManager().getMode(player) == NickHideMode.FULL_HIDDEN) {
                player.sendMessage(Component.text("Desative o modo invisível antes de ofuscar.", NamedTextColor.RED));
                return true;
            }
            plugin.getNickHideManager().toggleObfuscated(player);
            boolean on = plugin.getNickHideManager().getMode(player) == NickHideMode.OBFUSCATED;
            player.sendMessage(on
                    ? Component.text("Nick ofuscado ativado (skin Steve, prefixo/economia/clãs mantidos).", NamedTextColor.GREEN)
                    : Component.text("Nick ofuscado desativado — skin restaurada.", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("habilitar")) {
            boolean enabled = plugin.getNickHideManager().toggleNickLine(player);
            player.sendMessage(enabled
                    ? Component.text("Linha do nick na tag habilitada.", NamedTextColor.GREEN)
                    : Component.text("Linha do nick na tag desabilitada (prefixo, economia e clãs permanecem).", NamedTextColor.YELLOW));
            return true;
        }

        if (sub.equals("invisivel") || sub.equals("invisible")) {
            Player target = player;
            if (args.length >= 2) {
                if (!player.hasPermission(NickHideManager.PERM_INVISIBLE_OTHERS)) {
                    player.sendMessage(Component.text("Sem permissão para alterar tags de outros jogadores.", NamedTextColor.RED));
                    return true;
                }
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(Component.text("Jogador não encontrado.", NamedTextColor.RED));
                    return true;
                }
            } else if (!player.hasPermission(NickHideManager.PERM_INVISIBLE)) {
                player.sendMessage(Component.text("Sem permissão para modo invisível.", NamedTextColor.RED));
                return true;
            }

            if (!plugin.getNickHideManager().toggleInvisible(player, target)) {
                player.sendMessage(Component.text("Não foi possível alterar o modo invisível.", NamedTextColor.RED));
                return true;
            }
            boolean hidden = plugin.getNickHideManager().getMode(target) == NickHideMode.FULL_HIDDEN;
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(hidden
                        ? Component.text("Sua tag foi ocultada.", NamedTextColor.GREEN)
                        : Component.text("Sua tag voltou ao normal.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(hidden
                        ? Component.text("Tag de " + target.getName() + " ocultada.", NamedTextColor.GREEN)
                        : Component.text("Tag de " + target.getName() + " restaurada.", NamedTextColor.YELLOW));
            }
            return true;
        }

        player.sendMessage(Component.text("Uso: /escondernick | habilitar | invisivel [jogador]", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("habilitar");
            if (sender.hasPermission(NickHideManager.PERM_INVISIBLE)
                    || sender.hasPermission(NickHideManager.PERM_INVISIBLE_OTHERS)) {
                options.add("invisivel");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(o -> o.startsWith(prefix)).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invisivel") || args[0].equalsIgnoreCase("invisible"))) {
            if (!sender.hasPermission(NickHideManager.PERM_INVISIBLE_OTHERS)) {
                return List.of();
            }
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
