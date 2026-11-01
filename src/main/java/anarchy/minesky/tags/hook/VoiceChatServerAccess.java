package anarchy.minesky.tags.hook;

import anarchy.minesky.tags.AnarchyTags;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Obtém VoicechatServerApi quando o addon SVC não passou por initialize (load tardio / Folia). */
final class VoiceChatServerAccess {

    private VoiceChatServerAccess() {}

    static VoicechatServerApi tryResolve() {
        Plugin voice = Bukkit.getPluginManager().getPlugin("voicechat");
        if (voice == null) {
            return null;
        }
        return scan(voice, 0);
    }

    private static VoicechatServerApi scan(Object root, int depth) {
        if (root == null || depth > 4) {
            return null;
        }
        if (root instanceof VoicechatServerApi serverApi) {
            return serverApi;
        }
        Class<?> type = root instanceof Class<?> clazz ? clazz : root.getClass();
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!VoicechatServerApi.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            try {
                Object result = method.invoke(root instanceof Class<?> ? null : root);
                if (result instanceof VoicechatServerApi api) {
                    return api;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && root instanceof Class<?>) {
                    try {
                        field.setAccessible(true);
                        Object result = field.get(null);
                        VoicechatServerApi nested = scan(result, depth + 1);
                        if (nested != null) {
                            return nested;
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                    continue;
                }
                if (root instanceof Class<?>) {
                    continue;
                }
                if (!VoicechatServerApi.class.isAssignableFrom(field.getType())
                        && !field.getType().getName().startsWith("de.maxhenkel.voicechat")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object result = field.get(root);
                    if (result instanceof VoicechatServerApi api) {
                        return api;
                    }
                    VoicechatServerApi nested = scan(result, depth + 1);
                    if (nested != null) {
                        return nested;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    static void logLateBind(AnarchyTags plugin, VoicechatServerApi api) {
        if (api != null) {
            plugin.getLogger().info("Simple Voice Chat API recuperada (servidor já estava ativo).");
        }
    }
}
