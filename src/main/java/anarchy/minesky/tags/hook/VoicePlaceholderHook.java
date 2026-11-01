package anarchy.minesky.tags.hook;

import anarchy.minesky.tags.voice.AtvcPlaceholderExpansion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;

public final class VoicePlaceholderHook {

    private static PlaceholderExpansion expansion;

    private VoicePlaceholderHook() {}

    public static void register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        if (expansion != null) {
            return;
        }
        expansion = new AtvcPlaceholderExpansion();
        expansion.register();
    }

    public static void unregister() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }
}
