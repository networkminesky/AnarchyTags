package anarchy.minesky.tags.nick;

import java.util.Locale;

public enum NickHideMode {
    OFF,
    /** Tag visível com nick ofuscado (&k). */
    OBFUSCATED,
    /** Jogadores comuns não veem; admins com permissão veem &8. */
    ADMIN_HINT,
    /** Tag totalmente invisível (só quem pode usar). */
    FULL_HIDDEN;

    public static NickHideMode fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "off", "normal", "nenhum" -> OFF;
            case "obfuscated", "ofuscado", "k", "obf" -> OBFUSCATED;
            case "admin", "admin_hint", "dica", "hint" -> ADMIN_HINT;
            case "full", "invisivel", "hidden", "full_hidden" -> FULL_HIDDEN;
            default -> OFF;
        };
    }

    public String configId() {
        return switch (this) {
            case OFF -> "off";
            case OBFUSCATED -> "obfuscated";
            case ADMIN_HINT -> "admin_hint";
            case FULL_HIDDEN -> "full_hidden";
        };
    }
}
