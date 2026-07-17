package com.suntide_20210418.advancedmemorycard.item;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;

public class ModItems {
    public static final Item ADVANCED_MEMORY_CARD = new AdvancedMemoryCardItem();

    public static void register(IForgeRegistry<Item> registry) {
        registry.register(ADVANCED_MEMORY_CARD);
    }
}
