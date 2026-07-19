package com.suntide_20210418.advancedmemorycard.item.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;

public abstract class CardMode {

    private static final Map<String, Supplier<CardMode>> REGISTRY = new HashMap<>();
    private static final TreeSet<String> CYCLE_ORDER = new TreeSet<>();
    private static final String MODE_TYPE = "type";
    private static final String MODE_DATA = "mode_data";

    public static CardMode of(ItemStack stack) {
        NBTTagCompound data = getData(stack);
        String typeStr = data.getString(MODE_TYPE);
        Supplier<CardMode> supplier = typeStr == null || typeStr.isEmpty() ? null : REGISTRY.get(typeStr);
        if (supplier == null) {
            return new CopyMode();
        } else {
            return supplier.get()
                .load(data);
        }
    }

    /** Extract the {@link NBTTagCompound} for this mode's data */
    public static NBTTagCompound getData(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? new NBTTagCompound() : tag.getCompoundTag(MODE_DATA);
    }

    /** Get or create the root tag of an ItemStack (rv6 has no getOrCreateTag) */
    public static NBTTagCompound getOrCreateRoot(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    public static void register(String key, Supplier<CardMode> supplier) {
        REGISTRY.put(key, supplier);
        CYCLE_ORDER.add(key);
    }

    public static void initializeModes() {
        if (REGISTRY.isEmpty()) { // avoid duplicate registration
            register(AdvancedMemoryCardMod.MOD_ID + ":copy", CopyMode::new);
            register(AdvancedMemoryCardMod.MOD_ID + ":config", ConfigMode::new);
        }
    }

    public abstract String getType();

    public static CardMode cycleMode(CardMode mode) {
        if (CYCLE_ORDER.isEmpty()) {
            return mode;
        }
        String current = mode.getType();
        String next = CYCLE_ORDER.higher(current);
        if (next == null) {
            next = CYCLE_ORDER.first();
        }
        Supplier<CardMode> supplier = REGISTRY.get(next);
        return supplier == null ? mode : supplier.get();
    }

    /**
     * Called from onItemUseFirst in AdvancedMemoryCardItem, before the item is actually
     * used on the part/block so it can be cancelled or modified.
     */
    public abstract boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
        int side, float hitX, float hitY, float hitZ);

    public NBTTagCompound save(NBTTagCompound tag) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(MODE_TYPE, getType());
        tag.setTag(MODE_DATA, data);
        return data;
    }

    protected CardMode load(NBTTagCompound tag) {
        return this;
    }

    public abstract String getName();

    protected abstract String getDescription();

    /** Called from onItemRightClick in AdvancedMemoryCardItem */
    public abstract ItemStack onItemUse(World world, EntityPlayer player);
}
