package anarchy.minesky.tags.utils.helpers;

public final class DependencyHelper {

    private DependencyHelper() {}

    public static void load() {
        // Mantido para compatibilidade; detecção é feita em tempo real via PlaceholderHelper.
    }

    public static boolean isPlaceholderApiEnabled() {
        return PlaceholderHelper.isAvailable();
    }
}
