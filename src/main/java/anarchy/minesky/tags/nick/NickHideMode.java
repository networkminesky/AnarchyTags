package anarchy.minesky.tags.nick;

import java.util.Locale;

public enum NickHideMode {
    OFF,
    /** Tag visível só com o nick ofuscado (&k); prefixo, dinheiro e clã ocultos. */
    OBFUSCATED,
    /** Tag totalmente invisível (admins). */
    FULL_HIDDEN;

    public static NickHideMode fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "off", "normal", "nenhum" -> OFF;
            case "obfuscated", "ofuscado", "k", "obf" -> OBFUSCATED;
            case "full", "invisivel", "hidden", "full_hidden", "admin", "admin_hint", "dica", "hint" -> FULL_HIDDEN;
            default -> OFF;
        };
    }

    public String configId() {
        return switch (this) {
            case OFF -> "off";
            case OBFUSCATED -> "obfuscated";
            case FULL_HIDDEN -> "full_hidden";
        };
    }
}
