package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.CityChanRNGdle;
import com.krihcity.citychanrngdle.config.RNGdleConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = CityChanRNGdle.MODID)
public final class FateCommands {

    private static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS =
        (ctx, builder) -> {
            for (FateTier t : FateTier.values()) builder.suggest(t.name());
            return builder.buildFuture();
        };

    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS =
        (ctx, builder) -> {
            for (String key : FateEffectPools.NAME_TO_EFFECT.keySet()) builder.suggest(key);
            return builder.buildFuture();
        };

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS =
        (ctx, builder) -> {
            for (String p : RNGdleConfig.VALID_PRESETS) builder.suggest(p);
            return builder.buildFuture();
        };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("rngdle")
                .then(Commands.literal("help")
                    .executes(ctx -> executeHelp(ctx.getSource())))
                .then(Commands.literal("toggle")
                    .executes(ctx -> executeToggle(ctx.getSource())))
                .then(Commands.literal("profile")
                    .executes(ctx -> executeProfile(ctx.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> executeProfile(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("today")
                    .executes(ctx -> executeToday(ctx.getSource(), null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> executeToday(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("admin")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("reroll")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> executeReroll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                    .then(Commands.literal("toggle")
                        .executes(ctx -> executeAdminToggle(ctx.getSource())))
                    .then(Commands.literal("optout")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> executeAdminOptOut(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                    .then(Commands.literal("reload")
                        .executes(ctx -> executeAdminReload(ctx.getSource())))
                    .then(Commands.literal("config")
                        .then(Commands.literal("list")
                            .then(Commands.literal("tiers")
                                .executes(ctx -> executeConfigListTiers(ctx.getSource())))
                            .then(Commands.literal("effects")
                                .executes(ctx -> executeConfigListEffects(ctx.getSource()))))
                        .then(Commands.literal("get")
                            .then(Commands.literal("tier")
                                .then(Commands.argument("tier", StringArgumentType.word())
                                    .suggests(TIER_SUGGESTIONS)
                                    .executes(ctx -> executeConfigGetTier(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "tier")))))
                            .then(Commands.literal("effect")
                                .then(Commands.argument("effect", StringArgumentType.word())
                                    .suggests(EFFECT_SUGGESTIONS)
                                    .executes(ctx -> executeConfigGetEffect(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "effect"))))))
                        .then(Commands.literal("set")
                            .then(Commands.literal("preset")
                                .then(Commands.argument("name", StringArgumentType.word())
                                    .suggests(PRESET_SUGGESTIONS)
                                    .executes(ctx -> executeConfigSetPreset(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                            .then(Commands.literal("tier")
                                .then(Commands.argument("tier", StringArgumentType.word())
                                    .suggests(TIER_SUGGESTIONS)
                                    .then(Commands.literal("weight")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                            .executes(ctx -> executeConfigSetTierWeight(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "tier"),
                                                IntegerArgumentType.getInteger(ctx, "value")))))))
                            .then(Commands.literal("effect")
                                .then(Commands.argument("effect", StringArgumentType.word())
                                    .suggests(EFFECT_SUGGESTIONS)
                                    .then(Commands.literal("enabled")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                            .executes(ctx -> executeConfigSetEffectEnabled(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "effect"),
                                                BoolArgumentType.getBool(ctx, "value")))))
                                    .then(Commands.literal("tier")
                                        .then(Commands.argument("newTier", StringArgumentType.word())
                                            .suggests(TIER_SUGGESTIONS)
                                            .executes(ctx -> executeConfigSetEffectTier(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "effect"),
                                                StringArgumentType.getString(ctx, "newTier")))))
                                    .then(Commands.literal("weight")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                            .executes(ctx -> executeConfigSetEffectWeight(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "effect"),
                                                IntegerArgumentType.getInteger(ctx, "value")))))
                                    .then(Commands.literal("amplifier")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                            .executes(ctx -> executeConfigSetEffectAmplifier(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "effect"),
                                                IntegerArgumentType.getInteger(ctx, "value")))))))
                            .then(Commands.literal("defaultOptedIn")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> executeConfigSetDefaultOptedIn(ctx.getSource(),
                                        BoolArgumentType.getBool(ctx, "value"))))))
                        .then(Commands.literal("save")
                            .executes(ctx -> executeConfigSave(ctx.getSource())))))
        );
    }

    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("--- CityChans RNGdle ---")
            .withStyle(style -> style.withColor(FateTier.MAHJONG.color)), false);
        source.sendSuccess(() -> Component.literal("/rngdle help")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" — Shows this message")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle toggle")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" — Toggle daily fate rolls on/off")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle profile [player]")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" — View tier roll distribution")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle today [player]")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(" — View a player's roll for today")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin reroll <player>")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Force a new roll for a player")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin toggle")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Enable or disable the mod server-wide")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin optout <player>")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Toggle a player's opt-out status")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin reload")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Reload config from disk without restarting")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin config list <tiers|effects>")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] List current config values")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin config get <tier|effect> <name>")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Get a specific config value")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin config set ... (use tab)")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Modify config in-memory")
                .withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("/rngdle admin config save")
            .withStyle(ChatFormatting.RED)
            .append(Component.literal(" — [OP] Write in-memory config to disk")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeToggle(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        boolean nowOptedOut = !data.isOptedOut();
        data.setOptedOut(nowOptedOut);

        if (nowOptedOut) {
            boolean rolledToday = player.getServer() != null
                    && data.getLastRolledDay() == player.getServer().overworld().getDayTime() / 24000;
            if (rolledToday) {
                source.sendSuccess(() -> Component.literal("[CityChan] ")
                    .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                    .append(Component.literal("Fate rolls disabled. Your current effects will last until tomorrow.")
                        .withStyle(ChatFormatting.WHITE)), false);
            } else {
                source.sendSuccess(() -> Component.literal("[CityChan] ")
                    .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                    .append(Component.literal("Fate rolls disabled.")
                        .withStyle(ChatFormatting.WHITE)), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("[CityChan] ")
                .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                .append(Component.literal("Fate rolls enabled.")
                    .withStyle(ChatFormatting.WHITE)), false);
        }

        return 1;
    }

    private static int executeProfile(CommandSourceStack source, @Nullable ServerPlayer target) {
        ServerPlayer player = resolvePlayer(source, target);
        if (player == null) return 0;

        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        int total = data.getTotalRolls();
        String name = player.getName().getString();

        source.sendSuccess(() -> Component.literal("--- Fate Profile: ")
            .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" (" + total + " rolls) ---")
                .withStyle(style -> style.withColor(FateTier.MAHJONG.color))), false);

        for (FateTier tier : FateTier.values()) {
            int count = data.getTierCount(tier);
            double pct = total > 0 ? (count * 100.0 / total) : 0.0;
            String countPct = String.format(" — %d (%.1f%%)", count, pct);
            final int color = tier.color;
            MutableComponent row = Component.literal("  ")
                .append(Component.literal(tierDisplayName(tier)).withStyle(s -> s.withColor(color)))
                .append(Component.literal(countPct).withStyle(ChatFormatting.WHITE));
            source.sendSuccess(() -> row, false);
        }

        return 1;
    }

    private static int executeToday(CommandSourceStack source, @Nullable ServerPlayer target) {
        ServerPlayer player = resolvePlayer(source, target);
        if (player == null) return 0;

        FateData data = player.getData(FateAttachments.FATE_DATA.get());
        String name = player.getName().getString();

        long currentDay = player.getServer() != null
                ? player.getServer().overworld().getDayTime() / 24000 : -1;

        if (data.getLastRolledDay() != currentDay || data.getCurrentTier() == null) {
            source.sendSuccess(() -> Component.literal("[CityChan] ")
                .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
                .append(Component.literal(name + " has not rolled yet today.")
                    .withStyle(ChatFormatting.WHITE)), false);
            return 1;
        }

        FateTier tier = data.getCurrentTier();
        final int tierColor = tier.color;

        MutableComponent msg = Component.literal("[CityChan] ")
            .withStyle(style -> style.withColor(FateTier.MAHJONG.color))
            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" rolled ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(tierDisplayName(tier))
                .withStyle(style -> style.withColor(tierColor)));

        if (tier == FateTier.MAHJONG && data.getCurrentHand() != null) {
            msg.append(Component.literal(" — " + data.getCurrentHand().displayName)
                .withStyle(style -> style.withColor(FateTier.GODTIER.color)));
        } else if (tier == FateTier.FUNNY_NUMBERS && data.getCurrentFunnyNumber() != null) {
            msg.append(Component.literal(" — " + data.getCurrentFunnyNumber().displayName)
                .withStyle(style -> style.withColor(FateTier.FUNNY_NUMBERS.color)));
        }

        source.sendSuccess(() -> msg, false);

        List<ResourceLocation> effectIds = data.getActiveFateEffects();
        if (!effectIds.isEmpty()) {
            List<String> effectNames = new ArrayList<>();
            for (ResourceLocation id : effectIds) {
                BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(holder ->
                    effectNames.add(holder.value().getDisplayName().getString()));
            }
            if (!effectNames.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  Effects: " + String.join(", ", effectNames))
                    .withStyle(ChatFormatting.GRAY), false);
            }
        }

        return 1;
    }

    private static int executeReroll(CommandSourceStack source, ServerPlayer target) {
        if (!FateServerData.get(source.getServer()).isEnabled()) {
            source.sendFailure(Component.literal("Fate rolls are currently disabled server-wide."));
            return 0;
        }

        FateEventHandler.forceRoll(target);

        String targetName = target.getName().getString();
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Force-rolling fate for " + targetName + ".")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeAdminOptOut(CommandSourceStack source, ServerPlayer target) {
        FateData data = target.getData(FateAttachments.FATE_DATA.get());
        boolean nowOptedOut = !data.isOptedOut();
        data.setOptedOut(nowOptedOut);
        String targetName = target.getName().getString();

        if (nowOptedOut) {
            FateEventHandler.cancelPendingRoll(target.getUUID());
            FateRoller.suspendActiveEffects(target);

            source.sendSuccess(() -> Component.literal("[CityChan] ")
                .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
                .append(Component.literal(targetName + " has been opted out and their effects removed.")
                    .withStyle(ChatFormatting.WHITE)), false);
            target.sendSystemMessage(Component.literal("[CityChan] ")
                .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
                .append(Component.literal("You have been opted out of fate rolls by an administrator.")
                    .withStyle(ChatFormatting.GRAY)));
        } else {
            var server = source.getServer();
            if (FateServerData.get(server).isEnabled() && !data.getActiveFateEffects().isEmpty()) {
                FateRoller.reapplyCurrentEffects(target);
            }

            source.sendSuccess(() -> Component.literal("[CityChan] ")
                .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
                .append(Component.literal(targetName + " has been opted in to fate rolls.")
                    .withStyle(ChatFormatting.WHITE)), false);
            target.sendSystemMessage(Component.literal("[CityChan] ")
                .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
                .append(Component.literal("You have been opted in to fate rolls by an administrator.")
                    .withStyle(ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int executeAdminToggle(CommandSourceStack source) {
        var server = source.getServer();
        FateServerData serverData = FateServerData.get(server);
        boolean nowEnabled = !serverData.isEnabled();
        serverData.setEnabled(nowEnabled);

        long currentDay = server.overworld().getDayTime() / 24000;
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        if (nowEnabled) {
            for (ServerPlayer p : players) {
                FateData d = p.getData(FateAttachments.FATE_DATA.get());
                if (d.getLastRolledDay() == currentDay) {
                    FateRoller.reapplyCurrentEffects(p);
                }
            }
        } else {
            for (ServerPlayer p : players) {
                FateRoller.suspendActiveEffects(p);
            }
        }

        String togglerName = source.getEntity() instanceof ServerPlayer p
                ? p.getName().getString() : "Console";

        MutableComponent broadcast = Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Fate rolls have been ")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal(nowEnabled ? "enabled" : "disabled")
                .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED))
            .append(Component.literal(" by " + togglerName + ".")
                .withStyle(ChatFormatting.WHITE));

        server.getPlayerList().broadcastSystemMessage(broadcast, false);
        return 1;
    }

    private static int executeAdminReload(CommandSourceStack source) {
        RNGdleConfig.load(source.getServer());
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Config reloaded from disk.")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    // ── Config: list ─────────────────────────────────────────────────────────────

    private static int executeConfigListTiers(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("--- Config: Tier Weights ---")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color)), false);
        for (FateTier tier : FateTier.values()) {
            int weight = RNGdleConfig.INSTANCE.tierWeights.getOrDefault(tier.name(), 0);
            final int color = tier.color;
            final String line = tier.name() + ": " + weight;
            source.sendSuccess(() -> Component.literal("  ")
                .append(Component.literal(line).withStyle(s -> s.withColor(color))), false);
        }
        return 1;
    }

    private static int executeConfigListEffects(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("--- Config: Effects ---")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color)), false);
        for (Map.Entry<String, RNGdleConfig.EffectConfig> entry : RNGdleConfig.INSTANCE.effects.entrySet()) {
            RNGdleConfig.EffectConfig cfg = entry.getValue();
            String name = entry.getKey();
            String line = String.format("  %s: enabled=%s, tier=%s, weight=%d, amp=%d",
                name, cfg.enabled, cfg.tier, cfg.weight, cfg.amplifier);
            ChatFormatting color = cfg.enabled ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY;
            source.sendSuccess(() -> Component.literal(line).withStyle(color), false);
        }
        return 1;
    }

    // ── Config: get ──────────────────────────────────────────────────────────────

    private static int executeConfigGetTier(CommandSourceStack source, String tierName) {
        FateTier tier = parseTier(tierName);
        if (tier == null) {
            source.sendFailure(Component.literal("Unknown tier: " + tierName));
            return 0;
        }
        int weight = RNGdleConfig.INSTANCE.tierWeights.getOrDefault(tier.name(), 0);
        final int color = tier.color;
        source.sendSuccess(() -> Component.literal(tier.name() + ": ")
            .withStyle(s -> s.withColor(color))
            .append(Component.literal("weight=" + weight).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigGetEffect(CommandSourceStack source, String effectName) {
        RNGdleConfig.EffectConfig cfg = RNGdleConfig.INSTANCE.effects.get(effectName);
        if (cfg == null || !FateEffectPools.NAME_TO_EFFECT.containsKey(effectName)) {
            source.sendFailure(Component.literal("Unknown effect: " + effectName));
            return 0;
        }
        ChatFormatting color = cfg.enabled ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
        String detail = String.format("enabled=%s, tier=%s, weight=%d, amp=%d", cfg.enabled, cfg.tier, cfg.weight, cfg.amplifier);
        source.sendSuccess(() -> Component.literal(effectName + ": ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(detail).withStyle(color)), false);
        return 1;
    }

    // ── Config: set ──────────────────────────────────────────────────────────────

    private static int executeConfigSetTierWeight(CommandSourceStack source, String tierName, int weight) {
        FateTier tier = parseTier(tierName);
        if (tier == null) {
            source.sendFailure(Component.literal("Unknown tier: " + tierName));
            return 0;
        }
        RNGdleConfig.INSTANCE.tierWeights.put(tier.name(), weight);
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set " + tier.name() + " weight → " + weight)
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigSetEffectEnabled(CommandSourceStack source, String effectName, boolean enabled) {
        RNGdleConfig.EffectConfig cfg = getEffectConfig(source, effectName);
        if (cfg == null) return 0;
        cfg.enabled = enabled;
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set " + effectName + " enabled → " + enabled)
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigSetEffectTier(CommandSourceStack source, String effectName, String tierName) {
        RNGdleConfig.EffectConfig cfg = getEffectConfig(source, effectName);
        if (cfg == null) return 0;
        FateTier tier = parseTier(tierName);
        if (tier == null) {
            source.sendFailure(Component.literal("Unknown tier: " + tierName));
            return 0;
        }
        cfg.tier = tier.name();
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set " + effectName + " tier → " + tier.name())
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigSetEffectWeight(CommandSourceStack source, String effectName, int weight) {
        RNGdleConfig.EffectConfig cfg = getEffectConfig(source, effectName);
        if (cfg == null) return 0;
        cfg.weight = weight;
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set " + effectName + " weight → " + weight)
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigSetEffectAmplifier(CommandSourceStack source, String effectName, int amplifier) {
        RNGdleConfig.EffectConfig cfg = getEffectConfig(source, effectName);
        if (cfg == null) return 0;
        cfg.amplifier = amplifier;
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set " + effectName + " amplifier → " + amplifier + " (level " + (amplifier + 1) + ")")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int executeConfigSetDefaultOptedIn(CommandSourceStack source, boolean value) {
        RNGdleConfig.INSTANCE.defaultOptedIn = value;
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Set defaultOptedIn → " + value)
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    // ── Config: set preset ───────────────────────────────────────────────────────

    private static int executeConfigSetPreset(CommandSourceStack source, String presetName) {
        String upper = presetName.toUpperCase();
        if (!RNGdleConfig.VALID_PRESETS.contains(upper)) {
            source.sendFailure(Component.literal(
                "Unknown preset: " + presetName + " — valid: " + String.join(", ", RNGdleConfig.VALID_PRESETS)));
            return 0;
        }
        RNGdleConfig.applyPreset(upper);
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Applied preset " + upper + ". Use /rngdle admin config save to persist.")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    // ── Config: save ─────────────────────────────────────────────────────────────

    private static int executeConfigSave(CommandSourceStack source) {
        RNGdleConfig.save(source.getServer());
        source.sendSuccess(() -> Component.literal("[CityChan] ")
            .withStyle(s -> s.withColor(FateTier.MAHJONG.color))
            .append(Component.literal("Config saved to disk.")
                .withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    @Nullable
    private static FateTier parseTier(String name) {
        for (FateTier t : FateTier.values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    @Nullable
    private static RNGdleConfig.EffectConfig getEffectConfig(CommandSourceStack source, String effectName) {
        if (!FateEffectPools.NAME_TO_EFFECT.containsKey(effectName)) {
            source.sendFailure(Component.literal("Unknown effect: " + effectName));
            return null;
        }
        RNGdleConfig.EffectConfig cfg = RNGdleConfig.INSTANCE.effects.get(effectName);
        if (cfg == null) {
            source.sendFailure(Component.literal("Effect not found in config: " + effectName));
            return null;
        }
        return cfg;
    }

    @Nullable
    private static ServerPlayer resolvePlayer(CommandSourceStack source, @Nullable ServerPlayer target) {
        if (target != null) return target;
        if (source.getEntity() instanceof ServerPlayer self) return self;
        source.sendFailure(Component.literal("Specify a player or run as a player."));
        return null;
    }

    private static String tierDisplayName(FateTier tier) {
        return switch (tier) {
            case GODTIER       -> "Godtier";
            case MAHJONG       -> "Mahjong";
            case FUNNY_NUMBERS -> "Funny Numbers";
            default -> tier.name().charAt(0) + tier.name().substring(1).toLowerCase();
        };
    }

    private FateCommands() {}
}
