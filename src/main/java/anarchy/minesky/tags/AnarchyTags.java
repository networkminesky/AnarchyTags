package anarchy.minesky.tags;

import anarchy.minesky.tags.api.AnarchyTagsAPI;
import anarchy.minesky.tags.command.AnarchyTagsCommand;
import anarchy.minesky.tags.command.EsconderNickCommand;
import anarchy.minesky.tags.config.AnarchyTagsConfig;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.hook.VoiceChatBridge;
import anarchy.minesky.tags.hook.VoicePlaceholderHook;
import anarchy.minesky.tags.listener.PlayerListener;
import anarchy.minesky.tags.listener.VanishListener;
import anarchy.minesky.tags.nick.NickHideManager;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.nametags.NametagScheduler;
import anarchy.minesky.tags.nametags.PassengerMountListener;
import anarchy.minesky.tags.nametags.PlayerConfigPreferenceWatcher;
import anarchy.minesky.tags.scoreboard.ScoreboardManager;
import anarchy.minesky.tags.scoreboard.ScoreboardUpdateTask;
import anarchy.minesky.tags.sorting.TabOrder;
import anarchy.minesky.tags.utils.helpers.DependencyHelper;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class AnarchyTags extends JavaPlugin {

    private static AnarchyTags instance;

    private AnarchyTagsConfig config;
    private TabOrder tabOrder;
    private NametagManager nametagManager;
    private NametagScheduler nametagScheduler;
    private PlayerConfigPreferenceWatcher preferenceWatcher;
    private ScoreboardManager scoreboardManager;
    private ScoreboardUpdateTask scoreboardUpdateTask;
    private NickHideManager nickHideManager;
    private PassengerMountListener passengerMountListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        config = new AnarchyTagsConfig(this);
        config.load();

        tabOrder = new TabOrder(this);
        nametagManager = new NametagManager(tabOrder);
        nametagScheduler = new NametagScheduler(this);
        preferenceWatcher = new PlayerConfigPreferenceWatcher(this, nametagManager);
        scoreboardManager = new ScoreboardManager(this);
        scoreboardUpdateTask = new ScoreboardUpdateTask(this, scoreboardManager);
        nickHideManager = new NickHideManager(this);
        passengerMountListener = new PassengerMountListener(nametagManager);
        PacketEvents.getAPI().getEventManager().registerListener(passengerMountListener);
        VoicePlaceholderHook.register();
        nametagScheduler.start();
        preferenceWatcher.start();
        scoreboardUpdateTask.start();

        getServer().getServicesManager().register(
                AnarchyTagsAPI.class,
                nametagManager,
                this,
                ServicePriority.Normal
        );

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);

        TagsPlayerConfig.register(this, config.getNametagConfig(), config.getScoreboardConfig());

        AnarchyTagsCommand command = new AnarchyTagsCommand(this);
        if (getCommand("anarchytags") != null) {
            getCommand("anarchytags").setExecutor(command);
            getCommand("anarchytags").setTabCompleter(command);
        }

        EsconderNickCommand esconderNick = new EsconderNickCommand(this);
        if (getCommand("escondernick") != null) {
            getCommand("escondernick").setExecutor(esconderNick);
            getCommand("escondernick").setTabCompleter(esconderNick);
        }

        DependencyHelper.load();
        VoiceChatBridge.scheduleRegister(this);

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                String name = event.getPlugin().getName();
                if ("PlaceholderAPI".equals(name)) {
                    VoicePlaceholderHook.register();
                }
                if ("voicechat".equalsIgnoreCase(name) || "VoiceChatPlaceholders".equalsIgnoreCase(name)) {
                    VoiceChatBridge.tryRegister(AnarchyTags.this);
                }
                if ("PlaceholderAPI".equals(name)
                        || "AnarchyEconomy".equals(name)
                        || "AnarchyClans".equals(name)
                        || "LuckPerms".equals(name)) {
                    for (var nametag : nametagManager.getAll()) {
                        nametag.refreshAllViewerTextsIfChanged();
                    }
                    tabOrder.recomputeChangedAsync();
                    scoreboardManager.refreshAll();
                }
            }
        }, this);

        getLogger().info("AnarchyTags v" + getPluginMeta().getVersion() + " (DisplayTags-style / Folia 26.3) ativado.");
    }

    @Override
    public void onDisable() {
        if (nametagScheduler != null) {
            nametagScheduler.stop();
        }
        if (preferenceWatcher != null) {
            preferenceWatcher.stop();
        }
        if (scoreboardUpdateTask != null) {
            scoreboardUpdateTask.stop();
        }
        if (scoreboardManager != null) {
            scoreboardManager.clearAll();
        }
        if (nametagManager != null) {
            nametagManager.removeAll();
        }
        if (passengerMountListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(passengerMountListener);
            passengerMountListener = null;
        }
        VoiceChatBridge.shutdown();
        VoicePlaceholderHook.unregister();
        getLogger().info("AnarchyTags desativado.");
    }

    public boolean reloadPlugin() {
        try {
            reloadConfig();
            config.reload();
            TagsPlayerConfig.register(this, config.getNametagConfig(), config.getScoreboardConfig());
            nametagScheduler.stop();
            preferenceWatcher.stop();
            scoreboardUpdateTask.stop();
            scoreboardManager.clearAll();
            nametagManager.removeAll();
            if (config.getNametagConfig().isEnabled()) {
                nametagManager.createAll();
                nametagScheduler.start();
                preferenceWatcher.start();
            }
            scoreboardUpdateTask.start();
            scoreboardManager.refreshAll();
            VoiceChatBridge.refreshAllVoiceIcons();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Falha ao recarregar AnarchyTags: " + e.getMessage(), e);
            return false;
        }
    }

    public static AnarchyTags getInstance() {
        return instance;
    }

    public static AnarchyTagsAPI getAPI() {
        return instance != null ? instance.nametagManager : null;
    }

    public AnarchyTagsConfig config() {
        return config;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public PlayerConfigPreferenceWatcher getPreferenceWatcher() {
        return preferenceWatcher;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ScoreboardUpdateTask getScoreboardUpdateTask() {
        return scoreboardUpdateTask;
    }

    public NickHideManager getNickHideManager() {
        return nickHideManager;
    }
}
