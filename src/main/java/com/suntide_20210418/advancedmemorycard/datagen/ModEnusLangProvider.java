package com.suntide_20210418.advancedmemorycard.datagen;


import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys;

public class ModEnusLangProvider extends LanguageProvider {

    public ModEnusLangProvider(PackOutput pOutput) {
        super(pOutput, AdvancedMemoryCardMod.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 物品相关
        add(Keys.ITEM_ADVANCED_MEMORY_CARD, "Advanced Memory Card");
        
        // 物品组相关
        add(Keys.ITEM_GROUP_TAB, "Advanced Memory Card");
        
        // 复制模式工具提示
        add(Keys.TOOLTIP_COPY_INFO, "Copy Mode\n");
        add(Keys.TOOLTIP_COPY_FIRST_POS, "First position:%s\n");
        add(Keys.TOOLTIP_COPY_SECOND_POS, "Second position:%s\n");
        add(Keys.TOOLTIP_COPY_READY, "Ready to paste");
        
        // 配置模式显示
        add(Keys.CONFIG_MODE_SHOW, "Config Mode");
        
        // 复制模式显示
        add(Keys.COPY_MODE_SHOW, "Copy Mode");
        
        // 复制模式玩家消息
        add(Keys.COPY_MODE_FIRST_POS_MARKED, "First Position[%s] Marked");
        add(Keys.COPY_MODE_SECOND_POS_MARKED, "Second Position[%s] Marked, Current area:[%s,%s,%s]");
        add(Keys.COPY_MODE_TOO_LARGE, "Too large area,Max area is:%s,current area is:%s");
        add(Keys.COPY_MODE_COMPLETED, "Pasted %s blocks");
        add(Keys.COPY_MODE_ALREADY_MARKED, "This position has already been marked, please remark it");
        add(Keys.COPY_MODE_FAILED, "You have not marked first or second position, please mark first");
        
        // 配置模式工具提示
        add(Keys.TOOLTIP_CONFIG_INFO, "Config Mode\n");
    }
}