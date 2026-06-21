package com.krihcity.citychanrngdle.fate;

import com.krihcity.citychanrngdle.config.RNGdleConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FateData implements INBTSerializable<CompoundTag> {

    private long lastRolledDay = -1;
    private long lastOptedOutDay = -1;
    private boolean optedOut = !RNGdleConfig.INSTANCE.defaultOptedIn;
    private boolean hasSeenIntro = false;
    private final Map<FateTier, Integer> tierRollCounts = new EnumMap<>(FateTier.class);
    private @Nullable FateTier currentTier = null;
    private @Nullable MahjongHand currentHand = null;
    private @Nullable FunnyNumber currentFunnyNumber = null;
    private final List<ResourceLocation> activeFateEffects = new ArrayList<>();

    public long getLastRolledDay() { return lastRolledDay; }
    public void setLastRolledDay(long day) { this.lastRolledDay = day; }

    public long getLastOptedOutDay() { return lastOptedOutDay; }
    public void setLastOptedOutDay(long day) { this.lastOptedOutDay = day; }

    public boolean isOptedOut() { return optedOut; }
    public void setOptedOut(boolean optedOut) { this.optedOut = optedOut; }

    public boolean hasSeenIntro() { return hasSeenIntro; }
    public void setHasSeenIntro(boolean seen) { this.hasSeenIntro = seen; }

    public int getTierCount(FateTier tier) { return tierRollCounts.getOrDefault(tier, 0); }
    public void incrementTierCount(FateTier tier) { tierRollCounts.merge(tier, 1, Integer::sum); }
    public int getTotalRolls() { return tierRollCounts.values().stream().mapToInt(Integer::intValue).sum(); }

    public @Nullable FateTier getCurrentTier() { return currentTier; }
    public void setCurrentTier(@Nullable FateTier tier) { this.currentTier = tier; }

    public @Nullable MahjongHand getCurrentHand() { return currentHand; }
    public void setCurrentHand(@Nullable MahjongHand hand) { this.currentHand = hand; }

    public @Nullable FunnyNumber getCurrentFunnyNumber() { return currentFunnyNumber; }
    public void setCurrentFunnyNumber(@Nullable FunnyNumber number) { this.currentFunnyNumber = number; }

    public List<ResourceLocation> getActiveFateEffects() { return activeFateEffects; }

    public boolean isActiveFateEffect(ResourceLocation effectId) {
        return activeFateEffects.contains(effectId);
    }

    public void setActiveFateEffects(List<ResourceLocation> effects) {
        activeFateEffects.clear();
        activeFateEffects.addAll(effects);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastRolledDay", lastRolledDay);
        tag.putLong("lastOptedOutDay", lastOptedOutDay);
        tag.putBoolean("optedOut", optedOut);
        tag.putBoolean("hasSeenIntro", hasSeenIntro);
        CompoundTag counts = new CompoundTag();
        for (Map.Entry<FateTier, Integer> entry : tierRollCounts.entrySet())
            counts.putInt(entry.getKey().name(), entry.getValue());
        tag.put("tierRollCounts", counts);
        if (currentTier != null)        tag.putString("currentTier",        currentTier.name());
        if (currentHand != null)        tag.putString("currentHand",        currentHand.name());
        if (currentFunnyNumber != null) tag.putString("currentFunnyNumber", currentFunnyNumber.name());
        ListTag effects = new ListTag();
        for (ResourceLocation id : activeFateEffects) {
            effects.add(StringTag.valueOf(id.toString()));
        }
        tag.put("activeFateEffects", effects);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        lastRolledDay = tag.getLong("lastRolledDay");
        lastOptedOutDay = tag.getLong("lastOptedOutDay");
        // If key absent (new player), use server's defaultOptedIn config; existing players keep their saved value
        optedOut = !tag.contains("optedOut") ? !RNGdleConfig.INSTANCE.defaultOptedIn : tag.getBoolean("optedOut");
        hasSeenIntro = tag.getBoolean("hasSeenIntro");
        tierRollCounts.clear();
        if (tag.contains("tierRollCounts")) {
            CompoundTag counts = tag.getCompound("tierRollCounts");
            for (FateTier tier : FateTier.values())
                if (counts.contains(tier.name())) tierRollCounts.put(tier, counts.getInt(tier.name()));
        }
        currentTier = tag.contains("currentTier")
                ? parseSafe(FateTier.values(), tag.getString("currentTier"))
                : null;
        currentHand = tag.contains("currentHand")
                ? parseSafe(MahjongHand.values(), tag.getString("currentHand"))
                : null;
        currentFunnyNumber = tag.contains("currentFunnyNumber")
                ? parseSafe(FunnyNumber.values(), tag.getString("currentFunnyNumber"))
                : null;
        activeFateEffects.clear();
        ListTag effects = tag.getList("activeFateEffects", Tag.TAG_STRING);
        for (int i = 0; i < effects.size(); i++) {
            activeFateEffects.add(ResourceLocation.parse(effects.getString(i)));
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> @Nullable E parseSafe(E[] values, String name) {
        for (E v : values) if (v.name().equals(name)) return v;
        return null;
    }
}
