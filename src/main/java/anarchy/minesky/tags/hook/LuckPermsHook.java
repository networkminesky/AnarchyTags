package anarchy.minesky.tags.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class LuckPermsHook {

    private static volatile LuckPerms api;

    private LuckPermsHook() {}

    public static int effectiveSortWeight(UUID uuid) {
        if (uuid == null) {
            return 0;
        }
        User user = user(uuid);
        if (user == null) {
            return 0;
        }

        LuckPerms luckPerms = resolve();
        if (luckPerms == null) {
            return 0;
        }

        int max = 0;
        for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
            Group group = luckPerms.getGroupManager().getGroup(node.getGroupName());
            if (group != null) {
                max = Math.max(max, group.getWeight().orElse(0));
            }
        }

        Group primary = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
        if (primary != null) {
            max = Math.max(max, primary.getWeight().orElse(0));
        }
        return max;
    }

    public static int highestInheritedWeight(Player player) {
        return player == null ? 0 : effectiveSortWeight(player.getUniqueId());
    }

    public static int highestInheritedWeight(UUID uuid) {
        return effectiveSortWeight(uuid);
    }

    public static String primaryGroup(Player player) {
        return player == null ? "" : primaryGroup(player.getUniqueId());
    }

    public static String primaryGroup(UUID uuid) {
        User user = user(uuid);
        return user == null ? "" : user.getPrimaryGroup();
    }

    private static User user(UUID uuid) {
        LuckPerms luckPerms = resolve();
        if (luckPerms == null) {
            return null;
        }
        return luckPerms.getUserManager().getUser(uuid);
    }

    private static LuckPerms resolve() {
        LuckPerms cached = api;
        if (cached != null) {
            return cached;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return null;
        }
        cached = Bukkit.getServicesManager().load(LuckPerms.class);
        api = cached;
        return cached;
    }
}
