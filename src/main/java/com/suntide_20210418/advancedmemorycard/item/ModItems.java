package com.suntide_20210418.advancedmemorycard.item;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AdvancedMemoryCardMod.MOD_ID);

    public static final DeferredHolder<Item, AdvancedMemoryCardItem> ADVANCED_MEMORY_CARD =
            ITEMS.register(
                    "advanced_memory_card",
                    () -> new AdvancedMemoryCardItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
