package com._27Mikael.immersiveHealth.events;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public class PlayerEventUpdater {

  public PlayerEventUpdater() {
    //
  }

  @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
  public void onPlayerTick(LivingEvent.LivingTickEvent event) {
    // Check if event is player event and update 
  }
}
