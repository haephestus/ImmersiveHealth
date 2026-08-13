package com._27Mikael.immersiveHealth.compat;

import net.minecraftforge.fml.ModList;

/**
 * SOFT (optional) awareness of the "Better Combat" mod (by ZsoltMolnarrr).
 *
 * IMPORTANT: the combat stamina drain in events/CombatEvents does NOT require this
 * class or Better Combat at all — it uses vanilla Forge attack events, which Better
 * Combat routes through. So combat drains stamina with or without Better Combat
 * installed (you can test it by punching mobs in the vanilla dev client).
 *
 * This class exists only for the ONE feature that needs Better Combat's own API:
 * draining on a SWING THAT MISSES (server-side). Better Combat exposes attack
 * callbacks under `net.bettercombat.api.event` (see `Publisher`) and combo/attack
 * info via `net.bettercombat.api.ComboState` + `EntityPlayer_BetterCombat`. When
 * you want miss-draining or per-combo-step costs, add a `compileOnly` Better Combat
 * dep and register a listener here, guarded by isLoaded(), then call the same
 * drain helper CombatEvents uses.
 *
 * Verified target: Better Combat Forge 1.8.6+1.20.1 (mod id "bettercombat").
 */
public final class BetterCombatCompat {

    public static final String MOD_ID = "bettercombat";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);

    private BetterCombatCompat() {}

    public static boolean isLoaded() {
        return LOADED;
    }
}
