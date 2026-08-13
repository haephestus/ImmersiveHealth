package com._27Mikael.immersiveHealth.capabilities;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com._27Mikael.immersiveHealth.ImmersiveHealth;

/**
 * STEP 1 of making a capability work: REGISTER it.
 *
 * A "capability" is Forge's official way to bolt custom data (like Stamina)
 * onto a vanilla object (like the Player) AND have it saved to disk automatically.
 *
 * Before Forge will let you attach your Stamina/Endurance data to a player,
 * you must tell Forge those classes exist. That happens exactly once, during
 * startup, when Forge fires the RegisterCapabilitiesEvent.
 *
 * WHY THIS FILE IS NEW: your Stamina/Endurance classes existed, but nothing
 * ever registered them, so `KinesisCapabilityProvider.STAMINA` was effectively
 * a dead handle that never pointed at a real, usable capability. This fixes that.
 *
 * NOTE ON THE ANNOTATION BELOW:
 *   bus = MOD  -> this event fires on the "mod event bus" (startup/lifecycle stuff).
 *   Registration and setup events live here. Gameplay events (ticks, attaching
 *   data to entities) live on the "forge event bus" instead. Putting a handler on
 *   the wrong bus is the #1 reason a @SubscribeEvent method "silently does nothing".
 *
 * Because of @Mod.EventBusSubscriber, Forge finds and registers this class for you
 * automatically — you do NOT need to `new` it up anywhere.
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(Stamina.class);
        event.register(Endurance.class);
        event.register(Thirst.class);
        event.register(BodyTemperature.class);
        ImmersiveHealth.LOGGER.info("[ImmersiveHealth] Registered Stamina, Endurance, Thirst & BodyTemperature capabilities.");
    }
}
