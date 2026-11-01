package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.entities.ClientTextDisplay;
import anarchy.minesky.tags.entities.TextDisplayDefaults;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import anarchy.minesky.tags.nick.NickHideManager;
import anarchy.minesky.tags.nick.NickHideMode;
import anarchy.minesky.tags.util.VanishUtil;
import anarchy.minesky.tags.utils.ComponentUtils;
import anarchy.minesky.tags.utils.helpers.PlaceholderHelper;
import com.github.retrooper.packetevents.util.Vector3f;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Nametag {

    private final AnarchyTags plugin;
    private final NametagManager manager;
    private final Player player;
    private final ClientTextDisplay display;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Visual> lastVisual = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> mountHash = new ConcurrentHashMap<>();
    private int textCursor;

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

    public int getViewerCount() {
        return viewers.size();
    }

    public int getSpawnedViewerCount() {
        return display.getSpawnedViewerCount();
    }

    public int getDisplayEntityId() {
        return display.getEntityId();
    }

    public boolean isSpawnedForViewer(Player viewer) {
        return viewer != null && display.isSpawnedFor(viewer);
    }

    public void hideForAll() {
        for (UUID viewerId : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                hide(viewer);
            } else {
                forgetViewer(viewerId);
            }
        }
    }

    public void invalidateVisuals() {
        lastVisual.clear();
    }

    public void updateVisibilityForAll() {
        updateVisibility(48);
    }

    /**
     * Mostra ou esconde só quem entrou ou saiu do alcance.
     * Não reenvia texto. Devolve quantos spawns novos foram feitos.
     */
    public int updateVisibility(int showBudget) {
        if (!player.isOnline() || player.getWorld() == null) {
            return 0;
        }

        Location location = player.getLocation();
        display.setLocation(location);
        int shown = 0;
        double maxDistSq = (long) visibilityDistance * visibilityDistance;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getWorld().equals(player.getWorld())) {
                if (viewers.contains(viewer.getUniqueId())) {
                    hide(viewer);
                }
                continue;
            }
            if (!shouldSee(viewer, maxDistSq)) {
                if (viewers.contains(viewer.getUniqueId())) {
                    hide(viewer);
                }
                continue;
            }

            boolean visible = viewers.contains(viewer.getUniqueId()) && display.isSpawnedFor(viewer);
            if (visible) {
                continue;
            }
            if (shown >= showBudget) {
                continue;
            }
            if (display.isSpawnedFor(viewer)) {
                display.destroy(viewer);
                lastVisual.remove(viewer.getUniqueId());
                mountHash.remove(viewer.getUniqueId());
            }
            show(viewer);
            shown++;
        }

        if (!viewers.isEmpty()) {
            for (UUID viewerId : List.copyOf(viewers)) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) {
                    forgetViewer(viewerId);
                    continue;
                }
                if (!shouldSee(viewer, maxDistSq)) {
                    hide(viewer);
                }
            }
        }
        return shown;
    }

    private boolean shouldSee(Player viewer) {
        long maxDistSq = (long) visibilityDistance * visibilityDistance;
        return shouldSee(viewer, maxDistSq);
    }

    private boolean shouldSee(Player viewer, double maxDistSq) {
        if (viewer == null || !viewer.isOnline() || viewer.isDead()) {
            return false;
        }
        if (!manager.isViewerReady(viewer)) {
            return false;
        }
        if (!manager.isPacketsReady(viewer)) {
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
        if (!AnarchyTags.getInstance().getNickHideManager().canSeeHiddenNametag(viewer, player)) {
            return false;
        }
        if (!viewer.getWorld().equals(player.getWorld())) {
            return false;
        }
        if (player.isInvisible()) {
            return false;
        }
        return viewer.getLocation().distanceSquared(player.getLocation()) < maxDistSq;
    }

    public void forgetViewer(UUID viewerId) {
        viewers.remove(viewerId);
        display.forget(viewerId);
        lastVisual.remove(viewerId);
        mountHash.remove(viewerId);
    }

    /** Envia a tag para quem ainda não a vê. Devolve false se não houve pacote. */
    public boolean presentTo(Player viewer) {
        if (viewer == null || !shouldSee(viewer)) {
            return false;
        }
        if (viewers.contains(viewer.getUniqueId()) && display.isSpawnedFor(viewer)) {
            return false;
        }
        show(viewer);
        return true;
    }

    public void show(Player viewer) {
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) {
            return;
        }

        viewers.add(viewer.getUniqueId());
        update(viewer);
    }

    public void hide(Player viewer) {
        viewers.remove(viewer.getUniqueId());
        display.despawn(viewer);
        lastVisual.remove(viewer.getUniqueId());
        mountHash.remove(viewer.getUniqueId());
    }

    public void update(Player viewer) {
        display.setLocation(player.getLocation());
        boolean first = !display.isSpawnedFor(viewer);
        if (first) {
            display.spawn(viewer);
            viewers.add(viewer.getUniqueId());
        }

        boolean changed = writeVisual(viewer);
        if (first || changed) {
            display.update(viewer);
        }
        mountIfNeeded(viewer, first);
    }

    private void mountIfNeeded(Player viewer, boolean force) {
        int hash = passengerHash();
        if (!force && mountHash.getOrDefault(viewer.getUniqueId(), Integer.MIN_VALUE) == hash) {
            return;
        }
        display.mount(player, viewer);
        mountHash.put(viewer.getUniqueId(), hash);
    }

    private int passengerHash() {
        int hash = 1;
        for (Entity passenger : player.getPassengers()) {
            hash = 31 * hash + passenger.getEntityId();
        }
        return hash;
    }

    private boolean writeVisual(Player viewer) {
        NametagConfig serverConfig = plugin.config().getNametagConfig();
        String contentKey = buildContentKey(viewer);
        boolean shadow = TagsPlayerConfig.nametagTextShadowEnabled(viewer, serverConfig.hasTextShadow());
        boolean empty = contentKey.isEmpty();
        int background = empty ? 0 : resolveBackgroundForViewer(viewer, serverConfig, Component.text("x"));
        Visual visual = new Visual(contentKey, shadow, background);
        Visual previous = lastVisual.get(viewer.getUniqueId());
        if (visual.equals(previous)) {
            return false;
        }
        lastVisual.put(viewer.getUniqueId(), visual);
        Component text = empty ? Component.empty() : composeDisplay(contentKey);
        display.setText(text);
        display.setTextShadow(shadow);
        display.setBackground(background);
        return true;
    }

    private String buildContentKey(Player viewer) {
        List<String> sourceLines = manager.getLinesFor(player);
        NickHideManager nickHide = AnarchyTags.getInstance().getNickHideManager();
        NickHideMode hideMode = nickHide.getMode(player);
        String playerToken = formatPlayerToken(viewer, hideMode);
        StringBuilder key = new StringBuilder(128);
        for (String line : sourceLines) {
            String modified = line.replace("{player}", playerToken);
            modified = modified.replace("{voiceicon}", VoiceChatBridge.iconFor(viewer, player));
            modified = PlaceholderHelper.apply(viewer, player, modified);
            if (!modified.isBlank()) {
                if (!key.isEmpty()) {
                    key.append('\n');
                }
                key.append(modified.trim());
            }
        }
        return key.toString();
    }

    private Component composeDisplay(String contentKey) {
        String[] lines = contentKey.split("\n", -1);
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            components.add(ComponentUtils.format(line));
        }
        if (components.isEmpty()) {
            return Component.empty();
        }
        return components.size() == 1 ? components.getFirst() : ComponentUtils.join(components);
    }

    /** Atualiza metadata só se texto/sombra/fundo mudaram — não remonta nem respawna. */
    public void refreshTextIfChanged(Player viewer) {
        if (viewer == null || !viewers.contains(viewer.getUniqueId()) || !display.isSpawnedFor(viewer)) {
            return;
        }
        if (writeVisual(viewer)) {
            display.update(viewer);
        }
    }

    public void refreshTexts(int max) {
        if (max <= 0 || viewers.isEmpty()) {
            return;
        }
        UUID[] ids = viewers.toArray(UUID[]::new);
        int steps = Math.min(max, ids.length);
        for (int i = 0; i < steps; i++) {
            UUID viewerId = ids[Math.floorMod(textCursor++, ids.length)];
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && viewers.contains(viewerId)) {
                refreshTextIfChanged(viewer);
            }
        }
    }

    /** Chamado quando ícone de voz ou placeholder muda. */
    public void refreshAllViewerTextsIfChanged() {
        for (UUID viewerId : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                refreshTextIfChanged(viewer);
            }
        }
    }

    public void teleportForAll() {
        syncPositionForViewers();
    }

    /** Teleport leve — sem remount (evita piscar). Mount só no spawn inicial. */
    public void syncPositionForViewers() {
        if (!player.isOnline() || viewers.isEmpty()) {
            return;
        }
        Location loc = player.getLocation();
        display.setLocation(loc);
        for (UUID viewerId : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline() || !display.isSpawnedFor(viewer)) {
                continue;
            }
            display.teleportFor(viewer);
        }
    }

    private record Visual(String contentKey, boolean shadow, int background) {}

    private String formatPlayerToken(Player viewer, NickHideMode hideMode) {
        String name = player.getName();
        boolean adminView = viewer != null && viewer.hasPermission(NickHideManager.PERM_SEE_HINT);
        return switch (hideMode) {
            case OFF -> name;
            case OBFUSCATED -> adminView ? "§8" + name : "§k" + name + "§r";
            case ADMIN_HINT -> adminView ? "§8" + name : name;
            case FULL_HIDDEN -> name;
        };
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
            return TextDisplayDefaults.BACKGROUND;
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
