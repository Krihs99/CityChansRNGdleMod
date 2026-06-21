package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FateAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CityChanRNGdle.MODID);

    public static final Supplier<AttachmentType<FateData>> FATE_DATA =
            ATTACHMENT_TYPES.register("fate_data",
                    () -> AttachmentType.serializable(FateData::new).copyOnDeath().build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private FateAttachments() {}
}
