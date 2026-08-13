package com.haephestus.immersiveHealth.events;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import com.haephestus.immersiveHealth.capabilities.Endurance;
import com.haephestus.immersiveHealth.capabilities.KinesisCapabilityProvider;
import com.haephestus.immersiveHealth.capabilities.Stamina;
import com.haephestus.immersiveHealth.capabilities.ThirstProvider;
import com.haephestus.immersiveHealth.config.IHConfig;
import com.haephestus.immersiveHealth.physiology.ExertionTracker;
import com.haephestus.immersiveHealth.physiology.KinesisModifiers;

/**
 * This class is the "engine" of the stamina vertical slice. It contains the three
 * pieces of wiring your project was missing, plus the actual per-tick gameplay logic.
 *
 * It lives on the FORGE event bus (bus = FORGE) because everything here happens
 * DURING GAMEPLAY (attaching data to a player, the player respawning, the world
 * ticking) — as opposed to one-time startup, which lives on the MOD bus.
 *
 * Read the three @SubscribeEvent methods top-to-bottom; together they are the
 * complete "life story" of a piece of custom player data in Forge.
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KinesisEvents {

    /**
     * STEP 2: ATTACH the capability to each player.
     *
     * Registering (ModCapabilities) only told Forge the type exists. It does NOT
     * put any data on any player. Forge fires AttachCapabilitiesEvent for every
     * object that CAN hold capabilities (entities, item stacks, block entities...).
     * We only care about players, so we filter with `instanceof Player` and then
     * bolt on a fresh KinesisCapabilityProvider (which holds one Stamina + one
     * Endurance object and knows how to save/load them from disk).
     *
     * The ResourceLocation is just a unique name/id for this attached data:
     * "immersivehealth:kinesis".
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(ImmersiveHealth.MOD_ID, "kinesis"),
                    new KinesisCapabilityProvider());
        }
    }

    /**
     * STEP 2b: KEEP the data across death/respawn.
     *
     * When a player dies and respawns, Minecraft actually creates a BRAND NEW
     * Player object. Its freshly attached capability would be back at default
     * (100 stamina). If you want stamina to persist through death, you must copy
     * it from the old ("original") player to the new one. Without this, players
     * would "cheat death" by getting full stamina every respawn.
     *
     * reviveCaps()/invalidateCaps() temporarily unlock the dead player's caps so
     * we can read them, then re-lock them. This is standard Forge boilerplate.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return; // also fires when changing dimensions; only copy on death
        event.getOriginal().reviveCaps();
        // Copy stamina from the dead player to the new one...
        event.getOriginal().getCapability(KinesisCapabilityProvider.STAMINA).ifPresent(oldStamina ->
                event.getEntity().getCapability(KinesisCapabilityProvider.STAMINA).ifPresent(newStamina ->
                        newStamina.setStamina(oldStamina.getStamina())));
        // ...and the same for endurance.
        event.getOriginal().getCapability(KinesisCapabilityProvider.ENDURANCE).ifPresent(oldEndurance ->
                event.getEntity().getCapability(KinesisCapabilityProvider.ENDURANCE).ifPresent(newEndurance ->
                        newEndurance.setEndurance(oldEndurance.getEndurance())));
        event.getOriginal().invalidateCaps();
    }

    /** Horizontal move (blocks/tick, squared) above which we count you as "walking". */
    private static final double WALK_THRESHOLD_SQ = 0.0004; // ~0.02 blocks/tick

    /**
     * Per-player movement snapshot from last tick, used to detect walking and jumps
     * SERVER-SIDE from synced position (Forge's LivingJumpEvent fires client-only for
     * players, so we can't rely on it). Cleared on logout.
     */
    private static final Map<UUID, double[]> LAST_STATE = new ConcurrentHashMap<>();
    // index layout: [0]=x [1]=y [2]=z [3]=onGround(1/0)

    /**
     * STEP 3: DO SOMETHING every tick — the actual gameplay.
     *
     * Runs on the SERVER only (single source of truth); the action-bar HUD
     * (DebugHudEvents) auto-syncs the values to the client. All numbers come from
     * IHConfig (config/immersivehealth-common.toml) and are scaled by KinesisModifiers
     * (the musculature/metabolism seam).
     *
     * THE KINESIS RULE:
     *   - Running/walking/jumping all cost STAMINA (running > jumping/sec > walking).
     *   - When stamina hits 0, that cost drains ENDURANCE (the deep reserve) instead.
     *   - Both empty -> EXHAUSTED -> Slowness.
     *   - Standing still quickly refills STAMINA.
     *   - ENDURANCE is a deep reserve: it only refills via SLEEP, EATING and RESTING,
     *     and NOT until a while after the last strenuous activity (see ExertionTracker
     *     + restDelayTicks). Parkour/combat/sprint all reset that timer.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return; // server owns the truth
        if (!IHConfig.ENABLE_STAMINA.get()) return; // stamina system disabled in config

        var staminaCap = player.getCapability(KinesisCapabilityProvider.STAMINA).resolve();
        var enduranceCap = player.getCapability(KinesisCapabilityProvider.ENDURANCE).resolve();
        if (staminaCap.isEmpty() || enduranceCap.isEmpty()) return;

        Stamina stamina = staminaCap.get();
        Endurance endurance = enduranceCap.get();

        ExertionTracker.tick(player); // advance the "time since strenuous activity" clock

        // Figure out how the player moved this tick from synced position.
        double[] prev = LAST_STATE.get(player.getUUID());
        double x = player.getX(), y = player.getY(), z = player.getZ();
        boolean onGround = player.onGround();
        double dx = prev == null ? 0 : x - prev[0];
        double dz = prev == null ? 0 : z - prev[2];
        double horizontalSq = dx * dx + dz * dz;
        boolean wasOnGround = prev == null || prev[3] != 0;
        boolean rose = prev != null && y > prev[1] + 0.02;
        boolean jumped = wasOnGround && !onGround && rose; // ground -> air while rising
        LAST_STATE.put(player.getUUID(), new double[]{x, y, z, onGround ? 1 : 0});

        double drainMult = KinesisModifiers.staminaDrainMultiplier(player);

        // --- one-off jump cost (jumping is strenuous) ---
        if (jumped) {
            drainStaminaThenEndurance(stamina, endurance, IHConfig.JUMP_COST.get() * drainMult);
            ExertionTracker.markExertion(player);
        }

        // --- continuous movement cost / regen ---
        if (player.isSprinting()) {
            drainStaminaThenEndurance(stamina, endurance, IHConfig.SPRINT_DRAIN.get() * drainMult);
            ExertionTracker.markExertion(player); // running is strenuous
        } else if (onGround && horizontalSq > WALK_THRESHOLD_SQ) {
            // walking is light activity: costs a little stamina but is NOT "strenuous",
            // so it doesn't reset the endurance rest timer.
            drainStaminaThenEndurance(stamina, endurance, IHConfig.WALK_DRAIN.get() * drainMult);
        } else {
            // Standing still: STAMINA (short-term) refills quickly.
            stamina.setStamina(stamina.getStamina()
                    + IHConfig.STAMINA_REGEN.get() * KinesisModifiers.staminaRegenMultiplier(player));

            // ENDURANCE (deep reserve) recovery — only if the endurance system is on.
            if (IHConfig.ENABLE_ENDURANCE.get()) {
                double eRate = 0.0;

                // (a) Passive slow trickle whenever stamina is topped up: you're not
                //     spending energy, so the reserve slowly creeps back.
                if (stamina.getStamina() >= stamina.getMaxStamina()) {
                    eRate += IHConfig.ENDURANCE_FULL_STAMINA_TRICKLE.get();
                }

                // (b) Real recovery from SLEEP / REST / EATING — only once enough time
                //     has passed since the last strenuous activity (parkour/sprint/combat).
                if (ExertionTracker.ticksSinceExertion(player) >= IHConfig.REST_DELAY_TICKS.get()) {
                    if (player.isSleeping()) {
                        eRate += IHConfig.ENDURANCE_SLEEP_REGEN.get() * sleepQuality(player);
                    } else {
                        eRate += IHConfig.ENDURANCE_REST_REGEN.get(); // awake but still = light rest
                    }
                    if (player.getFoodData().getSaturationLevel() > 0) {
                        eRate += IHConfig.ENDURANCE_EAT_BONUS.get(); // digesting fuels recovery
                    }
                }

                // FITNESS scaling: enduranceRegenMultiplier() is where musculature will
                // scale this. Planned formula (see KinesisModifiers):
                //   multiplier = 0.5 + fitness      // fitness = conditioning/100 (0..1)
                // -> an unfit player recovers at 0.5x, a peak-fit one at ~1.5x.
                eRate *= KinesisModifiers.enduranceRegenMultiplier(player);

                if (eRate > 0) {
                    endurance.setEndurance(endurance.getEndurance() + eRate);
                }
            }
        }

        // EXHAUSTION -> Slowness. Empty stamina counts; endurance only counts when enabled.
        boolean exhausted = stamina.getStamina() <= 0
                && (!IHConfig.ENABLE_ENDURANCE.get() || endurance.getEndurance() <= 0);
        if (exhausted) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        }
    }

    /**
     * Basic SLEEP QUALITY (0.1..1.0): sleeping while hungry, dehydrated, or at an
     * uncomfortable body temperature gives poorer endurance recovery. See
     * docs/PHYSIOLOGY_PLAN.md for the fuller planned model (whole-night accrual,
     * wake bonus, safety/comfort).
     */
    private static double sleepQuality(Player player) {
        double q = 1.0;
        q *= clamp(player.getFoodData().getFoodLevel() / 20.0, 0.3, 1.0);
        double hydration = player.getCapability(ThirstProvider.THIRST).resolve()
                .map(t -> t.getHydration() / t.getMaxHydration()).orElse(1.0);
        q *= clamp(hydration, 0.3, 1.0);
        double bodyTemp = TemperatureEvents.getEffectiveBodyTemperature(player).orElse(0.0);
        if (Math.abs(bodyTemp) > 40) q *= 0.5; // too hot/cold to sleep well
        return clamp(q, 0.1, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Free the per-player state when they leave, so the maps can't grow. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_STATE.remove(event.getEntity().getUUID());
        ExertionTracker.clear(event.getEntity());
    }

    /**
     * Spend {@code amount} of stamina; if stamina can't cover it, take the remainder
     * from endurance. Both setters clamp at [0, max]. Shared by all movement costs.
     */
    private static void drainStaminaThenEndurance(Stamina stamina, Endurance endurance, double amount) {
        double s = stamina.getStamina();
        // If the endurance reserve is disabled, drain stamina only (setter clamps at 0).
        if (s >= amount || !IHConfig.ENABLE_ENDURANCE.get()) {
            stamina.setStamina(s - amount);
            return;
        }
        stamina.setStamina(0);
        endurance.setEndurance(endurance.getEndurance() - (amount - s));
    }
}
