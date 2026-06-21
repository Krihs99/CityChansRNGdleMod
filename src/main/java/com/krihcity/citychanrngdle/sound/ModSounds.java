package com.krihcity.citychanrngdle.sound;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSounds {

    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CityChanRNGdle.MODID);

    /** One click per spin tick — pitch varies 0.8→1.4 as it slows down. */
    public static final Supplier<SoundEvent> FATE_SPIN = register("fate_spin");

    /** Plays when the tier lands (non-notable tiers). */
    public static final Supplier<SoundEvent> FATE_LAND = register("fate_land");

    /** Plays for each effect that pops in — pitch ascends with count. */
    public static final Supplier<SoundEvent> FATE_EFFECT_POP = register("fate_effect_pop");

    /** Plays on landing for GODTIER, HELL, and MAHJONG. */
    public static final Supplier<SoundEvent> FATE_NOTABLE = register("fate_notable");

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(CityChanRNGdle.MODID, name)));
    }

    private ModSounds() {}
}
