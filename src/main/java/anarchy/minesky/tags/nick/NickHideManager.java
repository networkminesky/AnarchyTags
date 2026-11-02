package anarchy.minesky.tags.nick;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.util.FoliaTasks;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NickHideManager {

    public static final String PERM_OBFUSCATED = "anarchytags.nick.ofuscado";
    public static final String PERM_OBFUSCATED_VIEW = "anarchytags.nick.ofuscado.view";
    public static final String PERM_INVISIBLE = "anarchytags.nick.invisible";
    public static final String PERM_INVISIBLE_OTHERS = "anarchytags.nick.invisible.others";

    private static final String STEVE_TEXTURE =
            "eyJ0aW1c3RhbXAiOjAsInByb2ZpbGVJZCI6Ijg2NjdiYTcxLWI4NWEtNDAwNC1hZjAzLTQ1OTkzOGQ1MzEzZSIsInByb2ZpbGVOYW1lIjoiU3RldmUifQ==";

    private final AnarchyTags plugin;
    private final NickHideStorage storage;
    private final Map<UUID, NickHideMode> active = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> nickLineEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> savedProfiles = new ConcurrentHashMap<>();

    public NickHideManager(AnarchyTags plugin) {
        this.plugin = plugin;
        this.storage = new NickHideStorage(plugin);
        storage.load();
    }

    public NickHideMode getMode(Player player) {
        if (player == null) {
            return NickHideMode.OFF;
        }
        return active.getOrDefault(player.getUniqueId(), NickHideMode.OFF);
    }

    public NickHideMode getMode(UUID playerId) {
        if (playerId == null) {
            return NickHideMode.OFF;
        }
        return active.getOrDefault(playerId, NickHideMode.OFF);
    }

    public boolean isNickLineEnabled(Player player) {
        if (player == null) {
            return true;
        }
        return nickLineEnabled.getOrDefault(player.getUniqueId(), true);
    }

    public boolean isHidden(Player player) {
        return getMode(player) != NickHideMode.OFF;
    }

    public boolean canSeeHiddenNametag(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }
        return getMode(target) != NickHideMode.FULL_HIDDEN;
    }

    public void restoreOnJoin(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        NickHideMode stored = storage.readMode(id);
        if (stored == NickHideMode.FULL_HIDDEN && !player.hasPermission(PERM_INVISIBLE)) {
            stored = NickHideMode.OFF;
            storage.writeMode(id, NickHideMode.OFF);
        }
        boolean nickLine = storage.readNickLineEnabled(id);
        nickLineEnabled.put(id, nickLine);
        if (stored == NickHideMode.OFF) {
            active.remove(id);
        } else {
            active.put(id, stored);
        }
        if (stored != NickHideMode.OFF || !nickLine) {
            FoliaTasks.runOnEntity(plugin, player, () -> applyVisualState(player, false));
        }
    }

    public boolean toggleObfuscated(Player player) {
        if (player == null || !player.isOnline() || !player.hasPermission(PERM_OBFUSCATED)) {
            return false;
        }
        NickHideMode current = getMode(player);
        if (current == NickHideMode.FULL_HIDDEN) {
            return false;
        }
        NickHideMode next = current == NickHideMode.OBFUSCATED ? NickHideMode.OFF : NickHideMode.OBFUSCATED;
        return setMode(player, next);
    }

    public boolean toggleNickLine(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        boolean next = !isNickLineEnabled(player);
        nickLineEnabled.put(player.getUniqueId(), next);
        storage.writeNickLineEnabled(player.getUniqueId(), next);
        FoliaTasks.runOnEntity(plugin, player, () -> applyVisualState(player, true));
        return next;
    }

    public boolean toggleInvisible(Player actor, Player target) {
        if (actor == null || target == null || !target.isOnline()) {
            return false;
        }
        if (!actor.getUniqueId().equals(target.getUniqueId())) {
            if (!actor.hasPermission(PERM_INVISIBLE_OTHERS)) {
                return false;
            }
        } else if (!actor.hasPermission(PERM_INVISIBLE)) {
            return false;
        }
        NickHideMode current = getMode(target);
        NickHideMode next = current == NickHideMode.FULL_HIDDEN ? NickHideMode.OFF : NickHideMode.FULL_HIDDEN;
        return setMode(target, next);
    }

    public boolean setMode(Player player, NickHideMode mode) {
        if (player == null || !player.isOnline() || mode == null) {
            return false;
        }
        if (mode == NickHideMode.FULL_HIDDEN && !player.hasPermission(PERM_INVISIBLE)) {
            return false;
        }
        if (mode == NickHideMode.OFF) {
            active.remove(player.getUniqueId());
            storage.writeMode(player.getUniqueId(), NickHideMode.OFF);
        } else {
            active.put(player.getUniqueId(), mode);
            storage.writeMode(player.getUniqueId(), mode);
        }
        FoliaTasks.runOnEntity(plugin, player, () -> applyVisualState(player, true));
        return true;
    }

    private void applyVisualState(Player player, boolean refreshOthers) {
        NickHideMode mode = getMode(player);
        applySkinForMode(player, mode);

        NametagManager manager = plugin.getNametagManager();
        Nametag own = manager.get(player);

        if (own != null) {
            if (mode == NickHideMode.FULL_HIDDEN) {
                own.hideForAll();
            } else if (manager.isPacketsReady(player)) {
                own.updateVisibility(24);
            }
            if (manager.isPacketsReady(player)) {
                own.invalidateVisuals();
                own.refreshAllViewerTextsIfChanged();
            }
        }

        if (refreshOthers) {
            for (Nametag tag : manager.getAll()) {
                Player owner = tag.getPlayer();
                if (owner == null || owner.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (!manager.isPacketsReady(player)) {
                    continue;
                }
                FoliaTasks.runOnEntity(plugin, owner, () -> {
                    if (tag.getViewers().contains(player.getUniqueId())) {
                        tag.invalidateVisuals();
                        tag.refreshTextIfChanged(player);
                    } else {
                        tag.presentTo(player);
                    }
                });
            }
        }

        manager.tabOrder().recompute(player);
        manager.refreshVisibilityFor(player);
    }

    private void applySkinForMode(Player player, NickHideMode mode) {
        if (mode == NickHideMode.OFF) {
            restoreSkin(player);
        } else if (mode == NickHideMode.OBFUSCATED) {
            savedProfiles.putIfAbsent(player.getUniqueId(), copyProfile(player.getPlayerProfile()));
            applySteveSkin(player);
        }
    }

    public void onQuit(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        savedProfiles.remove(id);
        active.remove(id);
        nickLineEnabled.remove(id);
    }

    private void restoreSkin(Player player) {
        PlayerProfile saved = savedProfiles.remove(player.getUniqueId());
        if (saved != null) {
            player.setPlayerProfile(copyProfile(saved));
        }
    }

    private static void applySteveSkin(Player player) {
        PlayerProfile profile = Bukkit.createProfile(player.getUniqueId(), player.getName());
        profile.setProperty(new ProfileProperty("textures", STEVE_TEXTURE));
        player.setPlayerProfile(profile);
    }

    private static PlayerProfile copyProfile(PlayerProfile source) {
        PlayerProfile copy = Bukkit.createProfile(source.getUniqueId(), source.getName());
        Collection<ProfileProperty> properties = source.getProperties();
        if (properties != null) {
            for (ProfileProperty property : properties) {
                copy.setProperty(property);
            }
        }
        return copy;
    }
}
