package anarchy.minesky.tags.debug;

import java.util.concurrent.atomic.AtomicLong;

public final class PacketMetrics {

    private static final AtomicLong teamPackets = new AtomicLong();
    private static final AtomicLong displaySpawn = new AtomicLong();
    private static final AtomicLong displayMetadata = new AtomicLong();
    private static final AtomicLong displayDestroy = new AtomicLong();
    private static final AtomicLong displayMount = new AtomicLong();
    private static final AtomicLong displayTeleport = new AtomicLong();
    private static final AtomicLong teamEnsureSent = new AtomicLong();
    private static final AtomicLong teamEnsureSkipped = new AtomicLong();

    private PacketMetrics() {}

    public static void recordTeamPacket() {
        teamPackets.incrementAndGet();
    }

    public static void recordDisplaySpawn() {
        displaySpawn.incrementAndGet();
    }

    public static void recordDisplayMetadata() {
        displayMetadata.incrementAndGet();
    }

    public static void recordDisplayDestroy() {
        displayDestroy.incrementAndGet();
    }

    public static void recordDisplayMount() {
        displayMount.incrementAndGet();
    }

    public static void recordDisplayTeleport() {
        displayTeleport.incrementAndGet();
    }

    public static void recordTeamEnsureSent() {
        teamEnsureSent.incrementAndGet();
    }

    public static void recordTeamEnsureSkipped() {
        teamEnsureSkipped.incrementAndGet();
    }

    public static long teamPackets() {
        return teamPackets.get();
    }

    public static long displaySpawn() {
        return displaySpawn.get();
    }

    public static long displayMetadata() {
        return displayMetadata.get();
    }

    public static long displayDestroy() {
        return displayDestroy.get();
    }

    public static long displayMount() {
        return displayMount.get();
    }

    public static long displayTeleport() {
        return displayTeleport.get();
    }

    public static long teamEnsureSentCount() {
        return teamEnsureSent.get();
    }

    public static long teamEnsureSkippedCount() {
        return teamEnsureSkipped.get();
    }

    public static long displayTotal() {
        return displaySpawn.get() + displayMetadata.get() + displayDestroy.get()
                + displayMount.get() + displayTeleport.get();
    }

    public static long totalPackets() {
        return teamPackets.get() + displayTotal();
    }

    public static void reset() {
        teamPackets.set(0);
        displaySpawn.set(0);
        displayMetadata.set(0);
        displayDestroy.set(0);
        displayMount.set(0);
        displayTeleport.set(0);
        teamEnsureSent.set(0);
        teamEnsureSkipped.set(0);
    }
}
