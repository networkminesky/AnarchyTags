package anarchy.minesky.tags.config;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.entities.DisplayBillboard;
import anarchy.minesky.tags.entities.TextAlignment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.List;

public class NametagConfig {

    private final AnarchyTags plugin;

    private boolean enabled;
    private boolean showSelf;
    private int updateInterval;
    private int visibilityDistance;
    private boolean hideInSpectator;
    private boolean hideInVanish;
    private List<String> lines;
    private boolean textShadow;
    private boolean seeThrough;
    private TextAlignment textAlignment;
    private String background;
    private DisplayBillboard billboard;
    private Vector translation;
    private Vector scale;

    public NametagConfig(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public void load() {
        load(plugin.getConfig());
    }

    public void load(FileConfiguration config) {
        enabled = config.getBoolean("nametags.enabled", true);
        showSelf = config.getBoolean("nametags.show-self", false);
        updateInterval = Math.max(1, config.getInt("nametags.update-interval", 1));
        visibilityDistance = config.getInt("nametags.visibility-distance", 64);
        hideInSpectator = config.getBoolean("nametags.hide-in-spectator", true);
        hideInVanish = config.getBoolean("nametags.hide-in-vanish", true);
        lines = config.getStringList("nametags.display.lines");
        if (lines.isEmpty()) {
            lines = List.of("%anarchyeconomy_formatted_tab%%anarchyclans_tab% <white>{player}");
        }
        textShadow = config.getBoolean("nametags.display.text-shadow", true);
        seeThrough = config.getBoolean("nametags.display.see-through", false);
        textAlignment = parseAlignment(config.getString("nametags.display.text-alignment", "center"));
        background = config.getString("nametags.display.background", "transparent");
        billboard = parseBillboard(config.getString("nametags.display.billboard", "center"));
        translation = new Vector(
                config.getDouble("nametags.display.translation.x", 0),
                config.getDouble("nametags.display.translation.y", 0.08),
                config.getDouble("nametags.display.translation.z", 0)
        );
        scale = new Vector(
                config.getDouble("nametags.display.scale.x", 1),
                config.getDouble("nametags.display.scale.y", 1),
                config.getDouble("nametags.display.scale.z", 1)
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean shouldHideSelf() {
        return !showSelf;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public int getVisibilityDistance() {
        return visibilityDistance;
    }

    public boolean isHideInSpectator() {
        return hideInSpectator;
    }

    public boolean isHideInVanish() {
        return hideInVanish;
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean hasTextShadow() {
        return textShadow;
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public TextAlignment getTextAlignment() {
        return textAlignment;
    }

    public String getBackground() {
        return background;
    }

    public DisplayBillboard getBillboard() {
        return billboard;
    }

    public Vector getTranslation() {
        return translation;
    }

    public Vector getScale() {
        return scale;
    }

    private static TextAlignment parseAlignment(String raw) {
        try {
            return TextAlignment.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TextAlignment.CENTER;
        }
    }

    private static DisplayBillboard parseBillboard(String raw) {
        try {
            return DisplayBillboard.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DisplayBillboard.CENTER;
        }
    }
}
