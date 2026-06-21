package com.krihcity.citychanrngdle;

import com.krihcity.citychanrngdle.client.FateRevealOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@Mod(value = CityChanRNGdle.MODID, dist = Dist.CLIENT)
public class CityChanRNGdleClient {

    public CityChanRNGdleClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(CityChanRNGdleClient::onClientSetup);
        modEventBus.addListener(CityChanRNGdleClient::onRegisterOverlays);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        CityChanRNGdle.LOGGER.info("CityChans RNGdle client setup complete.");
    }

    private static void onRegisterOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(FateRevealOverlay.ID, FateRevealOverlay.OVERLAY);
    }
}
