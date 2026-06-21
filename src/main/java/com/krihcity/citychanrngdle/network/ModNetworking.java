package com.krihcity.citychanrngdle.network;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CityChanRNGdle.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CityChanRNGdle.MODID);
        registrar.playToClient(
                FateRollPacket.TYPE,
                FateRollPacket.STREAM_CODEC,
                FateRollPacket::handle
        );
    }

    private ModNetworking() {}
}
