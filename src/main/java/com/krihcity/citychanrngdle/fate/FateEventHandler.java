package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import com.krihcity.citychanrngdle.config.RNGdleConfig;
import com.krihcity.citychanrngdle.effect.ModEffects;
import com.krihcity.citychanrngdle.effect.WeepingAngelEffect;
import com.krihcity.citychanrngdle.network.FateRollPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CityChanRNGdle.MODID)
public final class FateEventHandler {

    private static final Random RNG = new Random();
    private static volatile long lastAnnouncedDay = -1L;

    // Tracks players whose fate effects should be applied after animation completes
    private static final ConcurrentHashMap<UUID, PendingApply> pendingApplies = new ConcurrentHashMap<>();

    private record PendingApply(long applyAtTick, FateRoller.FateRollResult result) {}

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        RNGdleConfig.load(event.getServer());
    }

    // Restore effects on login + trigger roll if it's a new day
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        pendingApplies.remove(player.getUUID()); // discard any stale pending from before disconnect
        var server = player.getServer();
        if (server != null && FateServerData.get(server).isEnabled()) {
            FateRoller.reapplyCurrentEffects(player);
        }
        sendIntroIfNeeded(player);
        checkAndRoll(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        pendingApplies.remove(player.getUUID());
        WeepingAngelEffect.clearFrozenPosition(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check pending delayed effect applications every tick for timing accuracy
        PendingApply pending = pendingApplies.get(player.getUUID());
        if (pending != null && player.getServer() != null
                && player.getServer().getTickCount() >= pending.applyAtTick()) {
            pendingApplies.remove(player.getUUID());
            if (FateServerData.get(player.getServer()).isEnabled()) {
                FateRoller.reapplyCurrentEffects(player);
                broadcastIfNotable(player, pending.result());
            }
        }

        // Check for new day roll once per second
        if (player.tickCount % 20 != 0) return;
        checkAndRoll(player);
    }

    // Re-apply fate effects on respawn (death clears all effects)
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var server = player.getServer();
        if (server == null || !FateServerData.get(server).isEnabled()) return;
        FateRoller.reapplyCurrentEffects(player);
    }

    // Milk-proof: cancel removal of fate-tagged effects
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (event.getEffectInstance() == null) return; // can be null when removing an effect the player doesn't have
        var effectHolder = event.getEffectInstance().getEffect();
        if (effectHolder.is(ModEffects.WEEPING_ANGEL.getKey())) {
            WeepingAngelEffect.clearFrozenPosition(player.getUUID());
        }
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        var effectId = effectHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
        if (effectId != null && data.isActiveFateEffect(effectId)) {
            event.setCanceled(true);
        }
    }

    // Announce a new Minecraft day server-wide, once per day
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;
        long dayTime = serverLevel.getDayTime();
        if (dayTime % 24000 != 0) return;
        long day = dayTime / 24000;
        if (day == lastAnnouncedDay) return;
        lastAnnouncedDay = day;

        MutableComponent msg = Component.literal("[CityChan] ")
                .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                .append(Component.literal("Day ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(day))
                        .withStyle(style -> style.withColor(FateTier.GODTIER.color)))
                .append(Component.literal(" has begun.")
                        .withStyle(ChatFormatting.WHITE));

        serverLevel.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    // Enlightened: XP orbs give 50% more experience
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!event.getEntity().hasEffect(ModEffects.ENLIGHTENED)) return;
        event.getOrb().value = (int) Math.ceil(event.getOrb().value * 1.7);
    }

    // Bountiful Harvest: crop blocks drop +1 of each distinct item
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!player.hasEffect(ModEffects.BOUNTIFUL_HARVEST)) return;
        if (!event.getState().is(BlockTags.CROPS)) return;
        if (event.getDrops().isEmpty()) return;

        List<ItemEntity> drops = event.getDrops();
        List<ItemEntity> bonus = new ArrayList<>();
        BlockPos pos = event.getPos();
        Set<Item> seen = new HashSet<>();

        for (ItemEntity existing : drops) {
            if (!seen.add(existing.getItem().getItem())) continue;
            ItemStack extra = existing.getItem().copyWithCount(1);
            bonus.add(new ItemEntity(
                    event.getLevel(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    extra
            ));
        }
        drops.addAll(bonus);
    }

    private static void sendIntroIfNeeded(ServerPlayer player) {
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        if (data.hasSeenIntro()) return;
        data.setHasSeenIntro(true);

        int color = FateTier.MAHJONG.color;
        player.sendSystemMessage(Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(color))
            .append(Component.literal("Welcome to CityChans RNGdle!")
                .withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(color))
            .append(Component.literal("Each day you can receive a random fate with special effects.")
                .withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(color))
            .append(Component.literal("Type ")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal("/rngdle toggle")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" to opt in and start receiving daily rolls.")
                .withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(color))
            .append(Component.literal("Type ")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal("/rngdle help")
                .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" to see all available commands.")
                .withStyle(ChatFormatting.GRAY)));
    }

    private static void checkAndRoll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        var server = player.getServer();
        if (server == null) return;
        if (!FateServerData.get(server).isEnabled()) return;
        long currentDay = serverLevel.getDayTime() / 24000;
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        if (data.getLastRolledDay() == currentDay) return;
        if (data.isOptedOut()) {
            // Day changed while opted out — clear any lingering effects, tracked separately
            // so lastRolledDay stays untouched and a re-enable same day can still roll
            if (data.getLastOptedOutDay() != currentDay) {
                FateRoller.clearFateEffects(player);
                data.setLastOptedOutDay(currentDay);
            }
            return;
        }
        if (pendingApplies.containsKey(player.getUUID())) return; // roll already in flight

        FateRoller.FateRollResult result = FateRoller.roll(RNG);
        FateRoller.commitRoll(player, result, currentDay);

        sendRollPacketAndScheduleApply(player, result);

        CityChanRNGdle.LOGGER.debug("{} rolled {} on day {}",
                player.getName().getString(), result.tier(), currentDay);
    }

    // Forcefully rolls a new fate for a player, bypassing day/opt-out checks.
    public static void forceRoll(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        var server = player.getServer();
        if (server == null) return;

        pendingApplies.remove(player.getUUID()); // cancel any in-flight roll first

        long currentDay = serverLevel.getDayTime() / 24000;
        FateRoller.FateRollResult result = FateRoller.roll(RNG);
        FateRoller.commitRoll(player, result, currentDay);

        sendRollPacketAndScheduleApply(player, result);

        CityChanRNGdle.LOGGER.debug("{} force-rolled {} on day {}",
                player.getName().getString(), result.tier(), currentDay);
    }

    // Cancels any pending roll animation for a player (used by admin opt-out).
    public static void cancelPendingRoll(UUID uuid) {
        pendingApplies.remove(uuid);
    }

    private static void sendRollPacketAndScheduleApply(ServerPlayer player, FateRoller.FateRollResult result) {
        var server = player.getServer();
        if (server == null) return;
        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        PacketDistributor.sendToPlayer(player, new FateRollPacket(
                result.tier(),
                Optional.ofNullable(result.hand()),
                Optional.ofNullable(result.funnyNumber()),
                data.getActiveFateEffects(),
                pickTierFlavor(result.tier()),
                result.hand()        != null ? RNGdleConfig.INSTANCE.getEnabledMahjongFlavor(result.hand().name())            : "",
                result.funnyNumber() != null ? RNGdleConfig.INSTANCE.getEnabledFunnyNumberFlavor(result.funnyNumber().name()) : ""
        ));
        int effectCount = data.getActiveFateEffects().size();
        long animMs = FateRevealTiming.SPIN_MS + FateRevealTiming.SLOW_CRAWL_EXTRA_MS
                + FateRevealTiming.HAND_LEAD_MS
                + (long) effectCount * FateRevealTiming.EFFECT_INTERVAL_MS + 1000L;
        pendingApplies.put(player.getUUID(),
                new PendingApply(server.getTickCount() + animMs / 50L, result));
    }

    private static String pickTierFlavor(FateTier tier) {
        List<String> flavors = RNGdleConfig.INSTANCE.getEnabledFlavorsForTier(tier.name());
        if (flavors.isEmpty()) return "";
        return flavors.get(RNG.nextInt(flavors.size()));
    }

    private static void broadcastIfNotable(ServerPlayer player, FateRoller.FateRollResult result) {
        if (result.tier() != FateTier.GODTIER
                && result.tier() != FateTier.HELL
                && result.tier() != FateTier.MAHJONG
                && result.tier() != FateTier.FUNNY_NUMBERS) return;

        var server = player.getServer();
        if (server == null) return;

        Component tooltip = buildEffectTooltip(result);

        MutableComponent playerName = Component.literal(player.getName().getString())
                .withStyle(style -> style.applyFormat(ChatFormatting.WHITE)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));

        MutableComponent message = Component.literal("[CityChan] ")
                .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                .append(playerName)
                .append(Component.literal(" rolled ")
                        .withStyle(ChatFormatting.WHITE));

        if (result.tier() == FateTier.MAHJONG && result.hand() != null) {
            message.append(Component.literal("Mahjong")
                            .withStyle(style -> style.withColor(FateTier.MAHJONG.color)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))))
                    .append(Component.literal(" ---> ")
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(result.hand().displayName)
                            .withStyle(style -> style.withColor(FateTier.GODTIER.color)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        } else if (result.tier() == FateTier.FUNNY_NUMBERS && result.funnyNumber() != null) {
            message.append(Component.literal("Funny Numbers")
                            .withStyle(style -> style.withColor(FateTier.FUNNY_NUMBERS.color)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))))
                    .append(Component.literal(" ---> ")
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(result.funnyNumber().displayName)
                            .withStyle(style -> style.withColor(FateTier.FUNNY_NUMBERS.color)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        } else {
            String tierName = result.tier() == FateTier.GODTIER ? "Godtier" : "Hell";
            message.append(Component.literal(tierName)
                    .withStyle(style -> style.withColor(result.tier().color)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static Component buildEffectTooltip(FateRoller.FateRollResult result) {
        MutableComponent tooltip = Component.literal("Effects:")
                .withStyle(style -> style.withColor(result.tier().color));
        for (Holder<MobEffect> effect : result.effects()) {
            tooltip.append(Component.literal("\n  ").withStyle(ChatFormatting.WHITE))
                   .append(Component.translatable(effect.value().getDescriptionId())
                           .withStyle(ChatFormatting.WHITE));
        }
        return tooltip;
    }

    private FateEventHandler() {}
}
