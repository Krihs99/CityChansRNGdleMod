package com.krihcity.citychanrngdle.client;

import com.krihcity.citychanrngdle.network.FateRollPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FateClientHandler {

    public static void onFateRoll(FateRollPacket packet) {
        FatePendingReveal.beginReveal(packet);
    }

    private FateClientHandler() {}
}
