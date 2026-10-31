package anarchy.minesky.tags.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public final class ComponentUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final JoinConfiguration JOIN_CONFIG = JoinConfiguration.separator(Component.newline());

    private ComponentUtils() {}

    public static Component format(String input) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        String sanitized = input.indexOf('§') != -1 ? input.replace('§', '&') : input;
        Component legacy = LEGACY_SERIALIZER.deserialize(sanitized);
        String modern = MINI_MESSAGE.serialize(legacy).replace("\\", "");
        return MINI_MESSAGE.deserialize(modern);
    }

    public static Component join(List<Component> components) {
        return Component.join(JOIN_CONFIG, components);
    }
}
