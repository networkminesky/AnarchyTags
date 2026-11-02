package anarchy.minesky.tags.api;

import anarchy.minesky.tags.AnarchyTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ponto de entrada estável para outros plugins (softdepend AnarchyTags).
 */
public final class AnarchyTagsProvider {

    private AnarchyTagsProvider() {}

    @Nullable
    public static AnarchyTagsAPI get() {
        return AnarchyTags.getAPI();
    }

    @NotNull
    public static AnarchyTagsAPI require() {
        AnarchyTagsAPI api = get();
        if (api == null) {
            throw new IllegalStateException("AnarchyTags não está habilitado.");
        }
        return api;
    }
}
