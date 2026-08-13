package com._27Mikael.immersiveHealth.physiology.exhaustion;


public class ExhaustionHandler {

  
  public void Exhaustion(double endurance, double stamina) {
    // limit 
  }

  /**
   *
   * Stamina exhaustion logic
   * @param isSitting
   * @return void
   */
  public boolean isExhausted(double stamina, boolean isSprinting, boolean isSitting, boolean isCrouching, boolean isJumping) {
    if (stamina == 0 && isSprinting) {
      // stamina regenerates if you sleep
      // initiate regeneration
      // stamina += recoveryIndex;
      return true;
    } else if (stamina == 0 && isSitting){
      // stamina regenerates if you eat
      // initiate regeneration
      // stamina += recoveryIndex;
      return true;
    } else if (stamina == 0 && isCrouching){
      // stamina regenerates if you sit
      // initiate regeneration
      // stamina += recoveryIndex;
      return true;
    } else if (stamina == 0 && isJumping) {
      // stamina regenerates if 
      return true;
    }
    return false;
  }


  /**
   * Stamina exhaustion logic
   * @param stamina
   * @return void
   */
  public void Exhausted(double stamina) {
    // TODO: Im
    if (stamina == 0) {
      // initiate the stamina system
    }
  }
}
