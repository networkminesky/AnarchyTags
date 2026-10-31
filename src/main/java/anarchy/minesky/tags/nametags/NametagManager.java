package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.AnarchyTags;
import anarchy.minesky.tags.api.AnarchyTagsAPI;
import anarchy.minesky.tags.utils.handlers.NametagHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager implements AnarchyTagsAPI {

    private final Map<UUID, Nametag> nametags = new ConcurrentHashMap<>();
    private final Map<UUID, String> customFormats = new ConcurrentHashMap<>();

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

    public void create(Player player) {
        Nametag nametag = new Nametag(this, player);
        nametag.updateVisibilityForAll();
        nametags.put(player.getUniqueId(), nametag);
    }

    public void remove(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            NametagHandler.show(player, viewer);
        }

        Nametag nametag = nametags.remove(player.getUniqueId());
        if (nametag != null) {
            nametag.hideForAll();
        }
    }

    public void refreshViewFor(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        for (Nametag nametag : nametags.values()) {
            if (nametag.getViewers().contains(viewer.getUniqueId())) {
                nametag.update(viewer);
            }
        }
    }

    public void createAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            create(player);
        }
    }

    public void removeAll() {
        for (Nametag nametag : nametags.values()) {
            remove(nametag.getPlayer());
        }
    }

    @Override
    public void refreshTag(@NotNull Player target) {
        Nametag nametag = get(target);
        if (nametag != null) {
            nametag.updateVisibilityForAll();
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
