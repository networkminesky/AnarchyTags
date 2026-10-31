package anarchy.minesky.tags.hook;

import anarchy.minesky.core.api.MineSkyAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOptionSpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategorySpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Opções de visual da tag no menu {@code /config} (preferência de quem está olhando).
 */
public final class TagsPlayerConfig {

    public static final String CATEGORY_NICK = "minesky_nick";
    public static final String KEY_NAMETAG_BACKGROUND = "anarchytags:nametag_background";
    public static final String KEY_NAMETAG_TEXT_SHADOW = "anarchytags:nametag_text_shadow";

    private static PlayerConfigBooleanOption nametagBackground;
    private static PlayerConfigBooleanOption nametagTextShadow;

    private TagsPlayerConfig() {}

    public static void register(AnarchyTags plugin, NametagConfig config) {
        PlayerConfigAPI playerConfig = MineSkyAPI.playerConfig();
        if (playerConfig == null) {
            plugin.getLogger().warning("PlayerConfigAPI indisponível; opções de tag no /config não registradas.");
            return;
        }

        PlayerConfigRegistry registry = playerConfig.registry();

        if (registry.category(CATEGORY_NICK).isEmpty()) {
            registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(CATEGORY_NICK)
                    .title(Component.text("Exibição do Nick"))
                    .buttonLabel(Component.text("Visual do Nick"))
                    .iconMaterial(Material.NAME_TAG)
                    .sortOrder(40)
                    .build());
        }

        boolean defaultBackground = !"transparent".equalsIgnoreCase(config.getBackground());
        boolean defaultShadow = config.hasTextShadow();

        nametagBackground = registry.booleanOption(KEY_NAMETAG_BACKGROUND).orElseGet(() ->
                registry.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(KEY_NAMETAG_BACKGROUND, CATEGORY_NICK)
                                .label(Component.text("Caixa escura na tag"))
                                .defaultValue(defaultBackground)
                                .build()));

        nametagTextShadow = registry.booleanOption(KEY_NAMETAG_TEXT_SHADOW).orElseGet(() ->
                registry.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(KEY_NAMETAG_TEXT_SHADOW, CATEGORY_NICK)
                                .label(Component.text("Sombra no texto da tag"))
                                .defaultValue(defaultShadow)
                                .build()));
    }

    public static boolean nametagBackgroundEnabled(Player viewer, boolean serverDefault) {
        return readBoolean(nametagBackground, KEY_NAMETAG_BACKGROUND, viewer, serverDefault);
    }

    public static boolean nametagTextShadowEnabled(Player viewer, boolean serverDefault) {
        return readBoolean(nametagTextShadow, KEY_NAMETAG_TEXT_SHADOW, viewer, serverDefault);
    }

    private static boolean readBoolean(PlayerConfigBooleanOption handle, String key, Player player,
                                         boolean fallback) {
        if (player == null) {
            return fallback;
        }
        if (handle != null) {
            return handle.get(player);
        }
        PlayerConfigAPI api = MineSkyAPI.playerConfig();
        if (api != null) {
            return api.getBoolean(key, player);
        }
        return fallback;
    }
}
