package com.krihcity.citychanrngdle.client;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import com.krihcity.citychanrngdle.fate.FateRevealTiming;
import com.krihcity.citychanrngdle.fate.FateTier;
import com.krihcity.citychanrngdle.fate.FunnyNumber;
import com.krihcity.citychanrngdle.fate.MahjongHand;
import com.krihcity.citychanrngdle.network.FateRollPacket;
import com.krihcity.citychanrngdle.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FateRevealOverlay {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CityChanRNGdle.MODID, "fate_reveal");

    private static final int SPIN_TICKS   = 20;
    private static final int LAND_AT_TICK = 17;

    public static final LayeredDraw.Layer OVERLAY = (guiGraphics, deltaTracker) -> {
        long elapsed = FatePendingReveal.elapsed();
        if (elapsed < 0) return;

        FateRollPacket packet = FatePendingReveal.current;
        if (packet == null) return;

        // Snapshot volatile fields once — ensures consistent calculations across the whole frame
        FatePendingReveal.SuspenseMode mode = FatePendingReveal.suspenseMode;
        FateTier falseTier = FatePendingReveal.falseTier;

        long landMs  = effectiveLandMs(mode);
        long totalMs = totalDuration(packet, mode);

        if (elapsed >= totalMs) {
            FatePendingReveal.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int y = 12;

        boolean inSpinPhase = elapsed < FateRevealTiming.SPIN_MS;
        boolean landed      = elapsed >= landMs;
        boolean inSuspense  = !inSpinPhase && !landed;

        // --- Compute spinTick and displayTier ---
        int spinTick = -1;
        FateTier displayTier;

        if (inSpinPhase) {
            double tNorm = (double) elapsed / FateRevealTiming.SPIN_MS;
            double eased = 1.0 - Math.pow(1.0 - tNorm, 4.0);
            int maxTick = (mode == FatePendingReveal.SuspenseMode.SLOW_CRAWL
                    || mode == FatePendingReveal.SuspenseMode.DEATH_CRAWL)
                    ? LAND_AT_TICK - 1 : SPIN_TICKS - 1;
            spinTick = Math.min((int)(eased * SPIN_TICKS), maxTick);
            boolean revealDuringSpin = mode == FatePendingReveal.SuspenseMode.NORMAL
                    && spinTick >= LAND_AT_TICK;
            displayTier = revealDuringSpin
                    ? packet.tier()
                    : FateTier.SPIN_CYCLE[spinTick % FateTier.SPIN_CYCLE.length];
        } else if (inSuspense) {
            if (mode == FatePendingReveal.SuspenseMode.FALSE_LANDING
                    || mode == FatePendingReveal.SuspenseMode.SILENT_SNAP
                    || mode == FatePendingReveal.SuspenseMode.QUICK_TICK) {
                displayTier = falseTier != null ? falseTier : FateTier.NEUTRAL;
            } else if (mode == FatePendingReveal.SuspenseMode.SLOW_CRAWL) {
                long slowElapsed = elapsed - FateRevealTiming.SPIN_MS;
                long perTick     = FateRevealTiming.SLOW_CRAWL_EXTRA_MS / 2;
                int  slowOffset  = (int) Math.min(slowElapsed / perTick, 1L);
                spinTick    = LAND_AT_TICK + slowOffset;
                displayTier = FateTier.SPIN_CYCLE[spinTick % FateTier.SPIN_CYCLE.length];
            } else { // DEATH_CRAWL — hold on the real result for one beat before confirming
                spinTick    = LAND_AT_TICK;
                displayTier = packet.tier();
            }
        } else {
            displayTier = packet.tier();
        }

        // --- Sounds ---

        if (inSpinPhase && spinTick != FatePendingReveal.lastSpinTick) {
            FatePendingReveal.lastSpinTick = spinTick;
            boolean shouldClick = mode != FatePendingReveal.SuspenseMode.NORMAL
                    || spinTick < LAND_AT_TICK;
            if (shouldClick) {
                float pitch = 0.8f + (spinTick / (float)(SPIN_TICKS - 1)) * 0.6f;
                playUi(ModSounds.FATE_SPIN.get(), pitch);
            }
        }

        if (inSuspense && (mode == FatePendingReveal.SuspenseMode.SLOW_CRAWL
                || mode == FatePendingReveal.SuspenseMode.DEATH_CRAWL)
                && spinTick >= 0 && spinTick != FatePendingReveal.lastSpinTick) {
            FatePendingReveal.lastSpinTick = spinTick;
            FateTier lastSpinTier = FateTier.SPIN_CYCLE[(LAND_AT_TICK - 1) % FateTier.SPIN_CYCLE.length];
            if (mode != FatePendingReveal.SuspenseMode.DEATH_CRAWL || packet.tier() != lastSpinTier) {
                playUi(ModSounds.FATE_SPIN.get(), 1.35f);
            }
        }

        if (inSuspense && !FatePendingReveal.falseLandSoundPlayed) {
            FatePendingReveal.falseLandSoundPlayed = true;
            switch (mode) {
                case FALSE_LANDING, SILENT_SNAP -> playUi(ModSounds.FATE_LAND.get(), 0.9f);
                case QUICK_TICK                 -> playUi(ModSounds.FATE_SPIN.get(), 1.2f);
                default -> {}
            }
        }

        if (landed && !FatePendingReveal.landSoundPlayed) {
            FatePendingReveal.landSoundPlayed = true;
            if (mode == FatePendingReveal.SuspenseMode.FALSE_LANDING) {
                playUi(ModSounds.FATE_SPIN.get(), 1.5f);
            }
            if (packet.tier() == FateTier.HELL) {
                playUi(SoundEvents.WITHER_DEATH, 1.0f, 0.5f);
            } else if (packet.tier() == FateTier.MAHJONG) {
                playUi(SoundEvents.TOTEM_USE, 1.0f, 0.5f);
            } else if (packet.tier() == FateTier.FUNNY_NUMBERS) {
                playUi(SoundEvents.BEACON_ACTIVATE, 1.0f);
            } else {
                boolean notable = packet.tier() == FateTier.GODTIER;
                playUi(notable ? ModSounds.FATE_NOTABLE.get() : ModSounds.FATE_LAND.get(), 1.0f);
            }
        }

        int revealed = revealedEffectCount(elapsed, packet, mode);
        if (revealed > FatePendingReveal.lastEffectCount && revealed <= packet.effectIds().size()) {
            FatePendingReveal.lastEffectCount = revealed;
            float pitch = 1.0f + (revealed - 1) * 0.07f;
            playUi(ModSounds.FATE_EFFECT_POP.get(), Math.min(pitch, 2.0f));
        }

        // --- Header ---
        guiGraphics.drawCenteredString(font, "[ Your Fate ]", cx, y, 0xFFFFFFFF);
        y += 13;

        // --- Tier name ---
        String label = tierLabel(displayTier);
        boolean showBrackets = inSpinPhase
                || (inSuspense && (mode == FatePendingReveal.SuspenseMode.SLOW_CRAWL
                                || mode == FatePendingReveal.SuspenseMode.DEATH_CRAWL
                                || mode == FatePendingReveal.SuspenseMode.QUICK_TICK));
        if (showBrackets) label = "> " + label + " <";
        guiGraphics.drawCenteredString(font, label, cx, y, 0xFF000000 | displayTier.color);
        y += 11;

        // --- Flavor text (only after true landing) ---
        if (landed && !FatePendingReveal.pickedFlavorText.isEmpty()) {
            guiGraphics.drawCenteredString(font, FatePendingReveal.pickedFlavorText, cx, y, 0xFFAAAAAA);
            y += 11;
        }

        // --- Mahjong hand (after true landing) ---
        if (landed && displayTier == FateTier.MAHJONG && packet.hand().isPresent()) {
            MahjongHand hand = packet.hand().get();
            guiGraphics.drawCenteredString(font, hand.displayName, cx, y, 0xFF000000 | FateTier.GODTIER.color);
            y += 11;
            if (elapsed - landMs >= FateRevealTiming.HAND_LEAD_MS / 2 && !packet.handFlavorText().isEmpty()) {
                guiGraphics.drawCenteredString(font, packet.handFlavorText(), cx, y, 0xFFAAAAAA);
                y += 11;
            }
        }

        // --- Funny number (after true landing) ---
        if (landed && displayTier == FateTier.FUNNY_NUMBERS && packet.funnyNumber().isPresent()) {
            FunnyNumber number = packet.funnyNumber().get();
            guiGraphics.drawCenteredString(font, number.displayName, cx, y, 0xFF000000 | FateTier.FUNNY_NUMBERS.color);
            y += 11;
            if (elapsed - landMs >= FateRevealTiming.HAND_LEAD_MS / 2 && !packet.numberFlavorText().isEmpty()) {
                guiGraphics.drawCenteredString(font, packet.numberFlavorText(), cx, y, 0xFFAAAAAA);
                y += 11;
            }
        }

        // --- Effects (pop in one by one after true landing) ---
        if (landed) {
            y += 4;
            for (int i = 0; i < Math.min(revealed, packet.effectIds().size()); i++) {
                ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, packet.effectIds().get(i));
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getHolder(key)
                        .map(Holder::value)
                        .orElse(null);
                if (effect == null) continue;
                guiGraphics.drawCenteredString(font, effect.getDisplayName().getString(), cx, y, 0xFFE0E0E0);
                y += 10;
            }
        }
    };

    private static long effectiveLandMs(FatePendingReveal.SuspenseMode mode) {
        return switch (mode) {
            case FALSE_LANDING -> FateRevealTiming.SPIN_MS + FateRevealTiming.FALSE_LAND_HOLD_MS;
            case SILENT_SNAP   -> FateRevealTiming.SPIN_MS + FateRevealTiming.SILENT_SNAP_HOLD_MS;
            case QUICK_TICK    -> FateRevealTiming.SPIN_MS + FateRevealTiming.QUICK_TICK_EXTRA_MS;
            case SLOW_CRAWL    -> FateRevealTiming.SPIN_MS + FateRevealTiming.SLOW_CRAWL_EXTRA_MS;
            case DEATH_CRAWL   -> FateRevealTiming.SPIN_MS + FateRevealTiming.DEATH_CRAWL_EXTRA_MS;
            case NORMAL        -> FateRevealTiming.SPIN_MS;
        };
    }

    private static void playUi(SoundEvent sound, float pitch) {
        playUi(sound, pitch, 1.0f);
    }

    private static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private static boolean hasLeadIn(FateRollPacket packet) {
        return (packet.tier() == FateTier.MAHJONG      && packet.hand().isPresent())
            || (packet.tier() == FateTier.FUNNY_NUMBERS && packet.funnyNumber().isPresent());
    }

    private static int revealedEffectCount(long elapsed, FateRollPacket packet,
                                           FatePendingReveal.SuspenseMode mode) {
        long landMs = effectiveLandMs(mode);
        if (elapsed < landMs) return 0;
        long afterLand = elapsed - landMs;
        if (hasLeadIn(packet)) {
            afterLand = Math.max(0L, afterLand - FateRevealTiming.HAND_LEAD_MS);
        }
        return (int)(afterLand / FateRevealTiming.EFFECT_INTERVAL_MS);
    }

    private static long totalDuration(FateRollPacket packet, FatePendingReveal.SuspenseMode mode) {
        long total = effectiveLandMs(mode);
        if (hasLeadIn(packet)) total += FateRevealTiming.HAND_LEAD_MS;
        total += (long) packet.effectIds().size() * FateRevealTiming.EFFECT_INTERVAL_MS;
        return total + FateRevealTiming.HOLD_MS;
    }

    private static String tierLabel(FateTier tier) {
        return switch (tier) {
            case GODTIER       -> "GODTIER";
            case MAHJONG       -> "MAHJONG";
            case FUNNY_NUMBERS -> "Funny Numbers";
            default -> tier.name().charAt(0) + tier.name().substring(1).toLowerCase();
        };
    }

    private FateRevealOverlay() {}
}
