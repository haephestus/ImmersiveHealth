# Musculature & Metabolism — Design Plan

This plans the two systems that feed **stamina drain rate** and **endurance regen**.
They plug in through **one seam**: `physiology/KinesisModifiers.java`. The Kinesis
tick (`events/KinesisEvents.java`) already multiplies every drain/regen number by:

- `staminaDrainMultiplier(player)`
- `staminaRegenMultiplier(player)`
- `enduranceRegenMultiplier(player)`

Today those return `1.0`. Implementing the two systems below = filling those three
methods in. **The tick loop never changes.**

---

## 1. Musculature — "how strong/conditioned are the muscles"

Physical conditioning. Trained muscles spend less energy per action, recover faster,
and raise the stamina ceiling — but fatigue when overworked.

### Data (`capabilities/Musculature` + provider, NBT-saved, like Stamina)
| field | range | meaning |
|---|---|---|
| `conditioning` | 0–100 | long-term fitness. Grows slowly with exertion, atrophies with disuse. |
| `fatigue` | 0–100 | short-term muscle fatigue. Rises with heavy use, falls with rest. |

### Derived indices (the "sprintIndex/jumpIndex/recoveryIndex" from the old TODOs)
```
strength   = conditioning / 100                 // 0..1
efficiency = 0.6 + 0.4 * strength               // fit players spend 60–100% of base cost
fatiguePenalty = 1 + (fatigue / 100)            // tired muscles cost up to 2x
recoveryIndex  = 0.5 + strength                 // fit players regen 0.5x..1.5x faster
```

### Effects
- **Stamina drain** ← `efficiency * fatiguePenalty` (fitness lowers cost, fatigue raises it).
- **Stamina/Endurance regen** ← `recoveryIndex`.
- **Max stamina/endurance**: raise the caps with `conditioning` (e.g. `maxStamina = 80 + 0.4*conditioning`). Set via the capability's `setMaxStamina`.

### Progression (in the Kinesis tick, cheap)
- Sustained sprint/jump/parkour → `conditioning += tiny` (progressive overload), and `fatigue += small`.
- Idle/rest → `fatigue -= small`; long disuse → `conditioning -= tiny` (atrophy).
- Overtraining: while `fatigue` is high, drain is worse (the penalty above) — a natural soft cap.

---

## 2. Metabolism — "how much fuel is available"

Energy budget from nutrition + hydration. Sets whether the body *can afford* to
regenerate and how expensive movement is. This is where **food, thirst, and
temperature** all converge.

### Data (`capabilities/Metabolism` + provider)
| field | range | meaning |
|---|---|---|
| `energy` | 0–100 | usable fuel right now. |
| `burnRate` | — | derived; how fast energy is consumed. |

### Fuel inputs (read each tick — no new storage needed for these)
- Vanilla **hunger + saturation**: `player.getFoodData().getFoodLevel()/getSaturationLevel()`.
- **Thirst**: our `Thirst` capability (dehydration throttles metabolism).
- **Body temperature**: cold burns more fuel (shivering) — read via
  `TemperatureEvents.getEffectiveBodyTemperature` (Cold Sweat or our fallback).

```
fuel        = 0.5*(food/20) + 0.3*(saturation/20) + 0.2*(hydration/max)   // 0..1
coldBurn    = bodyTemp < 0 ? 1 + (-bodyTemp/200) : 1                        // cold => burn more
energyFactor= clamp(fuel, 0.2, 1.2)
```

### Effects
- **Endurance regen** ← `energyFactor` (well-fed & hydrated => real recovery; starving => almost none). This is metabolism's *primary* job.
- **Stamina drain** ← `coldBurn` and a small penalty when `fuel` is very low (running on empty is inefficient).
- **Stamina regen** ← mild `energyFactor` bonus.

---

## 3. What `KinesisModifiers` becomes

```java
staminaDrainMultiplier   = musc.efficiency * musc.fatiguePenalty * metab.coldBurn * metab.lowFuelPenalty
staminaRegenMultiplier   = musc.recoveryIndex * lerp(0.5,1.0, metab.energyFactor)
enduranceRegenMultiplier = metab.energyFactor * (rested ? 1.5 : 1.0)   // rest = sitting/sleeping
```
All clamped to sane bounds (e.g. drain 0.5x–2.5x, regen 0x–2x).

---

## 4. Build order (each step independently testable)
1. `Musculature` capability + provider + register + attach/clone (copy the Stamina files).
2. Fill `staminaDrainMultiplier` from musculature only; verify fit vs unfit players.
3. Add musculature progression in the Kinesis tick (grow/atrophy/fatigue).
4. `Metabolism` capability; read vanilla food + our thirst + body temp.
5. Fill `enduranceRegenMultiplier` (metabolism) and finish `staminaDrainMultiplier`.
6. Tie sitting/sleeping "rest" bonus into regen.
7. Show musculature/metabolism in `DebugHudEvents`.

---

## Endurance refill model (IMPLEMENTED)

Endurance is a **deep reserve**. In `KinesisEvents` (all rates in config → `kinesis.endurance`):

- **Passive trickle**: whenever stamina is *full*, endurance creeps back by
  `fullStaminaTricklePerTick` (default 0.02). Always on when you're topped up.
- **Real recovery** (bigger), gated behind `restDelayTicks` of NO strenuous activity
  (sprint/jump/parkour/combat all reset the timer via `ExertionTracker`):
  - **Sleeping** → `sleepRegenPerTick` × **sleep quality**.
  - **Resting** (awake, standing still) → `restRegenPerTick`.
  - **Eating** (vanilla saturation > 0) → `+ eatRegenBonusPerTick`.
- All of the above is multiplied by `KinesisModifiers.enduranceRegenMultiplier`
  (the FITNESS/metabolism seam; planned `0.5 + fitness`).

## Sleep Quality

**Implemented (basic):** `sleepQuality(player)` ∈ [0.1, 1.0] scales sleep recovery:
```
q  = clamp(foodLevel/20, 0.3, 1)          // hungry => sleep poorly
q *= clamp(hydration/max, 0.3, 1)         // thirsty => sleep poorly
q *= (|bodyTemp| > 40 ? 0.5 : 1)          // too hot/cold => sleep poorly
```

**Planned (fuller model):**
- Accrue quality across the *whole night* (uninterrupted sleep is better than
  repeatedly getting in/out of bed); grant a **wake bonus** to endurance/max-stamina
  in the morning proportional to accrued quality.
- **Comfort**: bed vs floor, roof over head, light/mob safety, nearby campfire warmth.
- **Temperature**: pull real comfort range from Cold Sweat when present.
- Poor sleep could leave a lingering "tired" debuff (reduced `staminaRegenMultiplier`).

## 5. Open tuning questions
- Real numbers for grow/atrophy rates (how many hours of play to gain conditioning?).
- Should max-stamina growth be visible/announced to the player?
- Hard-link to Cold Sweat body temp for `coldBurn`, or keep our fallback sufficient?
- Do muscle injuries (future Anatomy system) cap `conditioning`?
