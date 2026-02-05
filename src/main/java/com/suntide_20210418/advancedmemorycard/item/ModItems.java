package com.suntide_20210418.advancedmemorycard.item;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, AdvancedMemoryCardMod.MOD_ID);

  public static final RegistryObject<Item> ADVANCED_MEMORY_CARD =
      ITEMS.register(
          "advanced_memory_card", () -> new AdvancedMemoryCardItem(new Item.Properties()));

  public static void register(IEventBus eventBus) {
    ITEMS.register(eventBus);
  }
}
