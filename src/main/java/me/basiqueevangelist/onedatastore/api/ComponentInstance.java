package me.basiqueevangelist.onedatastore.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

public interface ComponentInstance {
    /**
     * Called component load if the component isn't present in the data entry.
     * Can be used to run data migration.
     */
    default void wasMissing() {

    }

    void fromTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries);

    NbtCompound toTag(NbtCompound tag, RegistryWrapper.WrapperLookup registries);
}
