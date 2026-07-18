package com.suntide_20210418.advancedmemorycard.item;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import net.minecraft.item.Item;
import cpw.mods.fml.common.registry.GameRegistry;

public class ModItems {
    public static final Item ADVANCED_MEMORY_CARD = new AdvancedMemoryCardItem();

    public static void register() {
        GameRegistry.registerItem(ADVANCED_MEMORY_CARD, "advanced_memory_card");
    }
}
