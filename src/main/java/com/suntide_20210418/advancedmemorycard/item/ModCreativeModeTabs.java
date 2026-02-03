package com.suntide_20210418.advancedmemorycard.item;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AdvancedMemoryCardMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ADVANCED_MEMORY_CARD_TAB =
            CREATIVE_MODE_TABS.register("advanced_memory_card_tab", () -> CreativeModeTab.builder()
                        .icon(() -> new ItemStack(ModItems.ADVANCED_MEMORY_CARD.get()))
                        .title(Component.translatable("itemGroup.advanced_memory_card_tab"))
                        .displayItems((parameters, output) -> {
                            output.accept(ModItems.ADVANCED_MEMORY_CARD.get());
                        })
                        .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
