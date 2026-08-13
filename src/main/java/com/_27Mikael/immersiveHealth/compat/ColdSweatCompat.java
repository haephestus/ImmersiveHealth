package com._27Mikael.immersiveHealth.compat;

import java.lang.reflect.Method;
import java.util.OptionalDouble;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import com._27Mikael.immersiveHealth.ImmersiveHealth;

/**
 * SOFT (optional) integration with the "Cold Sweat" temperature mod.
 *
 * THE MODEL (important):
 *   Cold Sweat owns AMBIENT/world temperature AND computes a full BODY temperature
 *   from its own systems (biomes, blocks, items, insulation, ...). When Cold Sweat
 *   is installed, Immersive Health FEEDS OFF Cold Sweat's BODY temperature — we do
 *   NOT compute our own. Only when Cold Sweat is ABSENT does our mod fall back to
 *   its own simple body-temperature model (see TemperatureEvents).
 *
 * WHY REFLECTION:
 *   The mod must build/run whether or not Cold Sweat is present. A direct import
 *   would crash when it's missing, so we look the API up by name at runtime and
 *   fall back to "no reading" if it isn't there.
 *
 * VERIFIED against Cold Sweat 2.4.2 (MC 1.20.1):
 *   com.momosoftworks.coldsweat.api.util.Temperature
 *   Temperature.Trait { WORLD, CORE, BASE, BODY, ... }
 *   public static double get(LivingEntity entity, Trait trait)
 *
 * Cold Sweat BODY scale: ~ -100 (freezing) .. 0 (neutral) .. +100 (overheating),
 * with damage near the extremes. WORLD is the ambient reading in Cold Sweat units.
 */
public final class ColdSweatCompat {

    public static final String MOD_ID = "coldsweat";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private static boolean triedInit = false;
    private static Method getMethod;   // Temperature.get(LivingEntity, Trait)
    private static Object bodyTrait;   // Trait.BODY
    private static Object worldTrait;  // Trait.WORLD

    private ColdSweatCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    // (className, enumSimpleName) candidates across Cold Sweat versions. Newest first.
    private static final String[][] CANDIDATES = {
            {"com.momosoftworks.coldsweat.api.util.Temperature", "Trait"}, // 2.x (current)
            {"com.momosoftworks.coldsweat.api.util.Temperature", "Type"},  // older 2.x
            {"dev.momostudios.coldsweat.api.temperature.Temperature", "Type"}, // legacy 1.x
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void ensureInit() {
        if (triedInit) return;
        triedInit = true;
        for (String[] c : CANDIDATES) {
            try {
                Class<?> tempClass = Class.forName(c[0]);
                Class<?> traitClass = Class.forName(c[0] + "$" + c[1]);
                getMethod = tempClass.getMethod("get", LivingEntity.class, traitClass);
                bodyTrait = Enum.valueOf((Class<Enum>) traitClass.asSubclass(Enum.class), "BODY");
                worldTrait = Enum.valueOf((Class<Enum>) traitClass.asSubclass(Enum.class), "WORLD");
                ImmersiveHealth.LOGGER.info("[ImmersiveHealth] Cold Sweat detected -> body temperature "
                        + "drives thirst & fever ({}).", c[0]);
                return;
            } catch (Throwable ignored) {
                // try next candidate
            }
        }
        ImmersiveHealth.LOGGER.warn("[ImmersiveHealth] Cold Sweat is loaded but its temperature API did "
                + "not match any known signature; using our own body-temp fallback. Update "
                + "ColdSweatCompat.CANDIDATES for this Cold Sweat version.");
    }

    private static OptionalDouble read(Player player, Object trait) {
        if (!LOADED) return OptionalDouble.empty();
        ensureInit();
        if (getMethod == null || trait == null) return OptionalDouble.empty();
        try {
            Object result = getMethod.invoke(null, player, trait);
            if (result instanceof Number n) return OptionalDouble.of(n.doubleValue());
        } catch (Throwable ignored) {
            // treat as no reading
        }
        return OptionalDouble.empty();
    }

    /** Cold Sweat's BODY temperature for the player. Authoritative when present. */
    public static OptionalDouble getBodyTemperature(Player player) {
        return read(player, bodyTrait);
    }

    /** Cold Sweat's WORLD/ambient temperature for the player. */
    public static OptionalDouble getAmbientTemperature(Player player) {
        return read(player, worldTrait);
    }

    /**
     * Thirst-drain multiplier from body temperature:
     *   neutral / no Cold Sweat -> 1.0
     *   hot (up to ~+100)        -> up to ~2.0x (sweating loses water)
     *   cold (down to ~-100)     -> down to ~0.67x
     */
    public static double thirstDrainMultiplier(Player player) {
        OptionalDouble temp = getBodyTemperature(player);
        if (temp.isEmpty()) return 1.0;
        double t = temp.getAsDouble();
        double mult = (t >= 0) ? 1.0 + (t / 100.0) : 1.0 + (t / 300.0);
        return Math.max(0.5, mult);
    }
}
