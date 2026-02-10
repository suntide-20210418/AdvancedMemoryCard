package com.suntide_20210418.advancedmemorycard.item;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Common;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdvancedMemoryCardMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ADVANCED_MEMORY_CARD_TAB =
            CREATIVE_MODE_TABS.register(
                    "advanced_memory_card_tab",
                    () ->
                            CreativeModeTab.builder()
                                    .icon(() -> new ItemStack(ModItems.ADVANCED_MEMORY_CARD.get()))
                                    .title(Common.itemGroupName())
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ModItems.ADVANCED_MEMORY_CARD.get());
                                            })
                                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
