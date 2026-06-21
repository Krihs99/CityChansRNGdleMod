package com.krihcity.citychanrngdle.client;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import com.krihcity.citychanrngdle.effect.ModEffects;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = CityChanRNGdle.MODID, value = Dist.CLIENT)
public final class ColorblindClientEvents {

    private static final ResourceLocation COLORBLIND_POST =
            ResourceLocation.fromNamespaceAndPath(CityChanRNGdle.MODID, "shaders/post/colorblind.json");

    @Nullable
    private static PostChain postChain = null;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    @Nullable
    private static RenderTarget lastMainTarget = null;

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasEffect(ModEffects.COLORBLIND)) {
            destroyChain();
            return;
        }

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        RenderTarget mainTarget = mc.getMainRenderTarget();

        if (postChain == null || w != lastWidth || h != lastHeight || mainTarget != lastMainTarget) {
            destroyChain();
            try {
                postChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mainTarget, COLORBLIND_POST);
                postChain.resize(w, h);
                lastWidth = w;
                lastHeight = h;
                lastMainTarget = mainTarget;
            } catch (Exception e) {
                CityChanRNGdle.LOGGER.warn("Colorblind: failed to create post-chain: {}", e.getMessage());
                return;
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        postChain.process(event.getPartialTick().getGameTimeDeltaTicks());
        mainTarget.bindWrite(true);
    }

    private static void destroyChain() {
        if (postChain != null) {
            postChain.close();
            postChain = null;
        }
    }

    private ColorblindClientEvents() {}
}
