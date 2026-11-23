package anarchy.minesky.tags.util;

import java.util.UUID;

public final class BedrockUtil {

    private static final String FLOODGATE_UUID_PREFIX = "00000000-0000-0000-";

    private BedrockUtil() {}

    public static boolean isMathematicallyBedrock(final UUID uuid) {
        return uuid != null && uuid.toString().startsWith(FLOODGATE_UUID_PREFIX);
    }
}