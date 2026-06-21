# Fate Mod — Full Design Document
**Minecraft 1.21.1 | NeoForge | ModDevGradle**

---

## Overview

A NeoForge mod for Minecraft 1.21.1 where each player rolls their fate once per Minecraft day. The roll determines a set of potion effects (buffs and debuffs) that last for the entire day and cannot be removed by drinking milk. Effects are split into tiers, with a secret 6th tier themed around Mahjong hands.

---

## Trigger Conditions

- **On first login of the day**: If the player has not yet rolled for the current in-game day, the roll triggers immediately on login.
- **On day tick**: If the player is online when the in-game day changes (world time crosses tick 0 of a new day), the roll triggers automatically.
- The in-game day number is tracked via `world.getDayTime() / 24000`.
- Each player rolls **independently** — there is no shared fate.

### Per-Player Persistent Data to Store
- `lastRolledDay` — the last in-game day the player rolled
- `currentRoll` — the tier and effects they received (persists through relog)
- `activeEffects` — the fate effects currently applied

---

## Tier System

There are 5 standard tiers and 1 secret tier:

| Tier | Chance |
|---|---|
| Neutral | 35% |
| Good | 25% |
| Bad | 25% |
| Godtier | 6% |
| Hell | 6% |
| Mahjong (secret) | 3% |

---

## Effect Pools

### Godtier
- Dolphins Grace
- Haste
- Resistance
- Regeneration

### Good
- Hero of the Village
- Invisibility
- Night Vision
- Fire Resistance
- Speed
- Conduit Power
- Strength

### Neutral
- Jump Boost
- Luck
- Glowing
- Slow Falling
- Water Breathing (infinite duration)
- Bad Luck

### Bad
- Hunger
- Bad Omen
- Slowness

### Hell
- Mining Fatigue
- Infested
- Blindness
- Darkness

---

## Roll Logic

### Step 1 — Roll the tier
Weighted random draw from the tier table above.

### Step 2 — If Mahjong, look up the hand
Skip all further steps. Apply the predefined hand directly (see Mahjong Hands section).

### Step 3 — Roll the effect count
Each tier has a weighted count distribution:

| Tier | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| Godtier | — | Common | Common | Rare |
| Good | Common | Common | Rare | — |
| Neutral | Common | Common | Rare | — |
| Bad | Common | Common | Rare | — |
| Hell | — | Common | Common | Rare |

### Step 4 — Draw effects
- Draw **1 guaranteed effect** from the rolled tier's pool (the anchor effect).
- Fill remaining slots using a **weighted neighbor draw** based on the rolled tier.

#### Neighbor weight table (rolled tier → draw weights per tier):

| Rolled Tier | Godtier | Good | Neutral | Bad | Hell |
|---|---|---|---|---|---|
| Godtier | High | Medium | Low | None | None |
| Good | Low | High | Medium | Low | None |
| Neutral | None | Low | High | Low | None |
| Bad | None | Low | Medium | High | Low |
| Hell | None | None | Low | Medium | High |

Godtier and Hell do not bleed into each other or their opposite extremes.

### Step 5 — Apply effects
- All fate effects are applied as permanent potion effects for the duration of the day.
- Fate effects are **milk-proof** — drinking milk must not remove them. Preferred implementation: hook into the milk use event and cancel removal for fate-tagged effects specifically (cleaner than re-applying after removal).
- Effects persist through relog until the next day tick triggers a new roll.

---

## Mahjong Hands

Mahjong is a secret 6th tier. When rolled, a specific predefined hand is selected and applied directly, bypassing the standard roll logic entirely. The hand name and flavor text are revealed dramatically during the reveal animation.

| Hand | Effects |
|---|---|
| Nine Gates | 9 random effects drawn equally from Godtier, Good, and Neutral pools |
| Thirteen Orphans | 13 random effects drawn equally from all pools, with Glowing guaranteed |
| Under the Sea | Dolphins Grace, Conduit Power, Night Vision |
| All Terminals | Blindness, Glowing |
| Tsumo | Fire Resistance, Luck, Haste |
| Four Concealed Triplets | Strength, Speed, Haste, Resistance |
| All Green Imperial Jade | Every Godtier effect + every Good effect |
| Riichi | Regeneration, Haste |
| Chanta | Slow Falling, Water Breathing, Jump Boost |
| Mixed Triple Sequence | 1 random effect drawn from each of the 5 standard tiers |

---

## Reveal Animation

The reveal plays client-side when the roll triggers. Effects are determined server-side first; the animation is purely visual. Effects are only applied once the animation completes.

