package com.krihcity.citychanrngdle.client;

import com.krihcity.citychanrngdle.fate.FateTier;
import com.krihcity.citychanrngdle.network.FateRollPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public final class FatePendingReveal {

    public enum SuspenseMode { NORMAL, FALSE_LANDING, SILENT_SNAP, QUICK_TICK, SLOW_CRAWL, DEATH_CRAWL }

    private static final Random RNG = new Random();

    // Core reveal state
    @Nullable
    public static volatile FateRollPacket current = null;
    public static volatile long startMs = -1L;
    public static volatile String pickedFlavorText = "";

    // Suspense state — determined once per roll when packet arrives
    public static volatile SuspenseMode suspenseMode = SuspenseMode.NORMAL;
    @Nullable
    public static volatile FateTier falseTier = null; // only set for FALSE_LANDING

    // Sound tracking — reset each reveal so sounds fire exactly once per event
    public static volatile int     lastSpinTick        = -1;
    public static volatile int     lastEffectCount     =  0;
    public static volatile boolean landSoundPlayed     = false;
    public static volatile boolean falseLandSoundPlayed = false;

    public static void beginReveal(FateRollPacket packet) {
        current              = packet;
        startMs              = System.currentTimeMillis();
        pickedFlavorText     = packet.tierFlavorText();
        lastSpinTick         = -1;
        lastEffectCount      = 0;
        landSoundPlayed      = false;
        falseLandSoundPlayed = false;

        // 10% slow crawl, 20% death crawl, 15% false landing, 10% silent snap, 10% quick tick, 35% normal
        int roll = RNG.nextInt(100);
        if (roll < 10) {
            suspenseMode = SuspenseMode.SLOW_CRAWL;
            falseTier    = null;
        } else if (roll < 30) {
            suspenseMode = SuspenseMode.DEATH_CRAWL;
            falseTier    = null;
        } else if (roll < 33) {
            suspenseMode = SuspenseMode.FALSE_LANDING;
            falseTier    = pickFalseTier(packet);
        } else if (roll < 35) {
            suspenseMode = SuspenseMode.SILENT_SNAP;
            falseTier    = pickFalseTier(packet);
        } else if (roll < 45) {
            suspenseMode = SuspenseMode.QUICK_TICK;
            falseTier    = pickFalseTier(packet);
        } else {
            suspenseMode = SuspenseMode.NORMAL;
            falseTier    = null;
        }
    }

    public static void clear() {
        current              = null;
        startMs              = -1L;
        pickedFlavorText     = "";
        suspenseMode         = SuspenseMode.NORMAL;
        falseTier            = null;
        lastSpinTick         = -1;
        lastEffectCount      = 0;
        landSoundPlayed      = false;
        falseLandSoundPlayed = false;
    }

    /** Elapsed ms since reveal began, or -1 if nothing is pending. */
    public static long elapsed() {
        if (current == null) return -1L;
        return System.currentTimeMillis() - startMs;
    }

    private static FateTier pickFalseTier(FateRollPacket packet) {
        // ~5% each: secret tiers can flash as a fake landing for extra tension
        if (RNG.nextInt(33) == 0 && packet.tier() != FateTier.MAHJONG) return FateTier.MAHJONG;
        if (RNG.nextInt(33) == 0 && packet.tier() != FateTier.FUNNY_NUMBERS) return FateTier.FUNNY_NUMBERS;
        List<FateTier> candidates = new ArrayList<>();
        for (FateTier t : FateTier.SPIN_CYCLE)
            if (t != packet.tier()) candidates.add(t);
        return candidates.isEmpty() ? FateTier.NEUTRAL : candidates.get(RNG.nextInt(candidates.size()));
    }

    private FatePendingReveal() {}
}
