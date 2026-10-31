package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.entities.ClientTextDisplay;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.util.VanishUtil;
import anarchy.minesky.tags.utils.ComponentUtils;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import anarchy.minesky.tags.utils.helpers.PlaceholderHelper;
import com.github.retrooper.packetevents.util.Vector3f;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Nametag {

    private final AnarchyTags plugin;
    private final NametagManager manager;
    private final Player player;
    private final ClientTextDisplay display;
    private final Set<UUID> viewers = new HashSet<>();

    private final boolean hideSelf;
    private final int visibilityDistance;
    private final boolean hideInSpectator;
    private final boolean hideInVanish;

    public Nametag(NametagManager manager, Player player) {
        this.plugin = AnarchyTags.getInstance();
        this.manager = manager;
        this.player = player;

        NametagConfig config = plugin.config().getNametagConfig();
        this.hideSelf = config.shouldHideSelf();
        this.visibilityDistance = config.getVisibilityDistance();
        this.hideInSpectator = config.isHideInSpectator();
        this.hideInVanish = config.isHideInVanish();

        this.display = new ClientTextDisplay(player.getLocation().clone());
        this.display.setTranslation(new Vector3f(
                (float) config.getTranslation().getX(),
                (float) config.getTranslation().getY(),
                (float) config.getTranslation().getZ()
        ));
        this.display.setScale(config.getScale());
        this.display.setTextAlignment(config.getTextAlignment());
        this.display.setSeeThrough(config.isSeeThrough());
        this.display.setBillboard(config.getBillboard());
    }

    public Player getPlayer() {
        return player;
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public void hideForAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            hide(viewer);
        }
    }

    public void updateVisibilityForAll() {
        display.setLocation(player.getLocation());

        viewers.removeIf(uuid -> {
            Player viewer = Bukkit.getPlayer(uuid);
            return viewer == null || !viewer.isOnline();
        });

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean shouldSee = shouldSee(viewer);
            boolean visible = viewers.contains(viewer.getUniqueId());

            if (shouldSee) {
                if (visible) {
                    update(viewer);
                } else {
                    show(viewer);
                }
            } else if (visible) {
                hide(viewer);
            }
        }
    }

    private boolean shouldSee(Player viewer) {
        if (viewer == null || !viewer.isOnline() || viewer.isDead()) {
            return false;
        }
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) {
            return false;
        }
        if (player.isDead()) {
            return false;
        }
        if (hideInSpectator && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (hideInVanish && VanishUtil.isVanished(player)) {
            return false;
        }
        if (!viewer.getWorld().equals(player.getWorld())) {
            return false;
        }
        if (player.isInvisible() || !viewer.canSee(player)) {
            return false;
        }
        return viewer.getLocation().distanceSquared(player.getLocation())
                < (long) visibilityDistance * visibilityDistance;
    }

    public void show(Player viewer) {
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) {
            return;
        }

        viewers.add(viewer.getUniqueId());
        if (!display.isSpawnedFor(viewer)) {
            display.spawn(viewer);
        }
        update(viewer);
    }

    public void hide(Player viewer) {
        viewers.remove(viewer.getUniqueId());
        display.despawn(viewer);
    }

    public void update(Player viewer) {
        NametagHandler.hide(player, viewer);

        if (!display.isSpawnedFor(viewer)) {
            display.spawn(viewer);
            viewers.add(viewer.getUniqueId());
        }

        NametagConfig serverConfig = plugin.config().getNametagConfig();
        Component text = getText(viewer);
        display.setLocation(player.getLocation());
        display.setText(text);
        display.setTextShadow(TagsPlayerConfig.nametagTextShadowEnabled(viewer, serverConfig.hasTextShadow()));
        display.setBackground(resolveBackgroundForViewer(viewer, serverConfig, text));
        display.mount(player, viewer);
        display.update(viewer);
    }

    public void teleportForAll() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                display.teleportFor(viewer);
            }
        }
    }

    private Component getText(Player viewer) {
        List<String> sourceLines = manager.getLinesFor(player);
        List<Component> components = new ArrayList<>(sourceLines.size());

        for (String line : sourceLines) {
            String modified = line.replace("{player}", player.getName());
            modified = PlaceholderHelper.apply(viewer, player, modified);
            Component formatted = ComponentUtils.format(modified.trim());
            if (!formatted.equals(Component.empty())) {
                components.add(formatted);
            }
        }

        if (components.isEmpty()) {
            return Component.empty();
        }
        return components.size() == 1 ? components.getFirst() : ComponentUtils.join(components);
    }

    private static int resolveBackgroundForViewer(Player viewer, NametagConfig serverConfig, Component text) {
        if (text.equals(Component.empty())) {
            return 0;
        }
        boolean serverWantsBackground = !"transparent".equalsIgnoreCase(serverConfig.getBackground());
        if (!TagsPlayerConfig.nametagBackgroundEnabled(viewer, serverWantsBackground)) {
            return 0;
        }
        return resolveBackground(serverConfig.getBackground());
    }

    private static int resolveBackground(String background) {
        if (background == null || background.equalsIgnoreCase("default")) {
            return -1;
        }
        if (background.equalsIgnoreCase("transparent")) {
            return 0;
        }
        String hex = background.startsWith("#") ? background.substring(1) : background;
        Color color = Color.fromARGB((int) Long.parseLong(hex, 16));
        if (hex.length() == 6) {
            color = color.setAlpha(255);
        }
        return color.asARGB();
    }
}
