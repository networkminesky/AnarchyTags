package anarchy.minesky.tags.voice;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import org.bukkit.entity.Player;

/**
 * Resolve ícone de voz via config (head_url / PAPI) e estado da API Simple Voice Chat.
 */
public final class VoiceIconService {

    private VoiceIconService() {}

    public static String iconFor(Player viewer, Player target) {
        if (target == null) {
            return "";
        }
        NametagConfig config = AnarchyTags.getInstance().config().getNametagConfig();
        if (!config.isVoiceChatEnabled()) {
            return "";
        }
        VoiceIconState state = VoiceChatBridge.resolveState(target);
        return format(config, state);
    }

    public static String format(NametagConfig config, VoiceIconState state) {
        return switch (state) {
            case TALKING -> config.getVoiceChatTalking();
            case WHISPERING -> config.getVoiceChatWhispering();
            case QUIET -> config.getVoiceChatQuiet();
            case DISABLED -> config.getVoiceChatDisabled();
            case NOT_INSTALLED -> config.getVoiceChatNotInstalled();
        };
    }
}
