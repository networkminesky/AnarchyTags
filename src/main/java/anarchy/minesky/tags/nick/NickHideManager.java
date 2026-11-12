package anarchy.minesky.tags.nick;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NickHideConfig;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.util.FoliaTasks;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
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
    private final Map<UUID, QuotaRuntime> quotas = new ConcurrentHashMap<>();
    private ScheduledTask quotaTask;
    private int persistTicks;

    public NickHideManager(AnarchyTags plugin) {
        this.plugin = plugin;
        this.storage = new NickHideStorage(plugin);
        storage.load();
    }

    public void start() {
        stop();
        persistTicks = 0;
        quotaTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduled -> tickQuotas(), 20L, 20L);
    }

    public void stop() {
        if (quotaTask != null) {
            quotaTask.cancel();
            quotaTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getMode(player) == NickHideMode.OBFUSCATED) {
                endObfuscatedSession(player, true);
            } else {
                flushQuota(player.getUniqueId(), true);
            }
        }
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

        loadQuota(player);
        if (stored == NickHideMode.OBFUSCATED && remainingMillis(player) <= 0) {
            stored = NickHideMode.OFF;
            storage.writeMode(id, NickHideMode.OFF);
        }

        if (stored == NickHideMode.OFF) {
            active.remove(id);
        } else {
            active.put(id, stored);
        }
        if (stored == NickHideMode.OBFUSCATED) {
            beginObfuscatedSession(player);
        }
        if (stored != NickHideMode.OFF || !nickLine) {
            FoliaTasks.runOnEntity(plugin, player, () -> applyVisualState(player, false));
        }
    }

    public NickHideToggleResult toggleObfuscated(Player player) {
        if (player == null || !player.isOnline() || !player.hasPermission(PERM_OBFUSCATED)) {
            return NickHideToggleResult.DENIED;
        }
        NickHideMode current = getMode(player);
        if (current == NickHideMode.FULL_HIDDEN) {
            return NickHideToggleResult.FULL_HIDDEN;
        }
        if (current == NickHideMode.OBFUSCATED) {
            return setMode(player, NickHideMode.OFF) ? NickHideToggleResult.DISABLED : NickHideToggleResult.DENIED;
        }
        loadQuota(player);
        if (remainingMillis(player) <= 0) {
            return NickHideToggleResult.QUOTA_EXHAUSTED;
        }
        if (!setMode(player, NickHideMode.OBFUSCATED)) {
            return NickHideToggleResult.DENIED;
        }
        maybeWarn(player);
        return NickHideToggleResult.ENABLED;
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
        NickHideMode previous = getMode(player);
        if (previous == NickHideMode.OBFUSCATED && mode != NickHideMode.OBFUSCATED) {
            endObfuscatedSession(player, true);
        }
        if (mode == NickHideMode.OFF) {
            active.remove(player.getUniqueId());
            storage.writeMode(player.getUniqueId(), NickHideMode.OFF);
        } else {
            active.put(player.getUniqueId(), mode);
            storage.writeMode(player.getUniqueId(), mode);
        }
        if (mode == NickHideMode.OBFUSCATED) {
            beginObfuscatedSession(player);
        }
        FoliaTasks.runOnEntity(plugin, player, () -> applyVisualState(player, true));
        return true;
    }

    public long remainingMillis(Player player) {
        if (player == null) {
            return 0L;
        }
        NickHideConfig config = plugin.config().getNickHideConfig();
        long quota = config.durationMillisFor(player);
        if (config.isUnlimited(quota)) {
            return quota;
        }
        QuotaRuntime runtime = quotas.get(player.getUniqueId());
        long used = runtime == null ? 0L : runtime.usedIncludingSession();
        return Math.max(0L, quota - used);
    }

    public boolean isUnlimited(Player player) {
        NickHideConfig config = plugin.config().getNickHideConfig();
        return config.isUnlimited(config.durationMillisFor(player));
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "min " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "min " + seconds + "s";
        }
        return seconds + "s";
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
                        tag.invalidateVisual(player.getUniqueId());
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
        if (getMode(player) == NickHideMode.OBFUSCATED) {
            endObfuscatedSession(player, true);
        } else {
            flushQuota(id, true);
        }
        savedProfiles.remove(id);
        active.remove(id);
        nickLineEnabled.remove(id);
        quotas.remove(id);
    }

    private void tickQuotas() {
        persistTicks++;
        boolean persist = persistTicks >= 10;
        if (persist) {
            persistTicks = 0;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (getMode(player) != NickHideMode.OBFUSCATED) {
                continue;
            }
            FoliaTasks.runOnEntity(plugin, player, () -> tickObfuscated(player, persist));
        }
    }

    private void tickObfuscated(Player player, boolean persist) {
        if (!player.isOnline() || getMode(player) != NickHideMode.OBFUSCATED) {
            return;
        }
        loadQuota(player);
        long remaining = remainingMillis(player);
        if (remaining <= 0) {
            expireObfuscated(player);
            return;
        }
        maybeWarn(player);
        if (persist) {
            flushQuota(player.getUniqueId(), false);
        }
    }

    private void expireObfuscated(Player player) {
        setMode(player, NickHideMode.OFF);
        player.sendMessage(Component.text("O tempo de nick ofuscado acabou por hoje.", NamedTextColor.RED));
    }

    private void maybeWarn(Player player) {
        NickHideConfig config = plugin.config().getNickHideConfig();
        long warnAt = config.getWarnRemainingMillis();
        if (warnAt <= 0 || isUnlimited(player)) {
            return;
        }
        QuotaRuntime runtime = quotas.get(player.getUniqueId());
        if (runtime == null || runtime.warned) {
            return;
        }
        long remaining = remainingMillis(player);
        if (remaining > warnAt) {
            return;
        }
        runtime.warned = true;
        flushQuota(player.getUniqueId(), true);
        player.sendMessage(Component.text(
                "Atenção: faltam " + formatDuration(remaining) + " de nick ofuscado hoje.",
                NamedTextColor.GOLD
        ));
    }

    private void beginObfuscatedSession(Player player) {
        loadQuota(player);
        QuotaRuntime runtime = quotas.get(player.getUniqueId());
        if (runtime == null) {
            return;
        }
        if (runtime.sessionStart == 0L) {
            runtime.sessionStart = System.currentTimeMillis();
        }
    }

    private void endObfuscatedSession(Player player, boolean persist) {
        QuotaRuntime runtime = quotas.get(player.getUniqueId());
        if (runtime == null) {
            return;
        }
        runtime.usedMillis = runtime.usedIncludingSession();
        runtime.sessionStart = 0L;
        if (persist) {
            flushQuota(player.getUniqueId(), true);
        }
    }

    private void loadQuota(Player player) {
        UUID id = player.getUniqueId();
        String today = LocalDate.now().toString();
        QuotaRuntime runtime = quotas.get(id);
        if (runtime == null) {
            NickHideStorage.QuotaSnapshot snapshot = storage.readQuota(id);
            runtime = new QuotaRuntime();
            runtime.day = snapshot.day();
            runtime.usedMillis = snapshot.usedMillis();
            runtime.warned = snapshot.warned();
            quotas.put(id, runtime);
        }
        if (!today.equals(runtime.day)) {
            runtime.day = today;
            runtime.usedMillis = 0L;
            runtime.warned = false;
            if (runtime.sessionStart != 0L) {
                runtime.sessionStart = System.currentTimeMillis();
            }
            flushQuota(id, true);
        }
    }

    private void flushQuota(UUID id, boolean forceWrite) {
        QuotaRuntime runtime = quotas.get(id);
        if (runtime == null) {
            return;
        }
        long used = runtime.usedIncludingSession();
        NickHideStorage.QuotaSnapshot snapshot = new NickHideStorage.QuotaSnapshot(runtime.day, used, runtime.warned);
        if (!forceWrite && snapshot.isEmpty()) {
            return;
        }
        storage.writeQuota(id, snapshot);
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

    private static final class QuotaRuntime {
        String day = "";
        long usedMillis;
        boolean warned;
        long sessionStart;

        long usedIncludingSession() {
            if (sessionStart <= 0L) {
                return usedMillis;
            }
            return usedMillis + Math.max(0L, System.currentTimeMillis() - sessionStart);
        }
    }
}
