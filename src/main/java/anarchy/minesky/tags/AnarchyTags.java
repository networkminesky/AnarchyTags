package anarchy.minesky.tags;

import anarchy.minesky.tags.api.AnarchyTagsAPI;
import anarchy.minesky.tags.command.AnarchyTagsCommand;
import anarchy.minesky.tags.config.AnarchyTagsConfig;
import anarchy.minesky.tags.hook.TagsPlayerConfig;
import anarchy.minesky.tags.listener.PlayerListener;
import anarchy.minesky.tags.listener.VanishListener;
import anarchy.minesky.tags.nametags.NametagManager;
import anarchy.minesky.tags.nametags.NametagScheduler;
import anarchy.minesky.tags.utils.helpers.DependencyHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class AnarchyTags extends JavaPlugin {

    private static AnarchyTags instance;

    private AnarchyTagsConfig config;
    private NametagManager nametagManager;
    private NametagScheduler nametagScheduler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        config = new AnarchyTagsConfig(this);
        config.load();

        nametagManager = new NametagManager();
        nametagScheduler = new NametagScheduler(this);
        nametagScheduler.start();

        getServer().getServicesManager().register(
                AnarchyTagsAPI.class,
                nametagManager,
                this,
                ServicePriority.Normal
        );

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);

        TagsPlayerConfig.register(this, config.getNametagConfig());

        AnarchyTagsCommand command = new AnarchyTagsCommand(this);
        if (getCommand("anarchytags") != null) {
            getCommand("anarchytags").setExecutor(command);
            getCommand("anarchytags").setTabCompleter(command);
        }

        DependencyHelper.load();

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                String name = event.getPlugin().getName();
                if ("PlaceholderAPI".equals(name)
                        || "AnarchyEconomy".equals(name)
                        || "AnarchyClans".equals(name)) {
                    for (var nametag : nametagManager.getAll()) {
                        nametag.updateVisibilityForAll();
                    }
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
        if (nametagManager != null) {
            nametagManager.removeAll();
        }
        getLogger().info("AnarchyTags desativado.");
    }

    public boolean reloadPlugin() {
        try {
            reloadConfig();
            config.reload();
            TagsPlayerConfig.register(this, config.getNametagConfig());
            nametagScheduler.stop();
            nametagManager.removeAll();
            if (config.getNametagConfig().isEnabled()) {
                nametagManager.createAll();
                nametagScheduler.start();
            }
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
        return instance.nametagManager;
    }

    public AnarchyTagsConfig config() {
        return config;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }
}
