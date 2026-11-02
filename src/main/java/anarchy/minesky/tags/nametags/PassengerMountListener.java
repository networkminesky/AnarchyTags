package anarchy.minesky.tags.nametags;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.bukkit.entity.Player;

/**
 * O servidor reenvia SetPassengers no teleporte (lista real, sem o TextDisplay).
 * Esse listener acrescenta o id do display no pacote — não usa NMS startRiding.
 */
public final class PassengerMountListener extends PacketListenerAbstract {

    private final NametagManager nametags;

    public PassengerMountListener(NametagManager nametags) {
        super(PacketListenerPriority.HIGH);
        this.nametags = nametags;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_PASSENGERS) {
            return;
        }
        if (!(event.getPlayer() instanceof Player viewer)) {
            return;
        }
        WrapperPlayServerSetPassengers wrapper = new WrapperPlayServerSetPassengers(event);
        int displayId = nametags.displayPassengerId(wrapper.getEntityId(), viewer);
        if (displayId < 0) {
            return;
        }
        int[] current = wrapper.getPassengers();
        if (current == null) {
            current = new int[0];
        }
        for (int id : current) {
            if (id == displayId) {
                return;
            }
        }
        int[] next = new int[current.length + 1];
        System.arraycopy(current, 0, next, 0, current.length);
        next[current.length] = displayId;
        wrapper.setPassengers(next);
    }
}
