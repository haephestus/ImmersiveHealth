package com.haephestus.immersiveHealth.events;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.haephestus.immersiveHealth.ImmersiveHealth;
import com.haephestus.immersiveHealth.capabilities.KinesisCapabilityProvider;
import com.haephestus.immersiveHealth.capabilities.ThirstProvider;

/**
 * TEMPORARY on-screen readout for all physiology systems, combined into ONE
 * action-bar line so the systems don't overwrite each other.
 *
 * This is a debug/demo display. The real goal is a proper HUD (icons/bars drawn
 * with RegisterGuiOverlaysEvent) synced to the client with packets — that's a
 * later milestone. For now this gives you an immediate, visible readout while you
 * build out the mechanics. Delete this class once the real HUD exists.
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugHudEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return; // server owns the values; action bar auto-syncs

        StringBuilder line = new StringBuilder();

        player.getCapability(KinesisCapabilityProvider.STAMINA).ifPresent(s ->
                line.append(String.format("Stam %.0f/%.0f  ", s.getStamina(), s.getMaxStamina())));

        player.getCapability(KinesisCapabilityProvider.ENDURANCE).ifPresent(e ->
                line.append(String.format("End %.0f/%.0f  ", e.getEndurance(), e.getMaxEndurance())));

        player.getCapability(ThirstProvider.THIRST).ifPresent(t ->
                line.append(String.format("Hydr %.0f/%.0f  ", t.getHydration(), t.getMaxHydration())));

        // Effective body temp: Cold Sweat's when installed, our fallback otherwise.
        TemperatureEvents.getEffectiveBodyTemperature(player).ifPresent(temp -> {
            String tag = temp >= 60 ? " [FEVER]" : temp <= -50 ? " [COLD]" : "";
            line.append(String.format("Body %.0f%s  ", temp, tag));
        });

        if (line.length() > 0) {
            player.displayClientMessage(Component.literal(line.toString().trim()), true);
        }
    }
}
