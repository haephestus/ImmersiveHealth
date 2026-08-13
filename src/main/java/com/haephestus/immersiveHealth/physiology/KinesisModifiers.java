package com.haephestus.immersiveHealth.physiology;

import net.minecraft.world.entity.player.Player;

/**
 * The single place where MUSCULATURE and METABOLISM will modulate Kinesis costs.
 *
 * Right now every method returns 1.0 (neutral), so behaviour is unchanged. The
 * Kinesis tick multiplies its base drain/regen numbers by these, so once the
 * Musculature and Metabolism capabilities exist, we only fill these three methods
 * in — the tick loop never changes. See docs/PHYSIOLOGY_PLAN.md for the design and
 * the exact formulas these will implement.
 *
 * Intended (from the plan):
 *   staminaDrainMultiplier  -> lower when musculature is strong (efficient muscles),
 *                              higher when metabolism is starved (low fuel).
 *   staminaRegenMultiplier  -> higher when well-fed & rested.
 *   enduranceRegenMultiplier-> driven mostly by metabolism (nutrition) + rest.
 */
public final class KinesisModifiers {

    private KinesisModifiers() {}

    public static double staminaDrainMultiplier(Player player) {
        // Planned (see docs/PHYSIOLOGY_PLAN.md). Let fitness = conditioning/100 (0..1):
        //   efficiency     = 0.6 + 0.4 * fitness           // fit muscles spend less
        //   fatiguePenalty = 1 + (fatigue / 100)           // tired muscles spend more
        //   return efficiency * fatiguePenalty * metabolism.coldBurn * metabolism.lowFuelPenalty
        return 1.0; // neutral until Musculature/Metabolism exist
    }

    public static double staminaRegenMultiplier(Player player) {
        // Planned:  return lerp(0.5, 1.0, metabolism.energyFactor) * (0.5 + fitness);
        return 1.0;
    }

    public static double enduranceRegenMultiplier(Player player) {
        // FITNESS modifier for endurance recovery (used by the endurance trickle/rest).
        // Planned formula, fitness = conditioning/100 (0..1):
        //   recoveryIndex = 0.5 + fitness                  // unfit 0.5x .. peak-fit ~1.5x
        //   return recoveryIndex * metabolism.energyFactor // well-fed recovers faster
        return 1.0; // neutral until Musculature/Metabolism exist
    }
}
