package me.basiqueevangelist.onedatastore.api;

import net.minecraft.nbt.NbtCompound;

public interface ComponentInstance {
    /**
     * Called component load if the component isn't present in the data entry.
     * Can be used to run data migration.
     */
    default void wasMissing() {

    }

    void fromTag(NbtCompound tag);

    NbtCompound toTag(NbtCompound tag);
}
