package com._27Mikael.immersiveHealth.physiology.kinesis.endurance;

import com._27Mikael.immersiveHealth.events.PlayerAttributeRetriever;
import com._27Mikael.immersiveHealth.events.PlayerActionRetriever;

public class EnduranceHandler {

  // attributes
  private double endurance;
  private double maxEndurance = 100.0;
  private PlayerAttributeRetriever attributes;

  // actions - to be injected by the system
  private PlayerActionRetriever action;

  public EnduranceHandler(double endurance) {
    this.endurance = endurance;
  }
  
  // Methods to inject the centralized retrievers
  public void setActionRetriever(PlayerActionRetriever actionRetriever) {
    this.action = actionRetriever;
  }
  
  public void setAttributeRetriever(PlayerAttributeRetriever attributeRetriever) {
    this.attributes = attributeRetriever;
  }

  // Getters and setters 
  public double getEndurance() {
    return endurance;
  }

  public void setEndurance(double endurance) {
    this.endurance = Math.max(0, Math.min(maxEndurance, endurance));
  }

  public double getMaxEndurance() {
    return maxEndurance;
  }

  public double setMaxEndurance(double maxEndurance) {
    this.maxEndurance = maxEndurance;
      return maxEndurance;
  }

  public double getEndurancePercentage() {
    return endurance / maxEndurance;
  }

  /**
   * Endurance regeneration logic
   * @param currentEndurance current endurance value
   * @return boolean indicating if regenerating
   */
  public boolean isRegenerating(double currentEndurance) {
    // TODO: Implement Musculature.recoveryIndex
    // double Musculature.recoveryIndex = recoveryIndex;
    if (action == null) return false;
    
    if (currentEndurance <= 0 && !action.isSprinting()) {
      // endurance regenerates if you dont run
      setEndurance(currentEndurance + 1.0);
      return true;
    } else if (currentEndurance <= 0 && action.isSitting()) {
      // endurance regenerates if you are sat
      setEndurance(currentEndurance + 2.0);
      return true;
    } else if (currentEndurance <= 0 && action.isCrouching()) {
      // endurance regenerates if you crouch  
      setEndurance(currentEndurance + 1.5);
      return true;
    } else if (currentEndurance <= 0 && !action.isJumping()) {
      // endurance regenerates you dont jump
      setEndurance(currentEndurance + 0.5);
      return true;
    }
    return false;
  }

  /**
   * Endurance drain logic for running
   * @param currentEndurance current endurance value
   * @return newEndurance
   */
  public double SprintState(double currentEndurance) {
    // TODO: Implement sprintIndex
    // double Musculature.sprintIndex = sprintIndex;
    if (action != null && action.isSprinting() && currentEndurance > 0) {
      double newEndurance = currentEndurance - 2f;
      setEndurance(newEndurance);
      // Apply movement speed penalty when running on endurance
      if (attributes != null) {
        attributes.getMovementSpeed(); // Reduced speed
      }
      return newEndurance;
    }
    return currentEndurance;
  }

  /**
   * Endurance drain logic for jumping
   * @param currentEndurance current endurance value
   * @return newEndurance
   */
  public double JumpingState(double currentEndurance) {
    // TODO: Implement jumpIndex
    // double Musculature.jumpIndex = jumpIndex;
    if (action != null && action.isJumping() && currentEndurance > 0) {
      double newEndurance = currentEndurance - 2f;
      setEndurance(newEndurance);
      return newEndurance;
    }
    return currentEndurance;
  }

  /**
   * Endurance drain logic for crouching
   * @param currentEndurance current endurance value  
   * @return newEndurance
   */
  public double CrouchingState(double currentEndurance) {
    // TODO: Implement recoveryIndex
    // double Musculature.recoveryIndex = recoveryIndex;
    if (action != null && action.isCrouching()) {
      double newEndurance = currentEndurance + 2f;
      setEndurance(newEndurance);
      return newEndurance;
    }
    return currentEndurance;
  }
}
