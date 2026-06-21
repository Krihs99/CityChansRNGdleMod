package com.krihcity.citychanrngdle.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WeepingAngelEffect extends MobEffect {

    private static final int WATCH_RANGE = 16;
    private static final double ANGLE_THRESHOLD = 15.0;
    private static final Map<UUID, Vec3> frozenPositions = new ConcurrentHashMap<>();

    public WeepingAngelEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!(player.level() instanceof ServerLevel level)) return true;

        if (isObserved(player, level)) {
            Vec3 frozen = frozenPositions.computeIfAbsent(
                    player.getUUID(), k -> player.position());
            player.connection.teleport(frozen.x, frozen.y, frozen.z,
                    player.getYRot(), player.getXRot());
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            frozenPositions.put(player.getUUID(), player.position());
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }

    public static void clearFrozenPosition(UUID uuid) {
        frozenPositions.remove(uuid);
    }

    private boolean isObserved(ServerPlayer target, ServerLevel level) {
        return level.players().stream()
                .filter(p -> p != target)
                .filter(p -> p.distanceTo(target) <= WATCH_RANGE)
                .anyMatch(p -> isLookingAt(p, target) && hasLineOfSight(p, target, level));
    }

    private boolean isLookingAt(ServerPlayer observer, ServerPlayer target) {
        Vec3 look = observer.getLookAngle();
        Vec3 direction = target.getEyePosition()
                .subtract(observer.getEyePosition())
                .normalize();
        double dot = Math.max(-1.0, Math.min(1.0, look.dot(direction)));
        return Math.toDegrees(Math.acos(dot)) < ANGLE_THRESHOLD;
    }

    private boolean hasLineOfSight(ServerPlayer observer, ServerPlayer target, ServerLevel level) {
        ClipContext ctx = new ClipContext(
                observer.getEyePosition(),
                target.getEyePosition(),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                observer);
        return level.clip(ctx).getType() == HitResult.Type.MISS;
    }
}
