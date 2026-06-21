package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.config.RNGdleConfig;
import com.krihcity.citychanrngdle.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FateEffectPools {

    // Hardcoded pools — used by Mahjong hands (not config-driven, intentionally fixed)
    public static final List<Holder<MobEffect>> GODTIER = List.of(
            MobEffects.DOLPHINS_GRACE,
            MobEffects.DIG_SPEED,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.REGENERATION,
            ModEffects.ENLIGHTENED
    );

    public static final List<Holder<MobEffect>> GOOD = List.of(
            MobEffects.HERO_OF_THE_VILLAGE,
            MobEffects.INVISIBILITY,
            MobEffects.NIGHT_VISION,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.CONDUIT_POWER,
            MobEffects.DAMAGE_BOOST,
            ModEffects.BOUNTIFUL_HARVEST
    );

    public static final List<Holder<MobEffect>> NEUTRAL = List.of(
            MobEffects.JUMP,
            MobEffects.LUCK,
            MobEffects.GLOWING,
            MobEffects.SLOW_FALLING,
            MobEffects.WATER_BREATHING,
            MobEffects.UNLUCK,
            MobEffects.WEAVING,
            MobEffects.WIND_CHARGED,
            MobEffects.OOZING,
            ModEffects.COLORBLIND,
            ModEffects.LIGHTWEIGHT
    );

    public static final List<Holder<MobEffect>> BAD = List.of(
            MobEffects.HUNGER,
            MobEffects.BAD_OMEN,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            ModEffects.SLIPPERY,
            ModEffects.FRAIL
    );

    public static final List<Holder<MobEffect>> HELL = List.of(
            MobEffects.DIG_SLOWDOWN,
            MobEffects.INFESTED,
            MobEffects.BLINDNESS,
            MobEffects.DARKNESS,
            ModEffects.BROKEN_LEGS,
            ModEffects.WEEPING_ANGEL
    );

    // ── Config name ↔ effect mapping ──────────────────────────────────────────

    public static final Map<String, Holder<MobEffect>> NAME_TO_EFFECT;
    private static final Map<Holder<MobEffect>, String> EFFECT_TO_NAME;

    static {
        Map<String, Holder<MobEffect>> n2e = new LinkedHashMap<>();
        n2e.put("DOLPHINS_GRACE",    MobEffects.DOLPHINS_GRACE);
        n2e.put("HASTE",             MobEffects.DIG_SPEED);
        n2e.put("RESISTANCE",        MobEffects.DAMAGE_RESISTANCE);
        n2e.put("REGENERATION",      MobEffects.REGENERATION);
        n2e.put("ENLIGHTENED",       ModEffects.ENLIGHTENED);
        n2e.put("HERO_OF_VILLAGE",   MobEffects.HERO_OF_THE_VILLAGE);
        n2e.put("INVISIBILITY",      MobEffects.INVISIBILITY);
        n2e.put("NIGHT_VISION",      MobEffects.NIGHT_VISION);
        n2e.put("FIRE_RESISTANCE",   MobEffects.FIRE_RESISTANCE);
        n2e.put("SPEED",             MobEffects.MOVEMENT_SPEED);
        n2e.put("CONDUIT_POWER",     MobEffects.CONDUIT_POWER);
        n2e.put("STRENGTH",          MobEffects.DAMAGE_BOOST);
        n2e.put("BOUNTIFUL_HARVEST", ModEffects.BOUNTIFUL_HARVEST);
        n2e.put("JUMP_BOOST",        MobEffects.JUMP);
        n2e.put("LUCK",              MobEffects.LUCK);
        n2e.put("GLOWING",           MobEffects.GLOWING);
        n2e.put("SLOW_FALLING",      MobEffects.SLOW_FALLING);
        n2e.put("WATER_BREATHING",   MobEffects.WATER_BREATHING);
        n2e.put("UNLUCK",            MobEffects.UNLUCK);
        n2e.put("WEAVING",           MobEffects.WEAVING);
        n2e.put("WIND_CHARGED",      MobEffects.WIND_CHARGED);
        n2e.put("OOZING",            MobEffects.OOZING);
        n2e.put("COLORBLIND",        ModEffects.COLORBLIND);
        n2e.put("LIGHTWEIGHT",       ModEffects.LIGHTWEIGHT);
        n2e.put("HUNGER",            MobEffects.HUNGER);
        n2e.put("BAD_OMEN",          MobEffects.BAD_OMEN);
        n2e.put("SLOWNESS",          MobEffects.MOVEMENT_SLOWDOWN);
        n2e.put("WEAKNESS",          MobEffects.WEAKNESS);
        n2e.put("SLIPPERY",          ModEffects.SLIPPERY);
        n2e.put("FRAIL",             ModEffects.FRAIL);
        n2e.put("MINING_FATIGUE",    MobEffects.DIG_SLOWDOWN);
        n2e.put("INFESTED",          MobEffects.INFESTED);
        n2e.put("BLINDNESS",         MobEffects.BLINDNESS);
        n2e.put("DARKNESS",          MobEffects.DARKNESS);
        n2e.put("BROKEN_LEGS",       ModEffects.BROKEN_LEGS);
        n2e.put("WEEPING_ANGEL",     ModEffects.WEEPING_ANGEL);
        // Extra vanilla effects — disabled by default; enable via config commands
        n2e.put("SATURATION",        MobEffects.SATURATION);
        n2e.put("HEALTH_BOOST",      MobEffects.HEALTH_BOOST);
        n2e.put("ABSORPTION",        MobEffects.ABSORPTION);
        n2e.put("NAUSEA",            MobEffects.CONFUSION);
        n2e.put("POISON",            MobEffects.POISON);
        n2e.put("WITHER",            MobEffects.WITHER);
        n2e.put("LEVITATION",        MobEffects.LEVITATION);
        n2e.put("TRIAL_OMEN",        MobEffects.TRIAL_OMEN);
        NAME_TO_EFFECT = Collections.unmodifiableMap(n2e);

        Map<Holder<MobEffect>, String> e2n = new LinkedHashMap<>();
        for (Map.Entry<String, Holder<MobEffect>> entry : n2e.entrySet()) {
            e2n.put(entry.getValue(), entry.getKey());
        }
        EFFECT_TO_NAME = Collections.unmodifiableMap(e2n);
    }

    // ── Config-driven pool (used for normal rolls) ────────────────────────────

    public static List<Holder<MobEffect>> poolFor(FateTier tier) {
        if (tier == FateTier.MAHJONG || tier == FateTier.FUNNY_NUMBERS) return List.of();
        List<Holder<MobEffect>> result = new ArrayList<>();
        String tierName = tier.name();
        for (Map.Entry<String, RNGdleConfig.EffectConfig> entry : RNGdleConfig.INSTANCE.effects.entrySet()) {
            RNGdleConfig.EffectConfig cfg = entry.getValue();
            if (!cfg.enabled || !tierName.equals(cfg.tier)) continue;
            Holder<MobEffect> effect = NAME_TO_EFFECT.get(entry.getKey());
            if (effect != null) result.add(effect);
        }
        return result;
    }

    // Per-effect draw weight — reads from config, falls back to 10
    public static int effectWeight(Holder<MobEffect> effect) {
        String name = EFFECT_TO_NAME.get(effect);
        if (name == null) return 10;
        RNGdleConfig.EffectConfig cfg = RNGdleConfig.INSTANCE.effects.get(name);
        return cfg != null ? cfg.weight : 10;
    }

    // Per-effect amplifier — reads from config, falls back to 0
    public static int effectAmplifier(Holder<MobEffect> effect) {
        String name = EFFECT_TO_NAME.get(effect);
        if (name == null) return 0;
        RNGdleConfig.EffectConfig cfg = RNGdleConfig.INSTANCE.effects.get(name);
        return cfg != null ? cfg.amplifier : 0;
    }

    // ── Neighbor and count weights (out of config scope, intentionally fixed) ─

    public static final Map<FateTier, Map<FateTier, Integer>> NEIGHBOR_WEIGHTS = Map.of(
            FateTier.GODTIER, Map.of(
                    FateTier.GODTIER, 6, FateTier.GOOD, 3, FateTier.NEUTRAL, 0,
                    FateTier.BAD, 0, FateTier.HELL, 0),
            FateTier.GOOD, Map.of(
                    FateTier.GODTIER, 2, FateTier.GOOD, 12, FateTier.NEUTRAL, 6,
                    FateTier.BAD, 1, FateTier.HELL, 0),
            FateTier.NEUTRAL, Map.of(
                    FateTier.GODTIER, 0, FateTier.GOOD, 1, FateTier.NEUTRAL, 6,
                    FateTier.BAD, 1, FateTier.HELL, 0),
            FateTier.BAD, Map.of(
                    FateTier.GODTIER, 0, FateTier.GOOD, 1, FateTier.NEUTRAL, 6,
                    FateTier.BAD, 12, FateTier.HELL, 2),
            FateTier.HELL, Map.of(
                    FateTier.GODTIER, 0, FateTier.GOOD, 0, FateTier.NEUTRAL, 1,
                    FateTier.BAD, 3, FateTier.HELL, 6)
    );

    public static final Map<FateTier, int[]> COUNT_WEIGHTS = Map.of(
            FateTier.GODTIER, new int[]{0, 1, 4, 3, 2},
            FateTier.GOOD,    new int[]{1, 5, 4, 2, 1},
            FateTier.NEUTRAL, new int[]{4, 3, 1},
            FateTier.BAD,     new int[]{2, 3, 3, 1, 1},
            FateTier.HELL,    new int[]{0, 2, 3, 2, 1}
    );

    private FateEffectPools() {}
}
