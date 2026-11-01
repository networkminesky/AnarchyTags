package anarchy.minesky.tags.nick;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.util.FoliaTasks;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NickHideManager {

    public static final String PERM_USE = "anarchytags.escondernick";
    public static final String PERM_FULL = "anarchytags.escondernick.invisivel";
    public static final String PERM_SEE_HINT = "anarchytags.escondernick.ver";

    private static final String STEVE_TEXTURE = "eyJ0aW1c3RhbXAiOjAsInByb2ZpbGVJZCI6Ijg2NjdiYTcxLWI4NWEtNDAwNC1hZjAzLTQ1OTkzOGQ1MzEzZSIsInByb2ZpbGVOYW1lIjoiU3RldmUifQ==";

    private final AnarchyTags plugin;
    private final NickHideStorage storage;
    private final Map<UUID, NickHideMode> active = new ConcurrentHashMap<>();
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

    /** Compatibilidade com debug / checks antigos. */
    public boolean isHidden(Player player) {
        return getMode(player) != NickHideMode.OFF;
    }

    public boolean isHidden(UUID playerId) {
        return getMode(playerId) != NickHideMode.OFF;
    }

    public boolean canSeeHiddenNametag(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }
        NickHideMode mode = getMode(target);
        return switch (mode) {
            case OFF, OBFUSCATED -> true;
            case ADMIN_HINT -> viewer.hasPermission(PERM_SEE_HINT);
            case FULL_HIDDEN -> false;
        };
    }

    public boolean viewerSeesAdminHint(Player viewer) {
        return viewer != null && viewer.hasPermission(PERM_SEE_HINT);
    }

    /** Chamado no join, antes de liberar pacotes. */
    public void restoreOnJoin(Player player) {
        if (player == null) {
            return;
        }
        NickHideMode stored = storage.read(player.getUniqueId());
        if (stored == NickHideMode.FULL_HIDDEN && !player.hasPermission(PERM_FULL)) {
            stored = NickHideMode.OBFUSCATED;
            storage.write(player.getUniqueId(), stored);
        }
        if (stored == NickHideMode.OFF) {
            active.remove(player.getUniqueId());
            return;
        }
        final NickHideMode mode = stored;
        active.put(player.getUniqueId(), mode);
        FoliaTasks.runOnEntity(plugin, player, () -> applyMode(player, mode, false));
    }

    public NickHideMode cycleMode(Player player) {
        if (player == null || !player.isOnline()) {
            return NickHideMode.OFF;
        }
        NickHideMode next = nextMode(player, getMode(player));
        setMode(player, next);
        return next;
    }

    public boolean setMode(Player player, NickHideMode mode) {
        if (player == null || !player.isOnline() || mode == null) {
            return false;
        }
        if (mode == NickHideMode.FULL_HIDDEN && !player.hasPermission(PERM_FULL)) {
            return false;
        }
        if (mode == NickHideMode.OFF) {
            active.remove(player.getUniqueId());
            storage.write(player.getUniqueId(), NickHideMode.OFF);
            FoliaTasks.runOnEntity(plugin, player, () -> applyMode(player, NickHideMode.OFF, true));
            return true;
        }
        active.put(player.getUniqueId(), mode);
        storage.write(player.getUniqueId(), mode);
        FoliaTasks.runOnEntity(plugin, player, () -> applyMode(player, mode, true));
        return true;
    }

    private NickHideMode nextMode(Player player, NickHideMode current) {
        boolean full = player.hasPermission(PERM_FULL);
        return switch (current) {
            case OFF -> NickHideMode.OBFUSCATED;
            case OBFUSCATED -> NickHideMode.ADMIN_HINT;
            case ADMIN_HINT -> full ? NickHideMode.FULL_HIDDEN : NickHideMode.OFF;
            case FULL_HIDDEN -> NickHideMode.OFF;
        };
    }

    private void applyMode(Player player, NickHideMode mode, boolean refreshOthers) {
        if (mode == NickHideMode.OFF) {
            restoreSkin(player);
        } else {
            savedProfiles.putIfAbsent(player.getUniqueId(), copyProfile(player.getPlayerProfile()));
            applySteveSkin(player);
        }

        NametagManager manager = plugin.getNametagManager();
        Nametag own = manager.get(player);
        if (own != null) {
            if (mode == NickHideMode.FULL_HIDDEN || mode == NickHideMode.ADMIN_HINT) {
                own.hideForAll();
            }
            if (manager.isPacketsReady(player)) {
                own.updateVisibility(16);
                own.refreshAllViewerTextsIfChanged();
            }
        }

        if (refreshOthers) {
            for (Nametag tag : manager.getAll()) {
                Player owner = tag.getPlayer();
                if (owner == null || owner.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (manager.isPacketsReady(player)) {
                    FoliaTasks.runOnEntity(plugin, owner, () -> {
                        if (tag.getViewers().contains(player.getUniqueId())) {
                            tag.refreshTextIfChanged(player);
                        }
                    });
                }
            }
        }

        manager.tabOrder().recompute(player);
    }

    /** Só limpa memória de sessão; mantém nick-hide.yml. */
    public void onQuit(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        savedProfiles.remove(id);
        active.remove(id);
    }

    private void restoreSkin(Player player) {
        PlayerProfile saved = savedProfiles.remove(player.getUniqueId());
        if (saved != null) {
            player.setPlayerProfile(copyProfile(saved));
        } else {
            player.setPlayerProfile(Bukkit.createProfile(player.getUniqueId(), player.getName()));
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

    public static NamedTextColor adminHintColor() {
        return NamedTextColor.DARK_GRAY;
    }
}
