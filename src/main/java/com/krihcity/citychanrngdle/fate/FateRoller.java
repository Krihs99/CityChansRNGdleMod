package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.config.RNGdleConfig;
import com.krihcity.citychanrngdle.effect.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

public final class FateRoller {

    // Effectively permanent — replaced on next day roll
    private static final int FATE_DURATION = Integer.MAX_VALUE / 2;

    // Per-hand amplifier overrides (amplifier 1 = level II in UI)
    private static final Map<MahjongHand, Map<ResourceLocation, Integer>> HAND_AMP_OVERRIDES = Map.of(
        MahjongHand.RIICHI, Map.of(
            ResourceLocation.withDefaultNamespace("strength"), 1
        ),
        MahjongHand.ALL_TERMINALS, Map.of(
            ResourceLocation.withDefaultNamespace("resistance"), 1
        )
    );

    // Per-number amplifier overrides for Funny Numbers tier (amp=4 = level V)
    private static final Map<FunnyNumber, Map<ResourceLocation, Integer>> FUNNY_AMP_OVERRIDES = Map.of(
        FunnyNumber.SIXTY_NINE, Map.of(
            ResourceLocation.withDefaultNamespace("health_boost"), 1
        ),
        FunnyNumber.FOUR_TWENTY, Map.of(
            ResourceLocation.withDefaultNamespace("absorption"), 1
        )
    );

    public static FateRollResult roll(Random rng) {
        FateTier tier = rollTier(rng);
        if (tier == FateTier.MAHJONG) {
            MahjongHand hand = rollMahjongHand(rng);
            List<Holder<MobEffect>> effects = resolveHandEffects(hand, rng);
            return new FateRollResult(tier, hand, null, effects);
        }
        if (tier == FateTier.FUNNY_NUMBERS) {
            FunnyNumber number = rollFunnyNumber(rng);
            List<Holder<MobEffect>> effects = resolveFunnyNumberEffects(number);
            return new FateRollResult(tier, null, number, effects);
        }
        int count = rollCount(tier, rng);
        List<Holder<MobEffect>> effects = drawEffects(tier, count, rng);
        return new FateRollResult(tier, null, null, effects);
    }

    // Saves the roll result and clears old fate effects, but does NOT apply new MobEffects.
    // Call reapplyCurrentEffects() after the client reveal animation finishes.
    public static void commitRoll(ServerPlayer player, FateRollResult result, long day) {
        clearFateEffects(player);

        List<ResourceLocation> ids = new ArrayList<>();
        for (Holder<MobEffect> effect : result.effects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
            if (id != null) ids.add(id);
        }

        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        data.setLastRolledDay(day);
        data.incrementTierCount(result.tier());
        data.setCurrentTier(result.tier());
        data.setCurrentHand(result.hand());
        data.setCurrentFunnyNumber(result.funnyNumber());
        data.setActiveFateEffects(ids);
    }

