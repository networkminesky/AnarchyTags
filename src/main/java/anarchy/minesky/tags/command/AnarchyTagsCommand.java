package anarchy.minesky.tags.command;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.debug.PacketMetrics;
import anarchy.minesky.tags.debug.TagsDebugReport;
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

public class AnarchyTagsCommand implements CommandExecutor, TabCompleter {

    private final AnarchyTags plugin;

    public AnarchyTagsCommand(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text(
                    "AnarchyTags v" + plugin.getPluginMeta().getVersion() + " — estilo DisplayTags (Folia 26.3)",
                    NamedTextColor.AQUA
            ));
            sender.sendMessage(Component.text("Uso: /" + label + " reload | debug [jogador|reset]", NamedTextColor.GRAY));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("anarchytags.admin")) {
                sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
                return true;
            }
            long start = System.currentTimeMillis();
            boolean ok = plugin.reloadPlugin();
            long ms = System.currentTimeMillis() - start;
            if (ok) {
                sender.sendMessage(Component.text("AnarchyTags recarregado (" + ms + "ms).", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Erro ao recarregar — veja o console.", NamedTextColor.RED));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("anarchytags.admin")) {
                sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                PacketMetrics.reset();
                sender.sendMessage(Component.text("Contadores de pacotes zerados.", NamedTextColor.GREEN));
                return true;
            }
            Player focus = null;
            if (args.length >= 2) {
                focus = Bukkit.getPlayer(args[1]);
                if (focus == null) {
                    sender.sendMessage(Component.text("Jogador não encontrado: " + args[1], NamedTextColor.RED));
                    return true;
                }
            }
            TagsDebugReport.send(plugin, sender, focus);
            return true;
        }

        sender.sendMessage(Component.text("Uso: /" + label + " reload | debug [jogador|reset]", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("anarchytags.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("reload");
            options.add("debug");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            List<String> options = new ArrayList<>();
            options.add("reset");
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        return List.of();
    }
}
