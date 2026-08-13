package com._27Mikael.immersiveHealth.physiology.kinesis.stamina;

import com._27Mikael.immersiveHealth.events.PlayerActionRetriever;


public class StaminaHandler {

  private double stamina;
  private double maxStamina = 100.0;

  // actions - to be injected by the system
  private PlayerActionRetriever action;

  public StaminaHandler(double stamina) {
    this.stamina = stamina;
  }
  
  // Method to inject the centralized action retriever
  public void setActionRetriever(PlayerActionRetriever actionRetriever) {
    this.action = actionRetriever;
  }

  public double getStamina() {
    return stamina;
  }

  public void setStamina(double stamina) {
    this.stamina = Math.max(0, Math.min(maxStamina, stamina));
  }

  public double getMaxStamina() {
    return maxStamina;
  }

  public double setMaxStamina(double maxStamina) {
    this.maxStamina = maxStamina;
      return maxStamina;
  }

  /**
   *  Check if player can perform regular movement actions
   *  @return true if stamina > 0
   */
  public boolean canPerformMovement() {
    return stamina > 0;
  }

  /**
   * Stamina regeneration logic
   * @param stamina current stamina value
   */
  public void isRegenerating(double stamina) {
    // TODO: Implement Musculature.recoveryIndex
    // double Musculature.recoveryIndex = recoveryIndex;
    if (stamina <= 0) {
      // stamina += recoveryIndex;
      setStamina(stamina + 1.0f);
    }
  }

  /**
   * Stamina drain logic for running
   * @param stamina Retrieves stamina
   * @return newStamina
   */
  public double SprintState(double stamina) {
    // TODO: Implement sprintIndex
    // double Musculature.sprintIndex = sprintIndex;
    if (action != null && action.isSprinting() && stamina > 0) {
      double newStamina =  stamina - 2f;
      setStamina(newStamina);
      return newStamina;
    }
    return stamina;
  }

  /**
   * Stamina drain logic for jumping
   * @param stamina Retrieves stamina
   * @return newStamina
   */
  public double JumpingState(double stamina) {
    // TODO: Implement jumpIndex
    // double Musculature.jumpIndex = jumpIndex;
    if (action != null && action.isJumping() && stamina > 0) {
      double newStamina = stamina - 2f;
      setStamina(newStamina);
      return newStamina;
    }
    return stamina;
  }
 

  /**
   * Stamina drain logic for crouching
   * @param stamina Retrieves current stamina
   * @return newStamina
   */
  public double CrouchingState(double stamina) {
    // TODO: Implement recoveryIndex
    // double Musculature.recoveryIndex = recoveryIndex;
    if (action != null && action.isCrouching()) {
      double newStamina = stamina + 2f;
      setStamina(newStamina);
      return newStamina;
    }
    return stamina;
  }

  /**
   * Calculate parkour cost modifier based on stamina level
   * @return cost multiplier (1.0 = normal, higher = more expensive)
   */
  public double getParkourCostModifier() { 
    if (stamina <= 0 ) {
      return 2.0;
    } else if (stamina < (maxStamina * 0.3)) {
      return 1.5;
    }
    return 1.0;
  }

  /**
   * Get stamina percentage for UI or other systems
   * @ return percentage from 0.0. to 1.0
   */
  public double getStaminaPercentage() {
    return stamina / maxStamina;
  }
}