    public static void reapplyCurrentEffects(ServerPlayer player) {
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        MahjongHand hand = data.getCurrentHand();
        Map<ResourceLocation, Integer> handAmpOverrides = hand != null
                ? HAND_AMP_OVERRIDES.getOrDefault(hand, Map.of())
                : Map.of();
        FunnyNumber funnyNumber = data.getCurrentFunnyNumber();
        Map<ResourceLocation, Integer> funnyAmpOverrides = funnyNumber != null
                ? FUNNY_AMP_OVERRIDES.getOrDefault(funnyNumber, Map.of())
                : Map.of();
        for (ResourceLocation id : data.getActiveFateEffects()) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect == null) continue;
            if (player.hasEffect(effect)) continue;
            int amp = handAmpOverrides.getOrDefault(id, funnyAmpOverrides.getOrDefault(id, FateEffectPools.effectAmplifier(effect)));
            int duration = effect == MobEffects.WATER_BREATHING ? Integer.MAX_VALUE : FATE_DURATION;
            player.addEffect(new MobEffectInstance(effect, duration, amp, false, false, true));
        }
    }

    public static void clearFateEffects(ServerPlayer player) {
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        // Snapshot and clear the list BEFORE removing effects so MobEffectEvent.Remove
        // does not see them as active fate effects and cancel the removal (milk-proof self-block).
        List<ResourceLocation> toRemove = new ArrayList<>(data.getActiveFateEffects());
        data.setActiveFateEffects(Collections.emptyList());
        for (ResourceLocation id : toRemove) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (effect != null) player.removeEffect(effect);
        }
    }

    // Removes active effects from the player without touching the stored list,
    // so reapplyCurrentEffects can restore them later (used by admin disable).
    public static void suspendActiveEffects(ServerPlayer player) {
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        List<ResourceLocation> saved = new ArrayList<>(data.getActiveFateEffects());
        data.setActiveFateEffects(Collections.emptyList()); // bypass milk-proof event
        for (ResourceLocation id : saved) {
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            // Only remove effects the player actually has — removeEffect on a missing effect
            // fires MobEffectEvent.Remove with a null effectInstance, causing an NPE
            if (effect != null && player.hasEffect(effect)) player.removeEffect(effect);
        }
        data.setActiveFateEffects(saved); // restore list — effects gone, record intact
    }

    // Mutually exclusive effect pairs — if one is chosen, the other is blocked
    private static final List<Set<Holder<MobEffect>>> EXCLUSIVE_PAIRS = List.of(
        Set.of(MobEffects.JUMP,              ModEffects.WEEPING_ANGEL),
        Set.of(MobEffects.DAMAGE_BOOST,      MobEffects.WEAKNESS),
        Set.of(MobEffects.MOVEMENT_SPEED,    MobEffects.MOVEMENT_SLOWDOWN),
        Set.of(MobEffects.DARKNESS,          MobEffects.BLINDNESS),
        Set.of(MobEffects.HERO_OF_THE_VILLAGE, MobEffects.BAD_OMEN),
        Set.of(MobEffects.LUCK,              MobEffects.UNLUCK)
    );

    private static boolean conflictsWithChosen(Holder<MobEffect> candidate,
                                                List<Holder<MobEffect>> chosen) {
        for (Set<Holder<MobEffect>> pair : EXCLUSIVE_PAIRS) {
            if (!pair.contains(candidate)) continue;
            for (Holder<MobEffect> c : chosen)
                if (pair.contains(c)) return true;
        }
        return false;
    }

    // --- Tier roll ---

    private static FateTier rollTier(Random rng) {
        Map<String, Integer> weights = RNGdleConfig.INSTANCE.tierWeights;
        int total = 0;
        for (FateTier t : FateTier.values()) total += weights.getOrDefault(t.name(), 0);
        if (total <= 0) return FateTier.NEUTRAL;
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (FateTier t : FateTier.values()) {
            cumulative += weights.getOrDefault(t.name(), 0);
            if (roll < cumulative) return t;
        }
        return FateTier.NEUTRAL;
    }

    // --- Count roll ---

    private static int rollCount(FateTier tier, Random rng) {
        int[] weights = FateEffectPools.COUNT_WEIGHTS.get(tier);
        int total = Arrays.stream(weights).sum();
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) return i + 1;
        }
        return 2;
    }

    // --- Effect draw ---

    private static List<Holder<MobEffect>> drawEffects(FateTier tier, int count, Random rng) {
        List<Holder<MobEffect>> pool = new ArrayList<>(FateEffectPools.poolFor(tier));
        List<Holder<MobEffect>> chosen = new ArrayList<>();

        // Anchor: one guaranteed from the rolled tier, drawn with per-effect weights
        if (!pool.isEmpty()) {
            Holder<MobEffect> anchor = weightedPickFrom(pool, chosen, rng);
            if (anchor != null) chosen.add(anchor);
        }

        // Fill remaining slots via neighbor-weighted draw
        Map<FateTier, Integer> neighborWeights = FateEffectPools.NEIGHBOR_WEIGHTS.get(tier);
        for (int i = chosen.size(); i < count; i++) {
            Holder<MobEffect> pick = weightedNeighborDraw(neighborWeights, chosen, rng);
            if (pick != null) chosen.add(pick);
        }

        return chosen;
    }

    private static Holder<MobEffect> weightedPickFrom(List<Holder<MobEffect>> pool,
                                                       List<Holder<MobEffect>> excluded,
                                                       Random rng) {
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Holder<MobEffect> e : pool) {
            if (!excluded.contains(e) && !conflictsWithChosen(e, excluded)) {
                candidates.add(e);
                weights.add(FateEffectPools.effectWeight(e));
            }
        }
        if (candidates.isEmpty()) return null;
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    private static Holder<MobEffect> weightedNeighborDraw(
            Map<FateTier, Integer> weights,
            List<Holder<MobEffect>> alreadyChosen,
            Random rng) {

        // Build combined weighted pool excluding already-chosen effects
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        List<Integer> candidateWeights = new ArrayList<>();

        for (FateTier t : FateTier.values()) {
            if (t == FateTier.MAHJONG) continue;
            int w = weights.getOrDefault(t, 0);
            if (w == 0) continue;
            for (Holder<MobEffect> e : FateEffectPools.poolFor(t)) {
                if (!alreadyChosen.contains(e) && !conflictsWithChosen(e, alreadyChosen)) {
                    candidates.add(e);
                    candidateWeights.add(w);
                }
            }
        }

        if (candidates.isEmpty()) return null;

        int total = candidateWeights.stream().mapToInt(Integer::intValue).sum();
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += candidateWeights.get(i);
            if (roll < cumulative) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    // --- Funny Numbers ---

    private static FunnyNumber rollFunnyNumber(Random rng) {
        int total = 0;
        for (FunnyNumber n : FunnyNumber.values()) total += n.weight;
        int roll = rng.nextInt(total);
        int cumulative = 0;
        for (FunnyNumber n : FunnyNumber.values()) {
            cumulative += n.weight;
            if (roll < cumulative) return n;
        }
        return FunnyNumber.SIXTY_NINE;
    }

    private static List<Holder<MobEffect>> resolveFunnyNumberEffects(FunnyNumber number) {
        return switch (number) {
            case SIXTY_SEVEN        -> List.of(MobEffects.GLOWING, MobEffects.LEVITATION);
            case SIXTY_NINE         -> List.of(MobEffects.SATURATION, MobEffects.HEALTH_BOOST,
                                               MobEffects.HERO_OF_THE_VILLAGE, MobEffects.WEAVING,
                                               MobEffects.WIND_CHARGED);
            case SEVEN_TWENTY_SEVEN -> List.of(MobEffects.JUMP, MobEffects.NIGHT_VISION,
                                               MobEffects.INVISIBILITY, MobEffects.MOVEMENT_SPEED,
                                               MobEffects.LUCK);
            case FOUR_TWENTY        -> List.of(ModEffects.BOUNTIFUL_HARVEST, MobEffects.WEAKNESS,
                                               MobEffects.SLOW_FALLING, MobEffects.WIND_CHARGED,
                                               MobEffects.ABSORPTION, MobEffects.MOVEMENT_SPEED);
        };
    }

    // --- Mahjong ---

    private static MahjongHand rollMahjongHand(Random rng) {
        MahjongHand[] hands = MahjongHand.values();
        return hands[rng.nextInt(hands.length)];
    }

    private static List<Holder<MobEffect>> resolveHandEffects(MahjongHand hand, Random rng) {
        return switch (hand) {
            case NINE_GATES           -> drawNineGates(rng);
            case THIRTEEN_ORPHANS     -> drawThirteenOrphans(rng);
            case UNDER_THE_SEA        -> List.of(MobEffects.DOLPHINS_GRACE,
                    MobEffects.CONDUIT_POWER, MobEffects.NIGHT_VISION);
            case ALL_TERMINALS        -> List.of(MobEffects.BLINDNESS, MobEffects.GLOWING, MobEffects.DAMAGE_RESISTANCE, MobEffects.DIG_SPEED);
            case TSUMO                -> List.of(MobEffects.FIRE_RESISTANCE,
                    MobEffects.LUCK, MobEffects.DIG_SPEED);
            case FOUR_CONCEALED_TRIPLETS -> List.of(MobEffects.DAMAGE_BOOST,
                    MobEffects.INVISIBILITY, MobEffects.DIG_SPEED, MobEffects.DAMAGE_RESISTANCE);
            case ALL_GREEN_IMPERIAL_JADE -> {
                List<Holder<MobEffect>> all = new ArrayList<>();
                all.addAll(FateEffectPools.GODTIER);
                all.addAll(FateEffectPools.GOOD);
                yield all;
            }
            case RIICHI               -> List.of(MobEffects.DAMAGE_BOOST, MobEffects.DIG_SPEED);
            case CHANTA               -> List.of(MobEffects.SLOW_FALLING,
                    MobEffects.WATER_BREATHING, MobEffects.JUMP, MobEffects.MOVEMENT_SPEED);
            case MIXED_TRIPLE_SEQUENCE -> drawMixedTriple(rng);
        };
    }

    @SafeVarargs
    private static List<Holder<MobEffect>> drawFromPools(Random rng, int count,
            List<Holder<MobEffect>>... pools) {
        List<Holder<MobEffect>> combined = new ArrayList<>();
        for (var pool : pools) combined.addAll(pool);
        Collections.shuffle(combined, rng);
        List<Holder<MobEffect>> result = new ArrayList<>();
        Set<Holder<MobEffect>> seen = new HashSet<>();
        for (Holder<MobEffect> e : combined) {
            if (seen.add(e)) result.add(e);
            if (result.size() >= count) break;
        }
        return result;
    }

    private static List<Holder<MobEffect>> drawThirteenOrphans(Random rng) {
        List<Holder<MobEffect>> result = new ArrayList<>();
        Set<Holder<MobEffect>> seen = new HashSet<>();

        // Glowing is guaranteed
        result.add(MobEffects.GLOWING);
        seen.add(MobEffects.GLOWING);

        // GODTIER and GOOD weight 2, all others weight 1; Blindness and Mining Fatigue excluded
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        addToOrphansPool(candidates, weights, FateEffectPools.GODTIER, 2);
        addToOrphansPool(candidates, weights, FateEffectPools.GOOD, 2);
        addToOrphansPool(candidates, weights, FateEffectPools.NEUTRAL, 1);
        addToOrphansPool(candidates, weights, FateEffectPools.BAD, 1);
        addToOrphansPool(candidates, weights, FateEffectPools.HELL, 1);

        while (result.size() < 13) {
            int total = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (!seen.contains(candidates.get(i))) total += weights.get(i);
            }
            if (total == 0) break;
            int roll = rng.nextInt(total);
            int cumulative = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (seen.contains(candidates.get(i))) continue;
                cumulative += weights.get(i);
                if (roll < cumulative) {
                    result.add(candidates.get(i));
                    seen.add(candidates.get(i));
                    break;
                }
            }
        }

        return result;
    }

    private static List<Holder<MobEffect>> drawNineGates(Random rng) {
        List<Holder<MobEffect>> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        addToOrphansPool(candidates, weights, FateEffectPools.GODTIER, 2);
        addToOrphansPool(candidates, weights, FateEffectPools.GOOD, 2);
        addToOrphansPool(candidates, weights, FateEffectPools.NEUTRAL, 1);

        List<Holder<MobEffect>> result = new ArrayList<>();
        Set<Holder<MobEffect>> seen = new HashSet<>();

        while (result.size() < 9) {
            int total = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (!seen.contains(candidates.get(i))) total += weights.get(i);
            }
            if (total == 0) break;
            int roll = rng.nextInt(total);
            int cumulative = 0;
            for (int i = 0; i < candidates.size(); i++) {
                if (seen.contains(candidates.get(i))) continue;
                cumulative += weights.get(i);
                if (roll < cumulative) {
                    result.add(candidates.get(i));
                    seen.add(candidates.get(i));
                    break;
                }
            }
        }

        return result;
    }

    private static void addToOrphansPool(List<Holder<MobEffect>> candidates,
                                          List<Integer> weights,
                                          List<Holder<MobEffect>> pool,
                                          int weight) {
        for (Holder<MobEffect> e : pool) {
            if (e == MobEffects.BLINDNESS || e == MobEffects.DIG_SLOWDOWN) continue;
            candidates.add(e);
            weights.add(weight);
        }
    }

    private static List<Holder<MobEffect>> drawMixedTriple(Random rng) {
        List<Holder<MobEffect>> result = new ArrayList<>();
        result.add(randomFrom(FateEffectPools.GODTIER, rng));
        result.add(randomFrom(FateEffectPools.GOOD, rng));
        result.add(randomFrom(FateEffectPools.NEUTRAL, rng));
        result.add(randomFrom(FateEffectPools.BAD, rng));
        result.add(randomFrom(FateEffectPools.HELL, rng));
        return result;
    }

    private static Holder<MobEffect> randomFrom(List<Holder<MobEffect>> pool, Random rng) {
        return pool.get(rng.nextInt(pool.size()));
    }

    public record FateRollResult(FateTier tier, MahjongHand hand, FunnyNumber funnyNumber, List<Holder<MobEffect>> effects) {}

    private FateRoller() {}
}
