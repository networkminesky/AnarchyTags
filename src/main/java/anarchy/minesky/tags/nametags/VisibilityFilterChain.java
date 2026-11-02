package anarchy.minesky.tags.nametags;

import anarchy.minesky.tags.api.NametagVisibilityFilter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

final class VisibilityFilterChain {

    private VisibilityFilterChain() {}

    static boolean passes(List<NametagVisibilityFilter> filters, Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        List<NametagVisibilityFilter> sorted = filters.stream()
                .sorted(Comparator.comparingInt(NametagVisibilityFilter::priority).reversed())
                .toList();
        for (NametagVisibilityFilter filter : sorted) {
            if (!filter.shouldShow(viewer, target)) {
                return false;
            }
        }
        return true;
    }
}
