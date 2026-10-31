package anarchy.minesky.tags.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.List;

public class ClientDisplay extends ClientEntity {

    private Vector3f translation;
    private Vector3f scale;
    private DisplayBillboard billboard;

    public ClientDisplay(EntityType type, Location location) {
        super(type, location);
    }

    public void setTranslation(Vector3f translation) {
        this.translation = translation;
    }

    public void setScale(Vector scale) {
        this.scale = new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ());
    }

    public void setBillboard(DisplayBillboard billboard) {
        this.billboard = billboard;
    }

    @Override
    public List<EntityData<?>> getEntityData() {
        List<EntityData<?>> data = super.getEntityData();
        if (scale != null) {
            data.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, scale));
        }
        if (translation != null) {
            data.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, translation));
        }
        if (billboard != null) {
            data.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) billboard.value));
        }
        return data;
    }
}
