package com.haephestus.immersiveHealth.events;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.haephestus.immersiveHealth.ImmersiveHealth;
import com.haephestus.immersiveHealth.capabilities.KinesisCapabilityProvider;
import com.haephestus.immersiveHealth.capabilities.Stamina;
import com.haephestus.immersiveHealth.config.IHConfig;
import com.haephestus.immersiveHealth.physiology.ExertionTracker;

/**
 * COMBAT drains your Stamina (and then Endurance). Built on vanilla Forge events,
 * so it works with OR without Better Combat installed (Better Combat routes its
 * attacks through these same events). Your Stamina is the single source of truth.
 *
 * Rules (from the design):
 *   - Each landed hit costs stamina, scaled by the damage dealt -> light swings are
 *     cheap, heavy weapons and crits cost more ("light vs heavy" for free).
 *   - Blocking a hit with a shield also costs stamina, scaled by damage blocked.
 *   - When stamina is empty (you're running on endurance / exhausted):
 *       * your attacks are WEAKENED (reduced damage), and
 *       * the cost drains from ENDURANCE instead.
 *   - A shield guard also falters (lets more through) when you're exhausted.
 *
 * NOT covered here: draining on a swing that MISSES — that needs Better Combat's
 * own attack callback (see compat/BetterCombatCompat). Everything else is here.
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEvents {

    // ---- tuning knobs ----
    private static final double ATTACK_BASE_COST = 5.0;
    private static final double ATTACK_COST_PER_DAMAGE = 1.5;
    private static final double BLOCK_BASE_COST = 4.0;
    private static final double BLOCK_COST_PER_DAMAGE = 1.0;
    private static final float EXHAUSTED_DAMAGE_MULT = 0.5f; // your hits do half damage when exhausted
    private static final float EXHAUSTED_BLOCK_MULT = 0.5f;  // your shield blocks less when exhausted

    /** A player landed a melee hit. */
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!IHConfig.ENABLE_STAMINA.get()) return;

        double damage = event.getAmount();
        double cost = ATTACK_BASE_COST + damage * ATTACK_COST_PER_DAMAGE;

        // Weaken the hit if we're already out of stamina (running on endurance).
        if (currentStamina(player) <= 0) {
            event.setAmount((float) (damage * EXHAUSTED_DAMAGE_MULT));
        }

        drainStaminaThenEndurance(player, cost);
        ExertionTracker.markExertion(player); // fighting is strenuous -> delays endurance regen
    }

    /** A player blocked an incoming hit with a shield. */
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!IHConfig.ENABLE_STAMINA.get()) return;

        double blocked = event.getBlockedDamage();
        double cost = BLOCK_BASE_COST + blocked * BLOCK_COST_PER_DAMAGE;

        // Guard falters when exhausted: let some of the blocked damage through.
        if (currentStamina(player) <= 0) {
            event.setBlockedDamage(event.getBlockedDamage() * EXHAUSTED_BLOCK_MULT);
        }

        drainStaminaThenEndurance(player, cost);
    }

    // ---- helpers ----

    private static double currentStamina(Player player) {
        return player.getCapability(KinesisCapabilityProvider.STAMINA)
                .resolve().map(Stamina::getStamina).orElse(100.0);
    }

    /**
     * Spend {@code amount} of stamina; if stamina can't cover it, take the
     * remainder from endurance. Both setters clamp at 0.
     */
    private static void drainStaminaThenEndurance(Player player, double amount) {
        player.getCapability(KinesisCapabilityProvider.STAMINA).ifPresent(stamina -> {
            double s = stamina.getStamina();
            if (s >= amount || !IHConfig.ENABLE_ENDURANCE.get()) {
                stamina.setStamina(s - amount); // drain stamina only (setter clamps at 0)
            } else {
                stamina.setStamina(0);
                double remainder = amount - s;
                player.getCapability(KinesisCapabilityProvider.ENDURANCE).ifPresent(endurance ->
                        endurance.setEndurance(endurance.getEndurance() - remainder));
            }
        });
    }
}
