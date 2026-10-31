package anarchy.minesky.tags.command;

import anarchy.minesky.tags.AnarchyTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

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
            sender.sendMessage(Component.text("Uso: /" + label + " reload", NamedTextColor.GRAY));
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

        sender.sendMessage(Component.text("Uso: /" + label + " reload", NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("anarchytags.admin")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