### Phase 1 — Tier Roll
- Screen darkens slightly (vignette effect).
- A low hum/wind sound plays as buildup.
- All 6 tier names cycle rapidly on screen in their respective colors.
- The cycling slows and lands on the rolled tier.
- The tier name is displayed prominently with its flavor text and a tier-specific sound sting.

### Tier Colors
| Tier | Color |
|---|---|
| Godtier | Gold |
| Good | Green |
| Neutral | White / Gray |
| Bad | Orange |
| Hell | Dark Red |
| Mahjong | Deep Purple or Teal |

### Tier Flavor Text
| Tier | Flavor Text |
|---|---|
| Godtier | *"The stars align in your favor."* |
| Good | *"Fortune smiles upon you today."* |
| Neutral | *"The world asks nothing of you today."* |
| Bad | *"Today will test your patience."* |
| Hell | *"Fate has chosen to make an example of you."* |
| Mahjong | Unique per hand (see below) |

### Phase 2 — Effect Reveal
- Effects appear one by one as **icon + name**.
- Each effect appearance plays a small sound.
- For Mahjong hands, each effect plays a tile slam sound instead.
- Once all effects are shown, a brief pause before the animation clears.

### Mahjong Hand Flavor Text
| Hand | Flavor Text |
|---|---|
| Nine Gates | *"The heavens open their gates for you."* |
| Thirteen Orphans | *"Chaos claims you as its own."* |
| Under the Sea | *"The ocean calls your name."* |
| All Terminals | *"You see everything and nothing."* |
| Tsumo | *"A self-drawn victory."* |
| Four Concealed Triplets | *"Power, concealed and absolute."* |
| All Green Imperial Jade | *"Blessed by the jade emperor."* |
| Riichi | *"You have declared your hand."* |
| Chanta | *"Drift, float, and wander."* |
| Mixed Triple Sequence | *"All paths converge on you."* |

### Sound Design
| Moment | Sound |
|---|---|
| Buildup | Low hum / wind |
| Tier cycling | Fast ticking |
| Godtier sting | Ascending angelic chime |
| Good sting | Upbeat jingle |
| Neutral sting | Single neutral chime |
| Bad sting | Low descending tone |
| Hell sting | Distorted ominous hit |
| Effect appearance | Soft click |
| Mahjong effect appearance | Hard percussive tile slam |
| Mahjong hand reveal | Unique dramatic sting |

### Suspense Mechanic
During Phase 1, before the roll lands, there is a chance of a suspense event:

| Outcome | Chance |
|---|---|
| Normal roll | 70% |
| False landing | 20% |
| Slow crawl | 10% |

- **False landing**: The cycling appears to stop on a tier, lingers for a beat, then ticks over one more time to the actual result.
- **Slow crawl**: The last 1–2 tier changes happen in slow motion before finally settling.
- The actual result is determined before the animation plays. The suspense mechanic is purely visual and does not affect the outcome.
- False landing and slow crawl are mutually exclusive per roll.

---

## Server Chat Announcements

When a player rolls **Godtier**, **Hell**, or **Mahjong**, a public message is broadcast to the entire server via chat. Neutral, Good, and Bad rolls are not announced.

Messages are sent under the name **CityChan** and use colored text matching the tier colors defined above.

### Message Format

At the start of each new in-game day, a day number message is broadcast to all online players:
```
[CityChan] Day <x>
```

Then, as players roll, individual announcements follow:
```
[CityChan] <player> rolled Godtier.
[CityChan] <player> rolled Hell.
[CityChan] <player> rolled Mahjong ---> <Hand Name>
```

### Example Sequence
```
[CityChan] Day 47
[CityChan] Steve rolled Hell.
[CityChan] Alex rolled Mahjong ---> Nine Gates
[CityChan] Notch rolled Godtier.
```

### Color Rules
- `[CityChan]` — always displayed in the Mahjong tier color (Deep Purple or Teal)
- `Day <x>` — white
- Player name — white
- `rolled` — white
- `Godtier` — Gold
- `Hell` — Dark Red
- `Mahjong` — Deep Purple or Teal
- `---> <Hand Name>` — Gold

### Timing
- The day announcement broadcasts at tick 0 of the new day (the same moment rolls are triggered for online players).
- Player roll announcements broadcast server-side as soon as the roll is determined, before the reveal animation plays on the client.

---

## Summary of Key Rules
- One roll per player per in-game day.
- Rolls are independent per player.
- Fate effects last the full day and are milk-proof.
- Mahjong bypasses standard roll logic entirely.
- Effects are only applied after the reveal animation completes.
- Water Breathing is applied at infinite duration when rolled.
