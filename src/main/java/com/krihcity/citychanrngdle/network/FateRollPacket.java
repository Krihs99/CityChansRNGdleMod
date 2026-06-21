package com.krihcity.citychanrngdle.network;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import com.krihcity.citychanrngdle.client.FateClientHandler;
import com.krihcity.citychanrngdle.fate.FateTier;
import com.krihcity.citychanrngdle.fate.FunnyNumber;
import com.krihcity.citychanrngdle.fate.MahjongHand;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record FateRollPacket(
        FateTier tier,
        Optional<MahjongHand> hand,
        Optional<FunnyNumber> funnyNumber,
        List<ResourceLocation> effectIds,
        String tierFlavorText,
        String handFlavorText,
        String numberFlavorText
) implements CustomPacketPayload {

    public static final Type<FateRollPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CityChanRNGdle.MODID, "fate_roll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FateRollPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public FateRollPacket decode(RegistryFriendlyByteBuf buf) {
                    FateTier tier = FateTier.values()[buf.readVarInt()];
                    Optional<MahjongHand> hand = buf.readBoolean()
                            ? Optional.of(MahjongHand.values()[buf.readVarInt()])
                            : Optional.empty();
                    Optional<FunnyNumber> funnyNumber = buf.readBoolean()
                            ? Optional.of(FunnyNumber.values()[buf.readVarInt()])
                            : Optional.empty();
                    int count = buf.readVarInt();
                    List<ResourceLocation> ids = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) ids.add(buf.readResourceLocation());
                    String tierFlavor   = buf.readUtf();
                    String handFlavor   = buf.readUtf();
                    String numberFlavor = buf.readUtf();
                    return new FateRollPacket(tier, hand, funnyNumber, ids, tierFlavor, handFlavor, numberFlavor);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, FateRollPacket packet) {
                    buf.writeVarInt(packet.tier().ordinal());
                    buf.writeBoolean(packet.hand().isPresent());
                    packet.hand().ifPresent(h -> buf.writeVarInt(h.ordinal()));
                    buf.writeBoolean(packet.funnyNumber().isPresent());
                    packet.funnyNumber().ifPresent(n -> buf.writeVarInt(n.ordinal()));
                    buf.writeVarInt(packet.effectIds().size());
                    for (ResourceLocation id : packet.effectIds()) buf.writeResourceLocation(id);
                    buf.writeUtf(packet.tierFlavorText());
                    buf.writeUtf(packet.handFlavorText());
                    buf.writeUtf(packet.numberFlavorText());
                }
            };

    // playToClient — only ever invoked on the physical client; FateClientHandler is safe to reference
    public static void handle(FateRollPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FateClientHandler.onFateRoll(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
