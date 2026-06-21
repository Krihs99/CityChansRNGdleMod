package com.krihcity.citychanrngdle;

import com.krihcity.citychanrngdle.effect.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CityChanRNGdle.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModCommonEvents {

    // Standard block friction (0.6) * vanilla dampener (0.91)
    private static final double GROUND_F = 0.6 * 0.91;   // ~0.546
    private static final double ICE_F    = 0.98 * 0.91;  // ~0.8918

    // On ice, input acceleration is scaled down by (groundF^3 / iceF^3) — vanilla formula
    private static final double INPUT_SCALE = Math.pow(GROUND_F, 3) / Math.pow(ICE_F, 3); // ~0.23

    private static final double MAX_SLIP_SPEED = 1.5;

    private static final Map<UUID, Vec3> preTickVelocity = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.onGround()) return;
        if (!player.hasEffect(ModEffects.SLIPPERY)) return;
        preTickVelocity.put(player.getUUID(), player.getDeltaMovement());
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Always clean up saved velocity regardless of early returns
        Vec3 vBefore = preTickVelocity.remove(player.getUUID());

        if (vBefore == null) return;
        if (!player.onGround()) return;
        if (!player.hasEffect(ModEffects.SLIPPERY)) return;

        // Don't stack with naturally slippery blocks (ice, packed ice, blue ice)
        if (player.getBlockStateOn().getBlock().getFriction() > 0.6f) return;

        Vec3 vAfter = player.getDeltaMovement();

        // Undo vanilla ground friction to find velocity before friction was applied
        double preFricX = vAfter.x / GROUND_F;
        double preFricZ = vAfter.z / GROUND_F;

        // Isolate the input acceleration that was applied this tick
        double inputX = preFricX - vBefore.x;
        double inputZ = preFricZ - vBefore.z;

        // Re-simulate as ice: reduce input control, retain much more momentum
        double newX = (vBefore.x + inputX * INPUT_SCALE) * ICE_F;
        double newZ = (vBefore.z + inputZ * INPUT_SCALE) * ICE_F;

        double horizSpeed = Math.sqrt(newX * newX + newZ * newZ);
        if (horizSpeed > MAX_SLIP_SPEED) {
            double scale = MAX_SLIP_SPEED / horizSpeed;
            newX *= scale;
            newZ *= scale;
        }

        player.setDeltaMovement(newX, vAfter.y, newZ);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(ModEffects.FRAIL)) return;
        event.setNewDamage(event.getNewDamage() * 1.5f);
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasEffect(ModEffects.LIGHTWEIGHT)) return;
        event.setStrength(event.getStrength() * 3.0f);
    }
}
