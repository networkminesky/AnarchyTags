package anarchy.minesky.tags.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.List;

public class ClientTextDisplay extends ClientDisplay {

    private Component text = Component.empty();
    private int background = -1;
    private int flags;

    public ClientTextDisplay(Location location) {
        super(EntityType.TEXT_DISPLAY, location);
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
    public List<EntityData<?>> getEntityData() {
        List<EntityData<?>> data = super.getEntityData();
        data.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text));
        if (background != -1) {
            data.add(new EntityData<>(25, EntityDataTypes.INT, background));
        }
        data.add(new EntityData<>(27, EntityDataTypes.BYTE, (byte) flags));
        return data;
    }

    private void setFlag(int mask, boolean enabled) {
        if (enabled) {
            flags |= mask;
        } else {
            flags &= ~mask;
        }
    }
}
