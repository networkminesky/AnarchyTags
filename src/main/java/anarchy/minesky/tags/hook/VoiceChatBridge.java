package anarchy.minesky.tags.hook;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.config.NametagConfig;
import anarchy.minesky.tags.nametags.Nametag;
import anarchy.minesky.tags.util.FoliaTasks;
import anarchy.minesky.tags.utils.helpers.PlaceholderHelper;
import anarchy.minesky.tags.voice.VoiceIconService;
import anarchy.minesky.tags.voice.VoiceIconState;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerStateChangedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoiceDistanceEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoiceChatBridge implements VoicechatPlugin {

    private static VoiceChatBridge instance;
    private static final AtomicBoolean registered = new AtomicBoolean(false);
    private static ScheduledTask registerTask;

    private final AnarchyTags plugin;
    private volatile VoicechatServerApi serverApi;
    private final Map<UUID, Long> talkingUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> whispering = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastIconKey = new ConcurrentHashMap<>();
    private final AtomicBoolean voiceEventsRegistered = new AtomicBoolean(false);

    private VoiceChatBridge(AnarchyTags plugin) {
        this.plugin = plugin;
    }

    public static void shutdown() {
        registered.set(false);
        instance = null;
        if (registerTask != null) {
            registerTask.cancel();
            registerTask = null;
        }
    }

    public static void scheduleRegister(AnarchyTags plugin) {
        tryRegister(plugin);
        if (registerTask != null) {
            return;
        }
        registerTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (!Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
                return;
            }
            tryRegister(plugin);
            if (instance != null) {
                instance.ensureServerApi();
            }
            if (registered.get() && instance != null && instance.serverApi != null) {
                task.cancel();
                registerTask = null;
            }
        }, 40L, 40L);
    }

    public static void tryRegister(AnarchyTags plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
            return;
        }
        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            return;
        }
        if (instance == null) {
            instance = new VoiceChatBridge(plugin);
        }
        if (registered.get()) {
            return;
        }
        service.registerPlugin(instance);
        registered.set(true);
        plugin.getLogger().info("Simple Voice Chat API registrada — {voiceicon} nas linhas da tag.");
        instance.ensureServerApi();
    }

    void ensureServerApi() {
        if (serverApi != null) {
            return;
        }
        VoicechatServerApi resolved = VoiceChatServerAccess.tryResolve();
        if (resolved != null) {
            serverApi = resolved;
            VoiceChatServerAccess.logLateBind(plugin, resolved);
            refreshAllVoiceIcons();
        }
    }

    public static void onPlayerJoin(Player player) {
        if (instance == null || player == null) {
            return;
        }
        instance.ensureServerApi();
        FoliaTasks.runOnEntityDelayed(instance.plugin, player, () -> {
            if (player.isOnline()) {
                instance.refreshPlayerVoiceIcon(player.getUniqueId());
            }
        }, 8L);
        FoliaTasks.runOnEntityDelayed(instance.plugin, player, () -> {
            if (player.isOnline()) {
                instance.refreshPlayerVoiceIcon(player.getUniqueId());
            }
        }, 40L);
    }

    public static void refreshAllVoiceIcons() {
        if (instance == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            instance.refreshPlayerVoiceIcon(online.getUniqueId());
        }
    }

    private void refreshPlayerVoiceIcon(UUID playerId) {
        lastIconKey.remove(playerId);
        notifyIfIconChanged(playerId);
    }

    public static void ensureReady(AnarchyTags plugin) {
        if (instance != null) {
            instance.ensureServerApi();
        } else if (Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
            tryRegister(plugin);
        }
    }

    public static boolean isApiReady() {
        return instance != null && instance.serverApi != null;
    }

    public static boolean isAddonRegistered() {
        return registered.get();
    }

    public static boolean isTalking(UUID playerId) {
        return instance != null && playerId != null && instance.isTalkingNow(playerId);
    }

    public static String debugLine(Player target) {
        if (target == null) {
            return "sem jogador";
        }
        NametagConfig config = AnarchyTags.getInstance().config().getNametagConfig();
        if (!config.isVoiceChatEnabled()) {
            return "desligado na config";
        }
        String icon = VoiceIconService.iconFor(null, target);
        VoiceIconState state = resolveState(target);
        return "registrado=" + isAddonRegistered()
                + " | api=" + isApiReady()
                + " | eventos=" + (instance != null && instance.voiceEventsRegistered.get())
                + " | falando=" + isTalking(target.getUniqueId())
                + " | estado=" + state
                + " | icone=\"" + icon + "\""
                + " | con=" + connectionDebug(target);
    }

    private static String connectionDebug(Player target) {
        if (instance == null || instance.serverApi == null || target == null) {
            return "sem-api";
        }
        VoicechatConnection connection = instance.serverApi.getConnectionOf(target.getUniqueId());
        if (connection == null) {
            return "null";
        }
        return "inst=" + connection.isInstalled()
                + ",conn=" + connection.isConnected()
                + ",off=" + connection.isDisabled();
    }

    public static VoiceIconState resolveState(Player target) {
        if (target == null) {
            return VoiceIconState.QUIET;
        }
        if (instance != null) {
            instance.ensureServerApi();
        } else {
            ensureReady(AnarchyTags.getInstance());
        }
        VoicechatServerApi api = instance != null ? instance.serverApi : null;
        if (api == null) {
            return VoiceIconState.NOT_INSTALLED;
        }
        VoicechatConnection connection = api.getConnectionOf(target.getUniqueId());
        if (connection == null || !connection.isInstalled()) {
            return VoiceIconState.NOT_INSTALLED;
        }
        if (!connection.isConnected()) {
            return VoiceIconState.NOT_INSTALLED;
        }
        if (connection.isDisabled()) {
            if (instance != null) {
                instance.clearTalking(target.getUniqueId());
            }
            return VoiceIconState.DISABLED;
        }
        if (instance != null && instance.isTalkingNow(target.getUniqueId())) {
            return instance.whispering.getOrDefault(target.getUniqueId(), false)
                    ? VoiceIconState.WHISPERING
                    : VoiceIconState.TALKING;
        }
        return VoiceIconState.QUIET;
    }

    public static String iconFor(Player viewer, Player target) {
        if (target == null) {
            return "";
        }
        NametagConfig config = AnarchyTags.getInstance().config().getNametagConfig();
        if (!config.isVoiceChatEnabled()) {
            return "";
        }
        if (config.isVoiceChatUsePlaceholder()) {
            String external = resolveExternalPlaceholder(target, config.getVoiceChatPlaceholder());
            if (external != null) {
                return external;
            }
        }
        return VoiceIconService.iconFor(viewer, target);
    }

    private static String resolveExternalPlaceholder(Player target, String placeholder) {
        if (placeholder == null || placeholder.isBlank() || !PlaceholderHelper.isAvailable()) {
            return null;
        }
        String resolved = PlaceholderHelper.applyTarget(target, placeholder.trim());
        if (resolved == null || resolved.equals(placeholder.trim()) || resolved.contains("%")) {
            return null;
        }
        return resolved;
    }

    public static void clearPlayer(UUID playerId) {
        if (instance == null || playerId == null) {
            return;
        }
        instance.clearTalking(playerId);
        instance.lastIconKey.remove(playerId);
    }

    private void clearTalking(UUID playerId) {
        talkingUntil.remove(playerId);
        whispering.remove(playerId);
    }

    private boolean voiceTransmitAllowed(UUID playerId) {
        if (serverApi == null) {
            return false;
        }
        VoicechatConnection connection = serverApi.getConnectionOf(playerId);
        return connection != null
                && connection.isInstalled()
                && connection.isConnected()
                && !connection.isDisabled();
    }

    private static boolean hasVoicePayload(byte[] opus) {
        return opus != null && opus.length > 0;
    }

    @Override
    public String getPluginId() {
        return "anarchytags";
    }

    @Override
    public void initialize(VoicechatApi api) {
        plugin.getLogger().info("VoiceChat addon initialize: "
                + (api == null ? "null" : api.getClass().getSimpleName()));
        if (api instanceof VoicechatServerApi server) {
            serverApi = server;
            plugin.getLogger().info("Simple Voice Chat server API ativa.");
            refreshAllVoiceIcons();
        } else {
            ensureServerApi();
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        voiceEventsRegistered.set(true);
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            serverApi = event.getVoicechat();
            plugin.getLogger().info("Simple Voice Chat server iniciado (evento API).");
            refreshAllVoiceIcons();
        });
        registration.registerEvent(MicrophonePacketEvent.class, event -> {
            if (!hasVoicePayload(event.getPacket().getOpusEncodedData())) {
                return;
            }
            onVoiceActivity(event.getSenderConnection(), event.getPacket().isWhispering());
        });
        registration.registerEvent(VoiceDistanceEvent.class, event -> {
            if (!hasVoicePayload(event.getPacket().getOpusEncodedData())) {
                return;
            }
            onVoiceActivity(event.getSenderConnection(), event.getPacket().isWhispering());
        });
        registration.registerEvent(PlayerStateChangedEvent.class, event -> {
            UUID playerId = event.getPlayerUuid();
            if (event.isDisabled() || event.isDisconnected()) {
                clearTalking(playerId);
            }
            notifyIfIconChanged(playerId);
        });
    }

    private void onVoiceActivity(VoicechatConnection sender, boolean whisper) {
        if (sender == null || sender.isDisabled()) {
            return;
        }
        onVoiceActivity(sender.getPlayer().getUuid(), whisper);
    }

    private void onVoiceActivity(UUID id, boolean whisper) {
        if (id == null || !voiceTransmitAllowed(id)) {
            return;
        }
        NametagConfig config = plugin.config().getNametagConfig();
        long until = System.currentTimeMillis() + config.getVoiceChatTalkTimeoutMs();
        boolean wasTalking = isTalkingNow(id);
        talkingUntil.put(id, until);
        whispering.put(id, whisper);
        if (!wasTalking) {
            notifyIfIconChanged(id);
        }
        scheduleTalkEndRefresh(id, config.getVoiceChatTalkTimeoutMs());
    }

    private boolean isTalkingNow(UUID id) {
        Long until = talkingUntil.get(id);
        return until != null && until >= System.currentTimeMillis();
    }

    private void scheduleTalkEndRefresh(UUID playerId, int timeoutMs) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        long ticks = Math.max(2L, timeoutMs / 50L);
        FoliaTasks.runOnEntityDelayed(plugin, player, () -> {
            if (!isTalkingNow(playerId)) {
                notifyIfIconChanged(playerId);
            }
        }, ticks);
    }

    private void notifyIfIconChanged(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        String key = iconFor(null, player);
        String previous = lastIconKey.put(playerId, key);
        if (Objects.equals(previous, key)) {
            return;
        }
        Nametag nametag = plugin.getNametagManager().get(player);
        if (nametag == null) {
            return;
        }
        FoliaTasks.runOnEntity(plugin, player, nametag::forceRefreshAllViewerTexts);
    }
}
