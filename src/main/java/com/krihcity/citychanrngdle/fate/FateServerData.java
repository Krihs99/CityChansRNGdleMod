package com.krihcity.citychanrngdle.fate;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class FateServerData extends SavedData {

    private static final String NAME = "citychanrngdle";

    public static final Factory<FateServerData> FACTORY = new Factory<>(
        FateServerData::new,
        FateServerData::load,
        null
    );

    private boolean enabled = true;

    public static FateServerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setDirty();
    }

    private static FateServerData load(CompoundTag tag, HolderLookup.Provider provider) {
        FateServerData data = new FateServerData();
        data.enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("enabled", enabled);
        return tag;
    }
}
