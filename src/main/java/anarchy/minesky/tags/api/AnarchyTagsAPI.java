package anarchy.minesky.tags.api;

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
}