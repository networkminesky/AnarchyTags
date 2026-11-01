package anarchy.minesky.tags.voice;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders estilo VoiceChatPlaceholders (Folia-safe, integrado ao AnarchyTags).
 * %atvc_% %atvc_icon% %atvc_status%
 */
public final class AtvcPlaceholderExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "atvc";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AnarchyTags";
    }

    @Override
    public @NotNull String getVersion() {
        return AnarchyTags.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        if (params.equalsIgnoreCase("status") || params.equalsIgnoreCase("state")) {
            return VoiceChatBridge.resolveState(player).name().toLowerCase(Locale.ROOT);
        }
        if (params.isEmpty() || params.equals("_") || params.equalsIgnoreCase("icon")) {
            return VoiceIconService.iconFor(null, player);
        }
        return null;
    }
}
