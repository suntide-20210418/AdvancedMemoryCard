package com.suntide_20210418.advancedmemorycard.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class ModCreativeModeTabs {
    public static final CreativeTabs ADVANCED_MEMORY_CARD_TAB = new CreativeTabs("advanced_memory_card.tab") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(ModItems.ADVANCED_MEMORY_CARD);
        }
    };
}
