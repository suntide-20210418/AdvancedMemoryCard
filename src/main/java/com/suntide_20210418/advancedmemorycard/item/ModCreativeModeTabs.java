package com.suntide_20210418.advancedmemorycard.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ModCreativeModeTabs {

    public static final CreativeTabs ADVANCED_MEMORY_CARD_TAB = new CreativeTabs("advanced_memory_card") {

        @Override
        public Item getTabIconItem() {
            return ModItems.ADVANCED_MEMORY_CARD;
        }
    };
}
