package anarchy.minesky.tags.utils;

import anarchy.minesky.tags.hook.HeadUrlFormatHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
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
        if (sanitized.contains("<head_url:") && HeadUrlFormatHelper.isAvailable()) {
            return formatWithHeadUrlSegments(sanitized);
        }
        return formatLegacyAndMiniMessage(sanitized);
    }

    /**
     * Cabeça via AnarchyCore + resto da linha com & / MiniMessage (prefixo LuckPerms, etc.).
     */
    private static Component formatWithHeadUrlSegments(String input) {
        List<Component> parts = new ArrayList<>();
        int cursor = 0;
        while (cursor < input.length()) {
            int tagStart = input.indexOf("<head_url:", cursor);
            if (tagStart < 0) {
                appendLegacySegment(parts, input.substring(cursor));
                break;
            }
            if (tagStart > cursor) {
                appendLegacySegment(parts, input.substring(cursor, tagStart));
            }
            int tagEnd = input.indexOf('>', tagStart);
            if (tagEnd < 0) {
                appendLegacySegment(parts, input.substring(tagStart));
                break;
            }
            String tag = input.substring(tagStart, tagEnd + 1);
            Component head = HeadUrlFormatHelper.deserialize(tag);
            parts.add(head != null ? head : Component.text(tag));
            cursor = tagEnd + 1;
        }
        if (parts.isEmpty()) {
            return Component.empty();
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return Component.empty().append(parts);
    }

    private static void appendLegacySegment(List<Component> parts, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        parts.add(formatLegacyAndMiniMessage(segment));
    }

    private static Component formatLegacyAndMiniMessage(String sanitized) {
        Component legacy = LEGACY_SERIALIZER.deserialize(sanitized);
        String modern = MINI_MESSAGE.serialize(legacy).replace("\\", "");
        return MINI_MESSAGE.deserialize(modern);
    }

    public static Component join(List<Component> components) {
        return Component.join(JOIN_CONFIG, components);
    }
}
