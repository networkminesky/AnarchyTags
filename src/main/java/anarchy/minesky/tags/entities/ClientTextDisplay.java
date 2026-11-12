package anarchy.minesky.tags.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientTextDisplay extends ClientDisplay {

    private Component text = Component.empty();
    private int background = TextDisplayDefaults.BACKGROUND;
    private int flags;
    private final Map<UUID, ViewerVisual> viewerVisuals = new ConcurrentHashMap<>();

    public ClientTextDisplay(Location location) {
        super(EntityType.TEXT_DISPLAY, location);
    }

    public void setViewerVisual(Player viewer, Component text, int background, boolean shadow) {
        if (viewer == null) {
            return;
        }
        viewerVisuals.put(viewer.getUniqueId(), new ViewerVisual(
                text == null ? Component.empty() : text,
                background,
                shadow
        ));
    }

    public void setText(Component text) {
        this.text = text == null ? Component.empty() : text;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public void setTextShadow(boolean enabled) {
        setFlag(0x01, enabled);
    }

    public void setSeeThrough(boolean enabled) {
        setFlag(0x02, enabled);
    }

    public void setTextAlignment(TextAlignment alignment) {
        flags &= ~(0x08 | 0x10);
        switch (alignment) {
            case LEFT -> flags |= 0x08;
            case RIGHT -> flags |= 0x10;
            case CENTER -> {}
        }
    }

    @Override
    public void spawn(Player viewer) {
        applyViewerVisual(viewer);
        super.spawn(viewer);
    }

    @Override
    public void update(Player viewer) {
        applyViewerVisual(viewer);
        super.update(viewer);
    }

    @Override
    public void despawn(Player viewer) {
        despawn(viewer, null);
    }

    @Override
    public void despawn(Player viewer, Entity vehicle) {
        if (viewer != null) {
            viewerVisuals.remove(viewer.getUniqueId());
        }
        super.despawn(viewer, vehicle);
    }

    @Override
    public void destroy(Player viewer) {
        if (viewer != null) {
            viewerVisuals.remove(viewer.getUniqueId());
        }
        super.destroy(viewer);
    }

    @Override
    public void forget(UUID viewerId) {
        if (viewerId != null) {
            viewerVisuals.remove(viewerId);
        }
        super.forget(viewerId);
    }

    @Override
    public List<EntityData<?>> getEntityData() {
        List<EntityData<?>> data = super.getEntityData();
        data.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text));
        // Sempre enviar: omitir o índice 25 deixa o cliente com valor antigo (ex.: transparente → default).
        data.add(new EntityData<>(25, EntityDataTypes.INT, background));
        data.add(new EntityData<>(27, EntityDataTypes.BYTE, (byte) flags));
        return data;
    }

    private void applyViewerVisual(Player viewer) {
        if (viewer == null) {
            return;
        }
        ViewerVisual visual = viewerVisuals.get(viewer.getUniqueId());
        if (visual == null) {
            return;
        }
        this.text = visual.text();
        this.background = visual.background();
        setTextShadow(visual.shadow());
    }

    private void setFlag(int mask, boolean enabled) {
        if (enabled) {
            flags |= mask;
        } else {
            flags &= ~mask;
        }
    }

    private record ViewerVisual(Component text, int background, boolean shadow) {}
}
