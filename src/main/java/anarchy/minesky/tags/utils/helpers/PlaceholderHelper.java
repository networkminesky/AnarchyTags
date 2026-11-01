package anarchy.minesky.tags.utils.helpers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderHelper {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%]+)%");

    private PlaceholderHelper() {}

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static String applyTarget(Player target, String text) {
        if (text == null || text.isBlank() || !isAvailable()) {
            return text == null ? "" : text;
        }
        return PlaceholderAPI.setPlaceholders(target, text);
    }

    /**
     * Expansions {@link Relational} do Anarchy (economy/clãs) exigem sintaxe {@code %rel_<id>_<params>%}
     * no PAPI 2.12+. Aceita também {@code %anarchyeconomy_formatted_tab%} na config e converte automaticamente.
     */
    public static String apply(Player viewer, Player target, String text) {
        if (text == null || text.isBlank() || !isAvailable()) {
            return text == null ? "" : text;
        }

        String relational = PlaceholderAPI.setRelationalPlaceholders(
                viewer,
                target,
                toRelationalSyntax(text)
        );
        return PlaceholderAPI.setPlaceholders(target, relational);
    }

    static String toRelationalSyntax(String text) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuffer buffer = new StringBuffer(text.length());
        while (matcher.find()) {
            String inner = matcher.group(1);
            if (inner.regionMatches(true, 0, "rel_", 0, 4)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            int separator = inner.indexOf('_');
            if (separator <= 0) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            String identifier = inner.substring(0, separator).toLowerCase();
            if (!PlaceholderAPI.isRegistered(identifier)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            PlaceholderExpansion expansion = PlaceholderAPIPlugin.getInstance()
                    .getLocalExpansionManager()
                    .getExpansion(identifier);
            if (!(expansion instanceof Relational)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement("%rel_" + inner + "%"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
