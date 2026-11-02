package anarchy.minesky.tags.hook;

import anarchy.minesky.core.api.MineSkyAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOptionSpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigCategorySpec;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.config.ScoreboardConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Opções de visual da tag e da scoreboard no menu {@code /config}.
 */
public final class TagsPlayerConfig {

    public static final String CATEGORY_NICK = "minesky_nick";
    public static final String CATEGORY_SCOREBOARD = "minesky_scoreboard";

    public static final String KEY_NAMETAG_BACKGROUND = "anarchytags:nametag_background";
    public static final String KEY_NAMETAG_TEXT_SHADOW = "anarchytags:nametag_text_shadow";
    public static final String KEY_SCOREBOARD_ENABLED = "anarchytags:scoreboard_enabled";

    private static PlayerConfigBooleanOption nametagBackground;
    private static PlayerConfigBooleanOption nametagTextShadow;
    private static PlayerConfigBooleanOption scoreboardEnabled;
    private static final Map<String, PlayerConfigBooleanOption> scoreboardToggles = new HashMap<>();

    private TagsPlayerConfig() {}

    public static void register(AnarchyTags plugin, NametagConfig nametagConfig, ScoreboardConfig scoreboardConfig) {
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

        boolean defaultBackground = !"transparent".equalsIgnoreCase(nametagConfig.getBackground());
        boolean defaultShadow = nametagConfig.hasTextShadow();

        nametagBackground = registry.booleanOption(KEY_NAMETAG_BACKGROUND).orElseGet(() ->
                registry.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(KEY_NAMETAG_BACKGROUND, CATEGORY_NICK)
                                .label(Component.text("Fundo escuro"))
                                .defaultValue(defaultBackground)
                                .build()));

        nametagTextShadow = registry.booleanOption(KEY_NAMETAG_TEXT_SHADOW).orElseGet(() ->
                registry.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(KEY_NAMETAG_TEXT_SHADOW, CATEGORY_NICK)
                                .label(Component.text("Sombra no texto da tag"))
                                .defaultValue(defaultShadow)
                                .build()));

        registerScoreboardOptions(plugin, registry, scoreboardConfig);
    }

    private static void registerScoreboardOptions(AnarchyTags plugin, PlayerConfigRegistry registry,
                                                  ScoreboardConfig scoreboardConfig) {
        if (registry.category(CATEGORY_SCOREBOARD).isEmpty()) {
            registry.registerCategory(plugin, PlayerConfigCategorySpec.builder(CATEGORY_SCOREBOARD)
                    .title(Component.text("Scoreboard"))
                    .buttonLabel(Component.text("Scoreboard"))
                    .iconMaterial(Material.OAK_SIGN)
                    .sortOrder(35)
                    .build());
        }

        scoreboardEnabled = registry.booleanOption(KEY_SCOREBOARD_ENABLED).orElseGet(() ->
                registry.registerBooleanOption(plugin,
                        PlayerConfigBooleanOptionSpec.builder(KEY_SCOREBOARD_ENABLED, CATEGORY_SCOREBOARD)
                                .label(Component.text("Mostrar scoreboard"))
                                .defaultValue(true)
                                .build()));

        for (ScoreboardConfig.ToggleLine toggle : scoreboardConfig.getToggles()) {
            String key = scoreboardToggleKey(toggle.id());
            PlayerConfigBooleanOption handle = registry.booleanOption(key).orElseGet(() ->
                    registry.registerBooleanOption(plugin,
                            PlayerConfigBooleanOptionSpec.builder(key, CATEGORY_SCOREBOARD)
                                    .label(Component.text(toggle.label()))
                                    .defaultValue(toggle.defaultEnabled())
                                    .build()));
            scoreboardToggles.put(toggle.id(), handle);
        }
    }

    public static String scoreboardToggleKey(String id) {
        return "anarchytags:scoreboard_" + id;
    }

    public static boolean nametagBackgroundEnabled(Player viewer, boolean serverDefault) {
        return readBoolean(nametagBackground, KEY_NAMETAG_BACKGROUND, viewer, serverDefault);
    }

    public static boolean nametagTextShadowEnabled(Player viewer, boolean serverDefault) {
        return readBoolean(nametagTextShadow, KEY_NAMETAG_TEXT_SHADOW, viewer, serverDefault);
    }

    public static boolean scoreboardEnabled(Player viewer, boolean fallback) {
        return readBoolean(scoreboardEnabled, KEY_SCOREBOARD_ENABLED, viewer, fallback);
    }

    public static boolean scoreboardToggleEnabled(Player viewer, String toggleId, boolean fallback) {
        PlayerConfigBooleanOption handle = scoreboardToggles.get(toggleId);
        return readBoolean(handle, scoreboardToggleKey(toggleId), viewer, fallback);
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
