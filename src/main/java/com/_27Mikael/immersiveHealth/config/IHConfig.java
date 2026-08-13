package com._27Mikael.immersiveHealth.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The mod's configuration.
 *
 * HOW FORGE CONFIG WORKS (so you can extend it):
 *   1. Declare a static ForgeConfigSpec value for each option, built with a Builder.
 *   2. Builder.push("category") / .pop() groups options into TOML tables.
 *   3. .comment(...).define(...) / .defineInRange(...) creates one option.
 *   4. Build the SPEC once (static block).
 *   5. Register SPEC in the mod constructor (see ImmersiveHealth):
 *        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IHConfig.SPEC);
 *   6. Read a live value anywhere with e.g. IHConfig.SPRINT_DRAIN.get().
 *
 * Forge writes/reads the file at:  <instance>/config/immersivehealth-common.toml
 * Values reload when that file changes, so always call .get() at the point of use
 * (don't cache into a `static final`).
 *
 * To add an option: copy a line into the right push/pop block. To make a whole new
 * system configurable (e.g. combat/parkour costs, still constants in their event
 * classes), add a push("combat") block here and swap those constants for .get().
 */
public final class IHConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- systems: master on/off switches ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_STAMINA;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDURANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HYDRATION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BODY_TEMP;

    // ---- kinesis: movement stamina costs ----
    public static final ForgeConfigSpec.DoubleValue SPRINT_DRAIN;
    public static final ForgeConfigSpec.DoubleValue WALK_DRAIN;
    public static final ForgeConfigSpec.DoubleValue JUMP_COST;
    public static final ForgeConfigSpec.DoubleValue STAMINA_REGEN;

    // ---- kinesis: endurance (deep reserve) ----
    public static final ForgeConfigSpec.IntValue REST_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue ENDURANCE_SLEEP_REGEN;
    public static final ForgeConfigSpec.DoubleValue ENDURANCE_REST_REGEN;
    public static final ForgeConfigSpec.DoubleValue ENDURANCE_EAT_BONUS;
    public static final ForgeConfigSpec.DoubleValue ENDURANCE_FULL_STAMINA_TRICKLE;

    // ---- thirst / hydration ----
    public static final ForgeConfigSpec.DoubleValue THIRST_BASE_DRAIN;
    public static final ForgeConfigSpec.DoubleValue WATER_BOTTLE_HYDRATION;
    public static final ForgeConfigSpec.DoubleValue OPEN_WATER_HYDRATION;
    public static final ForgeConfigSpec.DoubleValue DIRTY_WATER_SICKNESS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue OPEN_WATER_SICKNESS_CHANCE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Turn individual physiology systems on/off. Disabled systems do nothing",
                        "and stop applying their effects (their bar just sits idle).")
                .push("systems");
        ENABLE_STAMINA = b.comment("Stamina (movement/combat/parkour drain).")
                .define("enableStamina", true);
        ENABLE_ENDURANCE = b.comment("Endurance deep-reserve (drains after stamina, refills via rest).",
                        "If false, stamina is used alone with no reserve.")
                .define("enableEndurance", true);
        ENABLE_HYDRATION = b.comment("Thirst / hydration.")
                .define("enableHydration", true);
        ENABLE_BODY_TEMP = b.comment("Body temperature & fever effects.")
                .define("enableBodyTemperature", true);
        b.pop();

        b.comment("Kinesis: stamina & endurance").push("kinesis");

        SPRINT_DRAIN = b.comment("Stamina drained per tick while sprinting")
                .defineInRange("sprintDrainPerTick", 0.5, 0.0, 1000.0);
        WALK_DRAIN = b.comment("Stamina drained per tick while walking")
                .defineInRange("walkDrainPerTick", 0.05, 0.0, 1000.0);
        JUMP_COST = b.comment("Stamina spent per jump")
                .defineInRange("jumpCost", 5.0, 0.0, 1000.0);
        STAMINA_REGEN = b.comment("Stamina recovered per tick while not moving")
                .defineInRange("staminaRegenPerTick", 0.25, 0.0, 1000.0);

        b.comment("Endurance is a DEEP RESERVE. It recovers mainly via sleep, eating and",
                        "rest (no strenuous activity for restDelayTicks), plus a slow passive",
                        "trickle whenever stamina is full. All of it scales with fitness.")
                .push("endurance");
        REST_DELAY_TICKS = b.comment("Ticks of no strenuous activity before rest/sleep/eat recovery kicks in (20 = 1s)")
                .defineInRange("restDelayTicks", 100, 0, 100000);
        ENDURANCE_SLEEP_REGEN = b.comment("Endurance/tick while sleeping (x sleep quality)")
                .defineInRange("sleepRegenPerTick", 0.5, 0.0, 1000.0);
        ENDURANCE_REST_REGEN = b.comment("Endurance/tick while resting (still, awake)")
                .defineInRange("restRegenPerTick", 0.05, 0.0, 1000.0);
        ENDURANCE_EAT_BONUS = b.comment("Extra endurance/tick while digesting food (saturation > 0)")
                .defineInRange("eatRegenBonusPerTick", 0.1, 0.0, 1000.0);
        ENDURANCE_FULL_STAMINA_TRICKLE = b.comment("Passive endurance/tick whenever stamina is full (slow trickle)")
                .defineInRange("fullStaminaTricklePerTick", 0.02, 0.0, 1000.0);
        b.pop(); // endurance

        b.pop(); // kinesis

        b.comment("Thirst / hydration").push("thirst");
        THIRST_BASE_DRAIN = b.comment("Hydration lost per tick just from being alive")
                .defineInRange("baseDrainPerTick", 0.01, 0.0, 1000.0);
        WATER_BOTTLE_HYDRATION = b.comment("Hydration restored by drinking a water bottle")
                .defineInRange("waterBottleHydration", 30.0, 0.0, 1000.0);
        OPEN_WATER_HYDRATION = b.comment("Hydration per sip from open water (sneak + right-click)")
                .defineInRange("openWaterHydration", 15.0, 0.0, 1000.0);
        DIRTY_WATER_SICKNESS_CHANCE = b.comment("Chance a water bottle makes you ill (0..1)")
                .defineInRange("dirtyWaterSicknessChance", 0.30, 0.0, 1.0);
        OPEN_WATER_SICKNESS_CHANCE = b.comment("Chance open water makes you ill (0..1)")
                .defineInRange("openWaterSicknessChance", 0.55, 0.0, 1.0);
        b.pop(); // thirst

        SPEC = b.build();
    }

    private IHConfig() {}
}
