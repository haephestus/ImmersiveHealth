package com.haephestus.immersiveHealth.events;

import java.util.OptionalDouble;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.haephestus.immersiveHealth.ImmersiveHealth;
import com.haephestus.immersiveHealth.capabilities.BodyTemperatureProvider;
import com.haephestus.immersiveHealth.compat.ColdSweatCompat;
import com.haephestus.immersiveHealth.config.IHConfig;

/**
 * BODY TEMPERATURE + FEVER system.
 *
 * SOURCE OF TRUTH (matches the mod's design):
 *   - Cold Sweat installed  -> we FEED OFF Cold Sweat's BODY temperature. Our own
 *     stored value is not used; Cold Sweat already accounts for all ambient/biome/
 *     insulation factors.
 *   - Cold Sweat absent      -> we run a SIMPLE fallback model into our own
 *     BodyTemperature capability (biome warmth + activity + water cooling).
 *
 * Either way, downstream effects (fever, thirst drain) read the same "effective"
 * body temperature via getEffectiveBodyTemperature(), so behaviour is consistent.
 *
 * Scale: 0 = neutral, positive = hot (fever), negative = cold. (Cold Sweat units.)
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TemperatureEvents {

    // Fever / cold thresholds on the -100..100 scale (tuning knobs).
    private static final double FEVER_MILD = 40.0;
    private static final double FEVER_SEVERE = 60.0;
    private static final double COLD_SEVERE = -50.0;

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(ImmersiveHealth.MOD_ID, "body_temperature"),
                    new BodyTemperatureProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(BodyTemperatureProvider.BODY_TEMPERATURE).ifPresent(oldTemp ->
                event.getEntity().getCapability(BodyTemperatureProvider.BODY_TEMPERATURE).ifPresent(newTemp ->
                        newTemp.setTemperature(oldTemp.getTemperature())));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!IHConfig.ENABLE_BODY_TEMP.get()) return; // body-temperature system disabled in config

        // If Cold Sweat is present it owns body temp; we don't run our own model.
        if (ColdSweatCompat.isLoaded() && ColdSweatCompat.getBodyTemperature(player).isPresent()) {
            applyEffects(player, ColdSweatCompat.getBodyTemperature(player).getAsDouble());
            return;
        }

        // Fallback model (Cold Sweat absent): ease our stored temp toward a target.
        player.getCapability(BodyTemperatureProvider.BODY_TEMPERATURE).ifPresent(temp -> {
            double target = fallbackTargetTemperature(player);
            double current = temp.getTemperature();
            current += (target - current) * 0.01; // slow drift
            temp.setTemperature(current);
            applyEffects(player, temp.getTemperature());
        });
    }

    /** Simple target body temperature from biome warmth, activity and water. */
    private static double fallbackTargetTemperature(Player player) {
        // MC biome base temperature: ~0.0 (snowy) .. ~0.8 (temperate) .. ~2.0 (desert).
        float biome = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
        double target = (biome - 0.8) * 60.0;      // temperate ~0, desert ~ +72, snow ~ -48
        if (player.isSprinting()) target += 15.0;  // exertion warms you
        if (player.isInWaterOrRain()) target -= 15.0; // water/rain cools you
        return Math.max(-100.0, Math.min(100.0, target));
    }

    /** Apply fever (hot) or shivering (cold) effects based on effective body temp. */
    private static void applyEffects(Player player, double bodyTemp) {
        if (bodyTemp >= FEVER_SEVERE) {
            // Severe fever: disoriented + slowed.
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false)); // Nausea
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        } else if (bodyTemp >= FEVER_MILD) {
            // Mild fever: sluggish.
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        } else if (bodyTemp <= COLD_SEVERE) {
            // Shivering cold: sluggish (Cold Sweat also handles its own cold damage).
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }
    }

    /**
     * The value the rest of the mod (and the HUD) should use: Cold Sweat's BODY
     * temperature when available, otherwise our stored fallback. Read-only.
     */
    public static OptionalDouble getEffectiveBodyTemperature(Player player) {
        OptionalDouble cs = ColdSweatCompat.getBodyTemperature(player);
        if (cs.isPresent()) return cs;
        return player.getCapability(BodyTemperatureProvider.BODY_TEMPERATURE)
                .map(t -> OptionalDouble.of(t.getTemperature()))
                .orElse(OptionalDouble.empty());
    }
}
