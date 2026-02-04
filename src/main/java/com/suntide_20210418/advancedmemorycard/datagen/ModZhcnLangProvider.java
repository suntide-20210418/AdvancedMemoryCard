package com.suntide_20210418.advancedmemorycard.datagen;


import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModZhcnLangProvider extends LanguageProvider {

    public ModZhcnLangProvider(PackOutput pOutput) {
        super(pOutput, AdvancedMemoryCardMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ModItems.ADVANCED_MEMORY_CARD.get(), "高级内存卡");
    }
}