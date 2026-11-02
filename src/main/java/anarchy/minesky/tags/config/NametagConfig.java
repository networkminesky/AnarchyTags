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
    private boolean sortingEnabled;
    private int sortingInterval;
    private int sortingSyncPerTick;
    private boolean sortingApplyPrefix;
    private String sortingPrefix;
    private String sortingSuffix;
    private List<String> sortingTypes;
    private int joinDelaySeconds;
    private int joinInitDelayTicks;
    private boolean voiceChatEnabled;
    private boolean voiceChatUsePlaceholder;
    private String voiceChatPlaceholder;
    private String voiceChatTalking;
    private String voiceChatWhispering;
    private String voiceChatQuiet;
    private String voiceChatDisabled;
    private String voiceChatNotInstalled;
    private int voiceChatTalkTimeoutMs;
    private static final String DEFAULT_TALKING_HEAD =
            "<head_url:http://textures.minecraft.net/texture/62e4c4a61e4057e8422a73948be13a3272c286185abde852a6f616a74325e358> ";

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
        joinDelaySeconds = Math.max(0, config.getInt("nametags.join-delay-seconds", 0));
        joinInitDelayTicks = Math.max(0, config.getInt("nametags.join-init-delay-ticks", 0));
        hideInSpectator = config.getBoolean("nametags.hide-in-spectator", true);
        hideInVanish = config.getBoolean("nametags.hide-in-vanish", true);
        lines = config.getStringList("nametags.display.lines");
        if (lines.isEmpty()) {
            lines = List.of("%anarchyeconomy_formatted_tab%%anarchyclans_tab% <white>{player}");
        }
        textShadow = config.getBoolean("nametags.display.text-shadow", true);
        seeThrough = config.getBoolean("nametags.display.see-through", true);
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
        sortingEnabled = config.getBoolean("sorting.enabled", true);
        sortingInterval = Math.max(1, config.getInt("sorting.update-interval", 10));
        sortingSyncPerTick = Math.max(1, config.getInt("sorting.sync-per-tick", 2));
        sortingApplyPrefix = config.getBoolean("sorting.apply-prefix", false);
        sortingPrefix = config.getString("sorting.prefix", "%luckperms_prefix%");
        sortingSuffix = config.getString("sorting.suffix", "%luckperms_suffix%");
        sortingTypes = config.getStringList("sorting.types");
        if (sortingTypes.isEmpty()) {
            sortingTypes = List.of(
                    "PLACEHOLDER_HIGH_TO_LOW:%luckperms_highest_group_weight%",
                    "PLACEHOLDER_A_TO_Z:%player%"
            );
        }
        voiceChatEnabled = config.getBoolean("voice-chat.enabled", true);
        voiceChatUsePlaceholder = config.getBoolean("voice-chat.use-placeholder", false);
        voiceChatPlaceholder = config.getString("voice-chat.placeholder", "%atvc_%");
        voiceChatTalking = config.getString("voice-chat.talking", DEFAULT_TALKING_HEAD);
        voiceChatWhispering = config.getString("voice-chat.whispering", "🔉 ");
        voiceChatQuiet = config.getString("voice-chat.quiet", "");
        voiceChatDisabled = config.getString("voice-chat.disabled", "❌ ");
        voiceChatNotInstalled = config.getString("voice-chat.not-installed", "");
        voiceChatTalkTimeoutMs = Math.max(100, config.getInt("voice-chat.talk-timeout-ms", 350));
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

    public int getJoinDelaySeconds() {
        return joinDelaySeconds;
    }

    public int getJoinInitDelayTicks() {
        return joinInitDelayTicks;
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

    public boolean isSortingEnabled() {
        return sortingEnabled;
    }

    public int getSortingInterval() {
        return sortingInterval;
    }

    public int getSortingSyncPerTick() {
        return sortingSyncPerTick;
    }

    public boolean isSortingApplyPrefix() {
        return sortingApplyPrefix;
    }

    public String getSortingPrefix() {
        return sortingPrefix;
    }

    public String getSortingSuffix() {
        return sortingSuffix;
    }

    public List<String> getSortingTypes() {
        return sortingTypes;
    }

    public boolean isVoiceChatEnabled() {
        return voiceChatEnabled;
    }

    public boolean isVoiceChatUsePlaceholder() {
        return voiceChatUsePlaceholder;
    }

    public String getVoiceChatPlaceholder() {
        return voiceChatPlaceholder;
    }

    public String getVoiceChatTalking() {
        return voiceChatTalking;
    }

    public String getVoiceChatWhispering() {
        return voiceChatWhispering;
    }

    public String getVoiceChatQuiet() {
        return voiceChatQuiet;
    }

    public String getVoiceChatDisabled() {
        return voiceChatDisabled;
    }

    public String getVoiceChatNotInstalled() {
        return voiceChatNotInstalled;
    }

    public int getVoiceChatTalkTimeoutMs() {
        return voiceChatTalkTimeoutMs;
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
