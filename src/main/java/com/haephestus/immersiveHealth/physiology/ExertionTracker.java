package com.haephestus.immersiveHealth.physiology;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * Tracks how long each player has gone WITHOUT strenuous activity, so the Endurance
 * "deep reserve" can refuse to refill right after exertion.
 *
 * Any system that represents hard effort calls markExertion(player):
 *   - sprinting / jumping        (KinesisEvents)
 *   - parkour actions            (ParCoolActionHandler)
 *   - landing a melee hit        (CombatEvents)
 *
 * KinesisEvents calls tick(player) once per server tick to advance the counter, and
 * reads ticksSinceExertion(player) to decide whether endurance may regenerate.
 */
public final class ExertionTracker {

    private static final Map<UUID, Integer> SINCE = new ConcurrentHashMap<>();

    private ExertionTracker() {}

    /** Call when the player does something strenuous. Resets the "rested" timer. */
    public static void markExertion(Player player) {
        SINCE.put(player.getUUID(), 0);
    }

    /** Advance the timer one tick (only for players who have exerted at least once). */
    public static void tick(Player player) {
        SINCE.computeIfPresent(player.getUUID(),
                (k, v) -> v == Integer.MAX_VALUE ? v : v + 1);
    }

    /** Ticks since the player last exerted. MAX_VALUE if they never have (fully rested). */
    public static int ticksSinceExertion(Player player) {
        return SINCE.getOrDefault(player.getUUID(), Integer.MAX_VALUE);
    }

    /** Free state on logout so the map can't grow unbounded. */
    public static void clear(Player player) {
        SINCE.remove(player.getUUID());
    }
}
