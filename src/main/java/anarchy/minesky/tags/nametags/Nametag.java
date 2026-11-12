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
    private volatile boolean sneaking;

    public Nametag(NametagManager manager, Player player) {
        this.plugin = AnarchyTags.getInstance();
        this.manager = manager;
        this.player = player;

        NametagConfig config = plugin.config().getNametagConfig();
        this.hideSelf = config.shouldHideSelf();
        this.visibilityDistance = config.getVisibilityDistance();
        this.hideInSpectator = config.isHideInSpectator();
        this.hideInVanish = config.isHideInVanish();

        // Inicia a localização já na altura dos olhos para evitar spawn no pé
        Location spawnLoc = player.getLocation().clone().add(0, 1.8, 0);
        this.display = new ClientTextDisplay(spawnLoc);
        this.display.setTranslation(new Vector3f(
                (float) config.getTranslation().getX(),
                (float) config.getTranslation().getY(),
                (float) config.getTranslation().getZ()
        ));
        this.display.setScale(config.getScale());
        this.display.setTextAlignment(config.getTextAlignment());
        this.sneaking = player.isSneaking();
        this.display.setSeeThrough(!this.sneaking && config.isSeeThrough());
        this.display.setBillboard(config.getBillboard());
    }

    public Player getPlayer() {
        return player;
    }

    public int getVisibilityDistance() {
        return visibilityDistance;
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

    public void invalidateVisual(UUID viewerId) {
        if (viewerId != null) {
            lastVisual.remove(viewerId);
        }
    }

    public void updateVisibilityForAll() {
        updateVisibility(48);
    }

    public int updateVisibility(int showBudget) {
        if (!player.isOnline() || player.getWorld() == null) {
            return 0;
        }

        placeAboveHead();
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
        return canShowTo(viewer, maxDistSq);
    }

    public boolean canShowTo(Player viewer, double maxDistSq) {
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
        if (!manager.passesExternalVisibility(viewer, player)) {
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
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) {
            return;
        }

        viewers.add(viewer.getUniqueId());
        update(viewer);
    }

    public void hide(Player viewer) {
        if (viewer == null) return;
        viewers.remove(viewer.getUniqueId());
        display.despawn(viewer, player);
        lastVisual.remove(viewer.getUniqueId());
        mountHash.remove(viewer.getUniqueId());
    }

    public void update(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        placeAboveHead();
        // Sincroniza com o estado real do Bukkit como segurança
        this.sneaking = player.isSneaking();

        boolean first = !display.isSpawnedFor(viewer);

        if (first) {
            writeVisual(viewer);
            boolean seeThrough = !sneaking && plugin.config().getNametagConfig().isSeeThrough();
            display.setSeeThrough(seeThrough);
            display.spawn(viewer);
            display.update(viewer);
            display.mount(player, viewer);
            viewers.add(viewer.getUniqueId());
            mountHash.put(viewer.getUniqueId(), passengerHash());
            return;
        }

        // Se já está spawnado: só envia metadata se o texto ou estilo mudou
        if (writeVisual(viewer)) {
            boolean seeThrough = !sneaking && plugin.config().getNametagConfig().isSeeThrough();
            display.setSeeThrough(seeThrough);
            display.update(viewer);
        }

        // Só remonta se os passageiros reais do player mudaram (ex: subiu num barco/cavalo)
        int currentHash = passengerHash();
        Integer lastHash = mountHash.get(viewer.getUniqueId());
        if (lastHash == null || lastHash != currentHash) {
            display.mount(player, viewer);
            mountHash.put(viewer.getUniqueId(), currentHash);
        }
    }

    /**
     * Chamado quando o jogador aperta ou solta o Shift.
     */
    public void onSneakToggle(boolean isSneaking) {
        this.sneaking = isSneaking;
        updateSeeThrough();
    }

    /**
     * Atualiza o see-through e envia o pacote de metadata para quem está vendo a tag.
     */
    public void updateSeeThrough() {
        boolean seeThrough = !sneaking && plugin.config().getNametagConfig().isSeeThrough();
        display.setSeeThrough(seeThrough);

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && display.isSpawnedFor(viewer)) {
                display.update(viewer);
            }
        }
    }

    public void followAllViewers() {
        if (!player.isOnline() || player.getWorld() == null) {
            return;
        }
        placeAboveHead();
        double maxDistSq = (long) visibilityDistance * visibilityDistance;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getWorld().equals(player.getWorld()) || !shouldSee(viewer, maxDistSq)) {
                if (viewers.contains(viewer.getUniqueId()) || display.isSpawnedFor(viewer)) {
                    hide(viewer);
                }
                continue;
            }
            viewers.add(viewer.getUniqueId());
            update(viewer);
        }
    }

    private void placeAboveHead() {
        // Define a coordenada na altura da cabeça (1.8 blocos acima dos pés)
        Location loc = player.getLocation().clone().add(0, 1.8, 0);
        display.setLocation(loc);
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
        display.setViewerVisual(viewer, text, background, shadow);
        return true;
    }

    private String buildContentKey(Player viewer) {
        NickHideManager nickHide = AnarchyTags.getInstance().getNickHideManager();
        NickHideMode hideMode = nickHide.getMode(player);
        if (hideMode == NickHideMode.OBFUSCATED) {
            return formatPlayerToken(viewer, hideMode);
        }
        List<String> sourceLines = manager.getLinesFor(player);
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

    public void refreshAllViewerTextsIfChanged() {
        for (UUID viewerId : List.copyOf(viewers)) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                refreshTextIfChanged(viewer);
            }
        }
    }

    public void forceRefreshAllViewerTexts() {
        lastVisual.clear();
        refreshAllViewerTextsIfChanged();
    }

    public void teleportForAll() {
        followAllViewers();
    }

    public void syncPositionForViewers() {
        followAllViewers();
    }

    private record Visual(String contentKey, boolean shadow, int background) {}

    private String formatPlayerToken(Player viewer, NickHideMode hideMode) {
        NickHideManager nickHide = AnarchyTags.getInstance().getNickHideManager();
        if (hideMode != NickHideMode.OBFUSCATED && !nickHide.isNickLineEnabled(player)) {
            return "";
        }
        String name = player.getName();
        boolean adminView = viewer != null && viewer.hasPermission(NickHideManager.PERM_OBFUSCATED_VIEW);
        return switch (hideMode) {
            case OFF -> name;
            case OBFUSCATED -> adminView ? "§8" + name : "§k" + name + "§r";
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