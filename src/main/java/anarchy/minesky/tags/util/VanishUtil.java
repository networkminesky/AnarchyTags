package anarchy.minesky.tags.util;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public final class VanishUtil {

    private VanishUtil() {}

    public static boolean isVanished(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        if (player.hasMetadata("vanished")) {
            for (MetadataValue meta : player.getMetadata("vanished")) {
                if (meta.asBoolean()) {
                    return true;
                }
            }
        }

        if (player.hasMetadata("vanish")) {
            for (MetadataValue meta : player.getMetadata("vanish")) {
                if (meta.asBoolean()) {
                    return true;
                }
            }
        }

        try {
            Class<?> apiClass = Class.forName("anarchy.minesky.api.vanish.VanishAPI");
            java.lang.reflect.Method method = apiClass.getMethod("isInvisible", Player.class);
            if ((boolean) method.invoke(null, player)) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}