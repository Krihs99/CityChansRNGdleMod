package com.krihcity.citychanrngdle.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BrokenLegsEffect extends MobEffect {
    public BrokenLegsEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            ResourceLocation.fromNamespaceAndPath("citychanrngdle", "broken_legs_jump"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
