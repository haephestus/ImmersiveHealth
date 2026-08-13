package com._27Mikael.immersiveHealth.events;

import net.minecraftforge.event.TickEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerActionRetriever {

  private boolean isSitting;
  private boolean isJumping;
  private boolean isSleeping;
  private boolean isSprinting;
  private boolean isCrouching;

  private Player currentPlayer;

  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {

    // WARN: consider adding physics based movement detection.
    // getDeltaMovement().length() > 0.1
  
    this.currentPlayer = event.player;

    isSitting = currentPlayer.isPassenger();
    isSleeping = currentPlayer.isSleeping();
    isSprinting = currentPlayer.isSprinting();
    isCrouching = currentPlayer.isCrouching();

    if (currentPlayer instanceof LocalPlayer localplayer) {
      // Client-side: Direct input detection
        isJumping = localplayer.input.jumping;
    } else {
      // Server-side: Physics based detection  
      boolean wasOnGround = currentPlayer.onGround();
      boolean isMovingUp = currentPlayer.getDeltaMovement().y > 0.1;
      isJumping = !wasOnGround && isMovingUp;
    }

    // debug output
    // System.out.println("Jumping: " + isJumping + ", Sprinting: " + isSprinting);
  }

  public boolean isCrouching() {
      return isCrouching;
  }

  public boolean isJumping() {
      return isJumping;
  }

  public boolean isSprinting() {
      return isSprinting;
  }

  public boolean isSitting() {
      return isSitting;
  }
  
  public boolean isSleeping() {
    return isSleeping;
  }

  public Player getCurrentPlayer() {
    return currentPlayer;
  }
}
