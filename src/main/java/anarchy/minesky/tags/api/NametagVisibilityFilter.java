package anarchy.minesky.tags.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Integração externa (ex.: fog of war). Filtros com {@link #priority()} maior rodam primeiro;
 * o primeiro que retornar {@code false} esconde a tag para aquele par viewer→alvo.
 */
@FunctionalInterface
public interface NametagVisibilityFilter {

    /**
     * @return {@code true} se a tag 3D pode ser exibida para este viewer, {@code false} para ocultar
     */
    boolean shouldShow(@NotNull Player viewer, @NotNull Player target);

    /**
     * Filtros com valor maior são consultados primeiro (ex.: fog of war = 100, outros = 0).
     */
    default int priority() {
        return 0;
    }
}
