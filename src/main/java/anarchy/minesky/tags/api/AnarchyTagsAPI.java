package anarchy.minesky.tags.api;

import anarchy.minesky.tags.nick.NickHideMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AnarchyTagsAPI {

    /**
     * Força a atualização do texto da tag para todos que estão olhando o jogador.
     */
    void refreshTag(@NotNull Player target);

    /**
     * Define um formato customizado de placeholder exclusivo para este jogador.
     */
    void setCustomFormat(@NotNull Player player, @Nullable String format);

    /**
     * Recarrega a configuração do plugin e sincroniza as tags na hora.
     */
    void reload();

    /**
     * Registra um filtro de visibilidade (fog of war, zonas, etc.). Thread-safe.
     */
    void registerVisibilityFilter(@NotNull NametagVisibilityFilter filter);

    /**
     * Remove um filtro registrado anteriormente.
     */
    void unregisterVisibilityFilter(@NotNull NametagVisibilityFilter filter);

    /**
     * Avalia se a tag 3D de {@code target} deve aparecer para {@code viewer}
     * (distância, vanish, nick-hide, filtros externos, etc.).
     */
    boolean isNametagVisibleFor(@NotNull Player viewer, @NotNull Player target);

    /**
     * Modo de esconder nick persistido do jogador.
     */
    @NotNull
    NickHideMode getNickHideMode(@NotNull Player player);

    /**
     * Se a linha do nick ({@code {player}}) está habilitada na tag (prefixo / economia / clãs permanecem).
     */
    boolean isNickLineEnabled(@NotNull Player player);

    /**
     * Reavalia visibilidade e texto das tags afetadas por {@code target} (após mudar fog of war, etc.).
     */
    void refreshVisibilityFor(@NotNull Player target);
}
