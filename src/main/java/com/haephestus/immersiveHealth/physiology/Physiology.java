package com.haephestus.immersiveHealth.physiology;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * ============================ ANNOTATION (read me) ============================
 * This is meant to be the top-level ORCHESTRATOR ("run all body systems each tick:
 * kinesis, metabolism, muscles, ..."), but right now it only logs a line and does
 * nothing else. It has no per-tick update method and holds no player data.
 *
 * A realistic future shape: Physiology.update(Player) is called once per tick from
 * the KinesisEvents tick loop, and it in turn updates each sub-system using that
 * player's capability data. For the current slice you can ignore this class.
 * ============================================================================
 */
public class Physiology {
  public static final Logger LOGGER = LogUtils.getLogger();

  public Physiology() {
    // retrieve player events from server

    // pass the player events to their respective systems
    // kinesis systems
    // metabolism
    // muscular systems
  
    // pass the results off to the capability systems
    // 

  }

  public void initialize() {
    LOGGER.info("Physiology system initialized.");
  }
}
