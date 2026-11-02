package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.api.AnarchyTagsAPI;
import anarchy.minesky.tags.api.NametagVisibilityFilter;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.nick.NickHideMode;
import anarchy.minesky.tags.sorting.TabOrder;
import anarchy.minesky.tags.util.FoliaTasks;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NametagManager implements AnarchyTagsAPI {

    private final TabOrder tabOrder;
    private final Map<UUID, Nametag> nametags = new ConcurrentHashMap<>();
    private final Map<UUID, String> customFormats = new ConcurrentHashMap<>();
    private final Set<UUID> viewerReady = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> packetsAfterMs = new ConcurrentHashMap<>();
    private final List<NametagVisibilityFilter> visibilityFilters = new CopyOnWriteArrayList<>();
    private final Map<Integer, UUID> ownerByEntityId = new ConcurrentHashMap<>();

    public NametagManager(TabOrder tabOrder) {
        this.tabOrder = tabOrder;
    }

    public Nametag get(Player player) {
        return nametags.get(player.getUniqueId());
    }

    public Collection<Nametag> getAll() {
        return nametags.values();
    }

    List<String> getLinesFor(Player player) {
        String custom = customFormats.get(player.getUniqueId());
        if (custom != null) {
            return List.of(custom);
        }
        return AnarchyTags.getInstance().config().getNametagConfig().getLines();
    }

    public boolean isViewerReady(Player viewer) {
        return viewer != null && viewerReady.contains(viewer.getUniqueId());
    }

    public boolean isPacketsReady(Player viewer) {
        if (viewer == null) {
            return false;
        }
        Long after = packetsAfterMs.get(viewer.getUniqueId());
        return after != null && System.currentTimeMillis() >= after;
    }

    public boolean isPacketsReady(UUID viewerId) {
        Long after = packetsAfterMs.get(viewerId);
        return after != null && System.currentTimeMillis() >= after;
    }

    public void clearViewerReady(Player player) {
        if (player != null) {
            UUID id = player.getUniqueId();
            viewerReady.remove(id);
            packetsAfterMs.remove(id);
        }
    }

    public TabOrder tabOrder() {
        return tabOrder;
    }

    public long millisUntilPacketsReady(Player viewer) {
        if (viewer == null) {
            return 0;
        }
        Long after = packetsAfterMs.get(viewer.getUniqueId());
        if (after == null) {
            return 0;
        }
        long diff = after - System.currentTimeMillis();
        return Math.max(0, diff);
    }

    /** Init na thread da entidade — sem delay artificial. */
    public void scheduleJoin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        NametagConfig config = AnarchyTags.getInstance().config().getNametagConfig();
        long initTicks = Math.max(0, config.getJoinInitDelayTicks());
        if (initTicks <= 0) {
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), player, () -> startJoin(player));
        } else {
            FoliaTasks.runOnEntityDelayed(
                    AnarchyTags.getInstance(),
                    player,
                    () -> startJoin(player),
                    initTicks
            );
        }
    }

    public void create(Player player) {
        scheduleJoin(player);
    }

    private void startJoin(Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline()) {
            return;
        }

        Nametag existing = nametags.remove(id);
        if (existing != null) {
            existing.hideForAll();
        }

        nametags.put(id, new Nametag(this, player));
        ownerByEntityId.put(player.getEntityId(), id);
        tabOrder.precomputeAsync(id, player.getName());
        AnarchyTags.getInstance().getNickHideManager().restoreOnJoin(player);

        packetsAfterMs.put(id, System.currentTimeMillis());
        viewerReady.add(id);
        activate(player);
    }

    private void activate(Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline() || !nametags.containsKey(id)) {
            return;
        }

        packetsAfterMs.put(id, System.currentTimeMillis());

        Nametag own = nametags.get(id);
        if (own != null) {
            own.updateVisibility(Integer.MAX_VALUE / 4);
        }
        presentAllTagsToViewer(player);

        refreshVisibilityAfterPackets(player);

        FoliaTasks.runOnEntityDelayed(AnarchyTags.getInstance(), player, () -> {
            if (!player.isOnline()) {
                return;
            }
            tabOrder.beginFullSync(player);
            tabOrder.onPlayerJoinedTab(player);
        }, 15L);
    }

    /** Spawna todas as tags 3D visíveis para quem acabou de entrar. */
    public void presentAllTagsToViewer(Player viewer) {
        if (viewer == null || !viewer.isOnline() || !isViewerReady(viewer)) {
            return;
        }
        for (Nametag tag : nametags.values()) {
            Player owner = tag.getPlayer();
            if (owner == null || !owner.isOnline()) {
                continue;
            }
            if (owner.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), owner, () -> tag.presentTo(viewer));
        }
    }

    /**
     * Id do TextDisplay para injetar no SetPassengers do veículo, ou -1.
     */
    public int displayPassengerId(int vehicleEntityId, Player viewer) {
        UUID ownerId = ownerByEntityId.get(vehicleEntityId);
        if (ownerId == null || viewer == null) {
            return -1;
        }
        Nametag tag = nametags.get(ownerId);
        if (tag == null || !tag.isSpawnedForViewer(viewer)) {
            return -1;
        }
        return tag.getDisplayEntityId();
    }

    /** Reposiciona após o teleporte com delay seguro para o Folia carregar as chunks */
    public void afterPlayerTeleported(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!AnarchyTags.getInstance().config().getNametagConfig().isEnabled()) {
            return;
        }
        ownerByEntityId.put(player.getEntityId(), player.getUniqueId());

        // Espera 3 ticks para a chunk carregar no cliente
        FoliaTasks.runOnEntityDelayed(AnarchyTags.getInstance(), player, () -> {
            if (!player.isOnline()) return;

            Nametag own = get(player);
            if (own != null) {
                own.followAllViewers();
            }

            if (!isPacketsReady(player)) {
                return;
            }

            // Atualiza apenas jogadores no mesmo mundo em vez de disparar tarefas globais
            for (Player other : player.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                Nametag otherTag = get(other);
                if (otherTag != null) {
                    FoliaTasks.runOnEntity(AnarchyTags.getInstance(), other, () -> {
                        if (otherTag.canShowTo(player, (long) otherTag.getVisibilityDistance() * otherTag.getVisibilityDistance())) {
                            otherTag.show(player);
                        }
                    });
                }
            }
        }, 3L);
    }

    /** Garante que todos veem a tag de um jogador (ex.: após init). */
    public void presentPlayerToAllViewers(Player target) {
        Nametag tag = get(target);
        if (tag == null || !target.isOnline()) {
            return;
        }
        FoliaTasks.runOnEntity(AnarchyTags.getInstance(), target, () -> tag.updateVisibility(Integer.MAX_VALUE / 4));
    }

    /** Reaplica esconder nick após join (sem invalidar cache visual). */
    public void refreshVisibilityAfterPackets(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        NickHideMode mode = AnarchyTags.getInstance().getNickHideManager().getMode(player);
        if (mode == NickHideMode.OFF) {
            return;
        }
        Nametag own = nametags.get(player.getUniqueId());
        if (own != null) {
            if (mode == NickHideMode.FULL_HIDDEN) {
                own.hideForAll();
            }
            own.updateVisibility(32);
        }
        for (Nametag tag : nametags.values()) {
            Player owner = tag.getPlayer();
            if (owner == null || owner.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), owner, () -> tag.presentTo(player));
        }
    }

    public void discardNametag(Player player) {
        UUID id = player.getUniqueId();
        Nametag removed = nametags.remove(id);
        if (removed != null && player.isOnline()) {
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), player, removed::hideForAll);
        }
    }

    public void remove(Player player) {
        NametagHandler.clearTarget(player);
        tabOrder.forget(player.getUniqueId());
        UUID id = player.getUniqueId();
        ownerByEntityId.entrySet().removeIf(entry -> id.equals(entry.getValue()));
        Nametag nametag = nametags.remove(id);
        if (nametag != null) {
            nametag.hideForAll();
        }
        for (Nametag existing : nametags.values()) {
            existing.forgetViewer(id);
        }
    }

    public void refreshViewFor(Player viewer) {
        if (viewer == null || !viewer.isOnline() || !isPacketsReady(viewer)) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        for (Nametag nametag : nametags.values()) {
            if (!nametag.getViewers().contains(viewerId)) {
                continue;
            }
            Player owner = nametag.getPlayer();
            if (owner == null || !owner.isOnline()) {
                continue;
            }
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), owner, () -> nametag.refreshTextIfChanged(viewer));
        }
    }

    public void createAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleJoin(player);
        }
    }

    public void removeAll() {
        for (Player online : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            remove(online);
        }
        nametags.clear();
        tabOrder.reset();
        NametagHandler.reset();
        viewerReady.clear();
        packetsAfterMs.clear();
    }

    public boolean passesExternalVisibility(Player viewer, Player target) {
        return VisibilityFilterChain.passes(visibilityFilters, viewer, target);
    }

    @Override
    public void registerVisibilityFilter(@NotNull NametagVisibilityFilter filter) {
        visibilityFilters.add(filter);
    }

    @Override
    public void unregisterVisibilityFilter(@NotNull NametagVisibilityFilter filter) {
        visibilityFilters.remove(filter);
    }

    @Override
    public boolean isNametagVisibleFor(@NotNull Player viewer, @NotNull Player target) {
        Nametag tag = get(target);
        if (tag == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }
        long maxDistSq = (long) AnarchyTags.getInstance().config().getNametagConfig().getVisibilityDistance();
        maxDistSq *= maxDistSq;
        return tag.canShowTo(viewer, maxDistSq);
    }

    @Override
    public @NotNull NickHideMode getNickHideMode(@NotNull Player player) {
        return AnarchyTags.getInstance().getNickHideManager().getMode(player);
    }

    @Override
    public boolean isNickLineEnabled(@NotNull Player player) {
        return AnarchyTags.getInstance().getNickHideManager().isNickLineEnabled(player);
    }

    @Override
    public void refreshVisibilityFor(@NotNull Player target) {
        Nametag nametag = get(target);
        if (nametag == null || !target.isOnline()) {
            return;
        }
        FoliaTasks.runOnEntity(AnarchyTags.getInstance(), target, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }
                if (isNametagVisibleFor(viewer, target)) {
                    nametag.presentTo(viewer);
                } else if (nametag.getViewers().contains(viewer.getUniqueId())) {
                    nametag.hide(viewer);
                }
            }
            nametag.updateVisibility(32);
        });
    }

    @Override
    public void refreshTag(@NotNull Player target) {
        Nametag nametag = get(target);
        if (nametag != null) {
            FoliaTasks.runOnEntity(AnarchyTags.getInstance(), target, () -> {
                if (isViewerReady(target)) {
                    nametag.updateVisibility(16);
                    nametag.refreshAllViewerTextsIfChanged();
                }
            });
        }
    }

    @Override
    public void setCustomFormat(@NotNull Player player, @Nullable String format) {
        if (format == null || format.isBlank()) {
            customFormats.remove(player.getUniqueId());
        } else {
            customFormats.put(player.getUniqueId(), format);
        }
        refreshTag(player);
    }

    @Override
    public void reload() {
        AnarchyTags.getInstance().reloadPlugin();
    }
}
