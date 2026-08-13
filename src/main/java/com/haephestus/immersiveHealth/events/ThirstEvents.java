package com.haephestus.immersiveHealth.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.haephestus.immersiveHealth.ImmersiveHealth;
import com.haephestus.immersiveHealth.capabilities.ThirstProvider;
import com.haephestus.immersiveHealth.compat.ColdSweatCompat;
import com.haephestus.immersiveHealth.config.IHConfig;

/**
 * The THIRST system engine — same three-part lifecycle as KinesisEvents
 * (attach -> keep across death -> tick), plus the optional Cold Sweat hookup.
 *
 * Thirst works completely on its own. Cold Sweat only *modulates* the drain rate
 * via ColdSweatCompat, which returns "neutral" (1.0x) whenever Cold Sweat is not
 * installed. So this system is fully functional as a standalone feature.
 *
 * NOTE: display of the value is handled centrally in DebugHudEvents so the systems
 * don't fight over the single action-bar line. This class only mutates state.
 */
@Mod.EventBusSubscriber(modid = ImmersiveHealth.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThirstEvents {

    // All thirst tuning knobs now live in IHConfig (config/immersivehealth-common.toml).

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(ImmersiveHealth.MOD_ID, "thirst"),
                    new ThirstProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ThirstProvider.THIRST).ifPresent(oldThirst ->
                event.getEntity().getCapability(ThirstProvider.THIRST).ifPresent(newThirst ->
                        newThirst.setHydration(oldThirst.getHydration())));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return; // server owns the truth
        if (!IHConfig.ENABLE_HYDRATION.get()) return; // hydration system disabled in config

        player.getCapability(ThirstProvider.THIRST).ifPresent(thirst -> {
            double drain = IHConfig.THIRST_BASE_DRAIN.get();

            // Moving around costs extra water; sprinting most of all.
            if (player.isSprinting()) {
                drain *= 3.0;
            } else if (player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                drain *= 1.5;
            }

            // OPTIONAL Cold Sweat influence: hot => sweat => faster drain.
            // Returns 1.0 (no change) when Cold Sweat is absent.
            drain *= ColdSweatCompat.thirstDrainMultiplier(player);

            thirst.setHydration(thirst.getHydration() - drain);

            // Dehydration penalty: when the tank is empty, sap the player with
            // Weakness + Nausea (dizziness) — re-applied so it lasts while empty.
            if (thirst.getHydration() <= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            }
        });
    }

    /**
     * DRINKING: when a player finishes drinking a plain water bottle, restore
     * hydration. Untreated ("dirty") water risks illness — purified water (a later
     * custom item) would set a flag to skip the sickness roll.
     *
     * We use LivingEntityUseItemEvent.Finish so it fires once, when the drink
     * animation completes, on the server.
     */
    @SubscribeEvent
    public static void onFinishDrinking(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        ItemStack stack = event.getItem();
        // Is it a plain water bottle? (Potion item whose potion is WATER.)
        boolean isWaterBottle = stack.getItem() instanceof PotionItem
                && PotionUtils.getPotion(stack) == Potions.WATER;
        if (!isWaterBottle) return;

        player.getCapability(ThirstProvider.THIRST).ifPresent(thirst ->
                thirst.setHydration(thirst.getHydration() + IHConfig.WATER_BOTTLE_HYDRATION.get()));

        // Dirty-water risk. TODO(next): treat purified-water items as clean (skip this).
        if (player.getRandom().nextFloat() < IHConfig.DIRTY_WATER_SICKNESS_CHANCE.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
    }

    /**
     * DRINK FROM OPEN WATER: sneak + right-click while looking at a water source
     * takes a sip. We handle both the "clicked item" and "clicked block" cases so
     * it works whether or not there's a solid block behind the water. A fluid-aware
     * ray trace confirms you're actually aimed at water.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryDrinkFromWater(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryDrinkFromWater(event.getEntity())) {
            event.setCanceled(true); // we drank; don't also run the block interaction
        }
    }

    private static boolean tryDrinkFromWater(Player player) {
        if (player.level().isClientSide) return false;
        if (!player.isCrouching()) return false; // sneak + right-click = deliberate drink

        // Ray trace INCLUDING fluids so we can actually hit a water surface.
        HitResult hit = player.pick(4.5D, 0f, true);
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockHitResult blockHit = (BlockHitResult) hit;
        if (!player.level().getFluidState(blockHit.getBlockPos()).is(FluidTags.WATER)) return false;

        var thirstOpt = player.getCapability(ThirstProvider.THIRST).resolve();
        if (thirstOpt.isEmpty()) return false;
        var thirst = thirstOpt.get();
        if (thirst.getHydration() >= thirst.getMaxHydration()) return false; // already full

        thirst.setHydration(thirst.getHydration() + IHConfig.OPEN_WATER_HYDRATION.get());
        player.swing(InteractionHand.MAIN_HAND);

        if (player.getRandom().nextFloat() < IHConfig.OPEN_WATER_SICKNESS_CHANCE.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        return true;
    }
}
