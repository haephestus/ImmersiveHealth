package com.haephestus.immersiveHealth.compat.parcool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;

import com.haephestus.immersiveHealth.capabilities.Endurance;
import com.haephestus.immersiveHealth.capabilities.KinesisCapabilityProvider;
import com.haephestus.immersiveHealth.capabilities.Stamina;
import com.haephestus.immersiveHealth.config.IHConfig;
import com.haephestus.immersiveHealth.physiology.ExertionTracker;

/**
 * PARKOUR ↔ PHYSIOLOGY: makes ParCool parkour spend YOUR mod's Stamina/Endurance.
 *
 * Verified against ParCool 3.4.3.3 (MC 1.20.1), which fires ParCoolActionEvent
 * (on the FORGE bus) around every parkour action:
 *   - Start.Post   : a parkour move just began  -> we DRAIN stamina (then endurance).
 *   - TryToStart   : ParCool is about to start a move (cancelable) -> we CANCEL it
 *                    when the player is fully out of energy, so you can't parkour
 *                    while exhausted.
 * (The package is api.unstable.* — ParCool marks it as potentially-changing, so if
 *  a future ParCool update moves it, only this file needs adjusting.)
 *
 * ⚠️ LOADING SAFETY: references ParCool classes, so it is registered on the Forge
 * bus ONLY when ParCool is present (see ImmersiveHealth + ParCoolCompat.isLoaded()).
 * It is deliberately NOT @Mod.EventBusSubscriber (which would register always).
 *
 * TIP: for a "my stamina is the only stamina" feel, set ParCool's own stamina to
 * infinite in its config — then these events are the sole cost/gate for parkour.
 */
public class ParCoolActionHandler {

    /**
     * Stamina cost per parkour action, keyed by ParCool's action class SIMPLE NAME
     * (e.g. "Vault", "WallJump"). We key by name (via getClass().getSimpleName())
     * instead of importing 26 ParCool classes — easy to read and tune. Anything not
     * listed uses DEFAULT_ACTION_COST. All values are tuning knobs.
     *
     * Note: continuous moves (FastRun, wall-runs, WallSlide…) only pay this ONCE at
     * start. To also drain them per tick, subscribe to ParCoolActionEvent.Tick.Post
     * and charge a small amount there.
     */
    private static final double DEFAULT_ACTION_COST = 8.0;
    private static final Map<String, Double> ACTION_COSTS = new HashMap<>();
    static {
        // explosive / high-effort
        ACTION_COSTS.put("ChargeJump", 16.0);
        ACTION_COSTS.put("CatLeap", 14.0);
        ACTION_COSTS.put("VerticalWallRun", 14.0);
        ACTION_COSTS.put("HorizontalWallRun", 14.0);
        ACTION_COSTS.put("WallJump", 12.0);
        ACTION_COSTS.put("Dive", 12.0);
        ACTION_COSTS.put("Flipping", 10.0);
        ACTION_COSTS.put("JumpFromBar", 10.0);
        // moderate
        ACTION_COSTS.put("ClimbUp", 9.0);
        ACTION_COSTS.put("Vault", 8.0);
        ACTION_COSTS.put("ClimbPoles", 8.0);
        ACTION_COSTS.put("Dodge", 8.0);
        ACTION_COSTS.put("Roll", 6.0);
        ACTION_COSTS.put("Slide", 6.0);
        ACTION_COSTS.put("SkyDive", 6.0);
        ACTION_COSTS.put("FastSwim", 5.0);
        ACTION_COSTS.put("QuickTurn", 4.0);
        ACTION_COSTS.put("RideZipline", 3.0);
        // light / continuous / minor
        ACTION_COSTS.put("ClingToCliff", 3.0);
        ACTION_COSTS.put("FastRun", 2.0);
        ACTION_COSTS.put("Tap", 2.0);
        ACTION_COSTS.put("HangDown", 2.0);
        ACTION_COSTS.put("WallSlide", 2.0);
        ACTION_COSTS.put("Crawl", 1.0);
        ACTION_COSTS.put("HideInBlock", 1.0);
        ACTION_COSTS.put("BreakfallReady", 0.0);
    }

    @SubscribeEvent
    public void onParkourStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        if (!IHConfig.ENABLE_STAMINA.get()) return; // stamina system off -> parkour is free

        String actionName = event.getAction().getClass().getSimpleName();
        double cost = ACTION_COSTS.getOrDefault(actionName, DEFAULT_ACTION_COST);
        if (cost > 0) {
            drainStaminaThenEndurance(player, cost);
        }
        // Parkour is strenuous: reset the endurance rest timer so it won't refill
        // for a while afterwards (see ExertionTracker + KinesisEvents).
        ExertionTracker.markExertion(player);
    }

    @SubscribeEvent
    public void onTryParkour(ParCoolActionEvent.TryToStart event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        if (!IHConfig.ENABLE_STAMINA.get()) return; // don't gate parkour if stamina is off
        double energy = stamina(player) + endurance(player);
        if (energy <= 0 && event.isCancelable()) {
            event.setCanceled(true); // too exhausted to parkour
        }
    }

    // ---- helpers (mirrors CombatEvents; kept local to isolate ParCool code) ----

    private static double stamina(Player player) {
        return player.getCapability(KinesisCapabilityProvider.STAMINA)
                .resolve().map(Stamina::getStamina).orElse(0.0);
    }

    private static double endurance(Player player) {
        return player.getCapability(KinesisCapabilityProvider.ENDURANCE)
                .resolve().map(Endurance::getEndurance).orElse(0.0);
    }

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
