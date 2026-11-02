package anarchy.minesky.tags.entities;

import anarchy.minesky.tags.debug.PacketMetrics;
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
        sendDisplayPacket(packet, viewer, PacketKind.SPAWN);
    }

    public void update(Player viewer) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, getEntityData());
        sendDisplayPacket(packet, viewer, PacketKind.METADATA);
    }

    public void despawn(Player viewer) {
        if (!spawnedFor.remove(viewer.getUniqueId())) {
            return;
        }
        sendDestroy(viewer);
    }

    public void forget(UUID viewerId) {
        spawnedFor.remove(viewerId);
    }

    public void destroy(Player viewer) {
        spawnedFor.remove(viewer.getUniqueId());
        sendDestroy(viewer);
    }

    public int getEntityId() {
        return entityId;
    }

    public int getSpawnedViewerCount() {
        return spawnedFor.size();
    }

    public boolean isSpawnedFor(Player viewer) {
        return spawnedFor.contains(viewer.getUniqueId());
    }

    public void mount(Entity entity, Player viewer) {
        java.util.List<Entity> passengers = entity.getPassengers();
        int[] ids = new int[passengers.size() + 1];
        for (int i = 0; i < passengers.size(); i++) {
            ids[i] = passengers.get(i).getEntityId();
        }
        ids[passengers.size()] = entityId;
        // Só pacote SetPassengers. Não usa Entity.startRiding / addPassenger (NMS).
        sendDisplayPacket(new WrapperPlayServerSetPassengers(entity.getEntityId(), ids), viewer, PacketKind.MOUNT);
    }

    public void teleportFor(Player viewer) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                true
        );
        sendDisplayPacket(packet, viewer, PacketKind.TELEPORT);
    }

    private void sendDestroy(Player viewer) {
        sendDisplayPacket(new WrapperPlayServerDestroyEntities(new int[]{entityId}), viewer, PacketKind.DESTROY);
    }

    private enum PacketKind {
        SPAWN, METADATA, DESTROY, MOUNT, TELEPORT, OTHER
    }

    private void sendDisplayPacket(PacketWrapper<?> packet, Player player, PacketKind kind) {
        switch (kind) {
            case SPAWN -> PacketMetrics.recordDisplaySpawn();
            case METADATA -> PacketMetrics.recordDisplayMetadata();
            case DESTROY -> PacketMetrics.recordDisplayDestroy();
            case MOUNT -> PacketMetrics.recordDisplayMount();
            case TELEPORT -> PacketMetrics.recordDisplayTeleport();
            case OTHER -> { }
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void sendPacket(PacketWrapper<?> packet, Player player) {
        sendDisplayPacket(packet, player, PacketKind.OTHER);
    }
}
