package anarchy.minesky.tags.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ClientEntity {

    private final int entityId;
    private final UUID uuid;
    private final com.github.retrooper.packetevents.protocol.entity.type.EntityType type;
    private Location location;
    private final Set<UUID> spawnedFor = ConcurrentHashMap.newKeySet();

    public ClientEntity(EntityType type, Location location) {
        this.entityId = ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000);
        this.uuid = UUID.randomUUID();
        this.type = mapEntityType(type);
        this.location = location.clone();
    }

    private static com.github.retrooper.packetevents.protocol.entity.type.EntityType mapEntityType(EntityType type) {
        if (type == EntityType.TEXT_DISPLAY) {
            return EntityTypes.TEXT_DISPLAY;
        }
        throw new IllegalArgumentException("Unsupported client entity type: " + type);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location.clone();
    }

    public List<EntityData<?>> getEntityData() {
        return new ArrayList<>();
    }

    public void spawn(Player viewer) {
        if (!spawnedFor.add(viewer.getUniqueId())) {
            return;
        }
        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                type,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                0,
                0,
                Optional.empty()
        );
        sendPacket(packet, viewer);
    }

    public void update(Player viewer) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, getEntityData());
        sendPacket(packet, viewer);
    }

    public void despawn(Player viewer) {
        if (!spawnedFor.remove(viewer.getUniqueId())) {
            return;
        }
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(new int[]{entityId});
        sendPacket(packet, viewer);
    }

    public boolean isSpawnedFor(Player viewer) {
        return spawnedFor.contains(viewer.getUniqueId());
    }

    public void mount(Entity entity, Player viewer) {
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(
                entity.getEntityId(),
                new int[]{entityId}
        );
        sendPacket(packet, viewer);
    }

    public void teleportFor(Player viewer) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                true
        );
        sendPacket(packet, viewer);
    }

    private void sendPacket(PacketWrapper<?> packet, Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
