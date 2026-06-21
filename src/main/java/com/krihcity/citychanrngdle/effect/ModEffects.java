package com.krihcity.citychanrngdle.effect;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CityChanRNGdle.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BOUNTIFUL_HARVEST =
            MOB_EFFECTS.register("bountiful_harvest",
                    () -> new BountifulHarvestEffect(MobEffectCategory.BENEFICIAL, 0x66BB44));

    public static final DeferredHolder<MobEffect, MobEffect> ENLIGHTENED =
            MOB_EFFECTS.register("enlightened",
                    () -> new EnlightenedEffect(MobEffectCategory.BENEFICIAL, 0x88EEFF));

    public static final DeferredHolder<MobEffect, MobEffect> BROKEN_LEGS =
            MOB_EFFECTS.register("broken_legs",
                    () -> new BrokenLegsEffect(MobEffectCategory.HARMFUL, 0xC87941));

    public static final DeferredHolder<MobEffect, MobEffect> WEEPING_ANGEL =
            MOB_EFFECTS.register("weeping_angel",
                    () -> new WeepingAngelEffect(MobEffectCategory.HARMFUL, 0x1A1A2E));

    public static final DeferredHolder<MobEffect, MobEffect> SLIPPERY =
            MOB_EFFECTS.register("slippery",
                    () -> new SlipperyEffect(MobEffectCategory.HARMFUL, 0xAADDFF));

    public static final DeferredHolder<MobEffect, MobEffect> COLORBLIND =
            MOB_EFFECTS.register("colorblind",
                    () -> new ColorblindEffect(MobEffectCategory.HARMFUL, 0x888888));

    public static final DeferredHolder<MobEffect, MobEffect> LIGHTWEIGHT =
            MOB_EFFECTS.register("lightweight",
                    () -> new LightweightEffect(MobEffectCategory.NEUTRAL, 0xC0E8FF));

    public static final DeferredHolder<MobEffect, MobEffect> FRAIL =
            MOB_EFFECTS.register("frail",
                    () -> new FrailEffect(MobEffectCategory.HARMFUL, 0xD4B896));

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    private ModEffects() {}
}
