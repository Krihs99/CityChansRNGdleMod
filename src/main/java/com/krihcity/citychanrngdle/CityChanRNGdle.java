package com.krihcity.citychanrngdle;

import com.krihcity.citychanrngdle.effect.ModEffects;
import com.krihcity.citychanrngdle.fate.FateAttachments;
import com.krihcity.citychanrngdle.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CityChanRNGdle.MODID)
public class CityChanRNGdle {

    public static final String MODID = "citychanrngdle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CityChanRNGdle(IEventBus modEventBus, ModContainer modContainer) {
        FateAttachments.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        LOGGER.info("CityChans RNGdle initializing.");
    }
}
