package anarchy.minesky.tags.hook;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

/** Delega para {@code HeadURLResolver} do AnarchyCore quando o plugin está ativo. */
public final class HeadUrlFormatHelper {

    private static final Method DESERIALIZE = resolveDeserialize();

    private HeadUrlFormatHelper() {}

    public static boolean isAvailable() {
        return DESERIALIZE != null && Bukkit.getPluginManager().isPluginEnabled("AnarchyCore");
    }

    public static Component deserialize(String miniMessageInput) {
        if (miniMessageInput == null || miniMessageInput.isBlank() || DESERIALIZE == null) {
            return null;
        }
        try {
            Object result = DESERIALIZE.invoke(null, miniMessageInput);
            if (result instanceof Component component) {
                return component;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Method resolveDeserialize() {
        try {
            Class<?> resolver = Class.forName("anarchy.minesky.core.api.utils.HeadURLResolver");
            return resolver.getMethod("deserializeMiniMessage", String.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
