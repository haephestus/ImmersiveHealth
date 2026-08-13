package com.haephestus.immersiveHealth.compat;

import java.util.OptionalDouble;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import com.alrex.parcool.api.Stamina;

/**
 * SOFT (optional) integration with the "ParCool" parkour mod — READ side.
 *
 * This exposes ParCool's OWN parkour-stamina to the rest of our mod (used by
 * KinesisHandler's parkour gating). The actual "parkour costs my stamina" behaviour
 * lives in compat/parcool/ParCoolActionHandler, which drains our capability on
 * ParCool action events.
 *
 * Verified against ParCool 3.4.3.3 (MC 1.20.1): `com.alrex.parcool.api.Stamina`
 *   static Stamina get(Player);  int getValue(); int getMaxValue();
 *   boolean isExhausted(); void consume(int); void recover(int); void setValue(int).
 * (This class is still present in 1.20.1 builds — an earlier note claiming it was
 *  removed was based on ParCool's newer master branch, not the 1.20.1 API.)
 *
 * SAFETY: every public method checks isLoaded() before a private *Internal() helper
 * touches ParCool classes, so the mod runs fine when ParCool is absent (the JVM only
 * links the ParCool references inside the helpers, which run only when loaded).
 */
public final class ParCoolCompat {

    public static final String MOD_ID = "parcool";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private ParCoolCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** ParCool's current parkour stamina for the player, or empty if absent/unavailable. */
    public static OptionalDouble getStamina(Player player) {
        if (!LOADED) return OptionalDouble.empty();
        return getStaminaInternal(player);
    }

    /** ParCool's max parkour stamina, or empty if absent/unavailable. */
    public static OptionalDouble getMaxStamina(Player player) {
        if (!LOADED) return OptionalDouble.empty();
        return getMaxStaminaInternal(player);
    }

    /** Whether ParCool considers the player exhausted. false if ParCool absent. */
    public static boolean isExhausted(Player player) {
        if (!LOADED) return false;
        return isExhaustedInternal(player);
    }

    /** Spend ParCool parkour stamina. No-op if ParCool absent. */
    public static void consume(Player player, double amount) {
        if (!LOADED) return;
        consumeInternal(player, amount);
    }

    // ---- the only code that references ParCool classes ----

    private static OptionalDouble getStaminaInternal(Player player) {
        Stamina stamina = Stamina.get(player);
        return stamina == null ? OptionalDouble.empty() : OptionalDouble.of(stamina.getValue());
    }

    private static OptionalDouble getMaxStaminaInternal(Player player) {
        Stamina stamina = Stamina.get(player);
        return stamina == null ? OptionalDouble.empty() : OptionalDouble.of(stamina.getMaxValue());
    }

    private static boolean isExhaustedInternal(Player player) {
        Stamina stamina = Stamina.get(player);
        return stamina != null && stamina.isExhausted();
    }

    private static void consumeInternal(Player player, double amount) {
        Stamina stamina = Stamina.get(player);
        if (stamina != null) {
            stamina.consume((int) Math.ceil(amount));
        }
    }
}
