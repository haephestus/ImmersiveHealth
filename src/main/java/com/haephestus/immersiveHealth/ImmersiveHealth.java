package com.haephestus.immersiveHealth;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import org.slf4j.Logger;

import com.haephestus.immersiveHealth.physiology.Physiology;
import com.haephestus.immersiveHealth.events.PlayerActionRetriever;
import com.haephestus.immersiveHealth.events.PlayerAttributeRetriever;
import com.haephestus.immersiveHealth.events.PlayerEventUpdater;
import com.haephestus.immersiveHealth.compat.ParCoolCompat;
import com.haephestus.immersiveHealth.compat.parcool.ParCoolActionHandler;
import com.haephestus.immersiveHealth.config.IHConfig;

/**
 * ============================ ANNOTATION (read me) ============================
 * This is the mod's ENTRY POINT. Forge finds the @Mod annotation, sees the id
 * "immersivehealth", and calls the constructor below once at startup.
 *
 * WHAT ACTUALLY WORKS RIGHT NOW (the "vertical slice"):
 *   - capabilities/ModCapabilities.java   -> registers Stamina & Endurance
 *   - events/KinesisEvents.java           -> attaches Stamina to players and
 *                                            drains/regens it every tick, showing
 *                                            the value in the action bar.
 *   Those two classes use @Mod.EventBusSubscriber, so Forge wires them up
 *   AUTOMATICALLY. You do not need to touch the constructor below to enable them.
 *
 * WHAT IS OLD SCAFFOLDING (present but NOT driving gameplay yet):
 *   - PlayerActionRetriever / PlayerAttributeRetriever / PlayerEventUpdater
 *   - physiology/** (KinesisHandler, StaminaHandler, EnduranceHandler, Exhaustion)
 *   These are registered/constructed below but nothing ticks the KinesisHandler,
 *   so its drain/regen math never runs (see the note inside KinesisHandler.java).
 *   The next step is to move that logic into the working tick loop in KinesisEvents.
 * ============================================================================
 */
@Mod(ImmersiveHealth.MOD_ID)
public class ImmersiveHealth {
  public static final String MOD_ID = "immersivehealth";
  public static final Logger LOGGER = LogUtils.getLogger();
  private Physiology physiologySystem;
  private PlayerActionRetriever playerActionRetriever;
  private PlayerAttributeRetriever playerAttributeRetriever;
  private PlayerEventUpdater playerEventUpdater;

  public ImmersiveHealth() {
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    modEventBus.addListener(this::commonSetup);

    // Register our config. Forge creates/loads config/immersivehealth-common.toml
    // and keeps it in sync; read values via IHConfig.X.get(). See IHConfig.
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IHConfig.SPEC);

    MinecraftForge.EVENT_BUS.register(this);

    // OPTIONAL ParCool integration: parkour actions drain this mod's Stamina/Endurance
    // (and are blocked when exhausted). ParCoolActionEvent fires on the FORGE bus.
    // Guarded by isLoaded() so ParCool classes are only touched when ParCool is
    // actually installed — keeps ParCool a soft dependency.
    if (ParCoolCompat.isLoaded()) {
      MinecraftForge.EVENT_BUS.register(new ParCoolActionHandler());
    }

    // Initialize and register event handlers FIRST (so they can be used by physiology system)
    playerActionRetriever = new PlayerActionRetriever();
    playerAttributeRetriever = new PlayerAttributeRetriever();
    playerEventUpdater = new PlayerEventUpdater();
    
    // Register event handlers with Forge event bus
    MinecraftForge.EVENT_BUS.register(playerActionRetriever);
    MinecraftForge.EVENT_BUS.register(playerAttributeRetriever);
    MinecraftForge.EVENT_BUS.register(playerEventUpdater);
    
    // Initialize physiology system with the event handlers
    physiologySystem = new Physiology();

    // retrieve player events from server

    // pass the player events to their respective systems
    // kinesis systems
    // metabolism
    // muscular systems
  
    // pass the results off to the capability systems
    // 

  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    // Initialize physiology system
    physiologySystem.initialize();
    LOGGER.info("ImmersiveHealth common setup complete.");
  }

  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    LOGGER.info("ImmersiveHealth server starting.");
  }

  @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
  public static class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
      LOGGER.info("ImmersiveHealth client setup complete.");
    }
  }
  
  // Getters for other systems to access centralized event handlers
  public PlayerActionRetriever getPlayerActionRetriever() {
    return playerActionRetriever;
  }
  
  public PlayerAttributeRetriever getPlayerAttributeRetriever() {
    return playerAttributeRetriever;
  }
  
  public PlayerEventUpdater getPlayerEventUpdater() {
    return playerEventUpdater;
  }
  
  public Physiology getPhysiologySystem() {
    return physiologySystem;
  }
}
