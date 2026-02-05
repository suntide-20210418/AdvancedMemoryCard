package com.suntide_20210418.advancedmemorycard.datagen;


import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Keys;

public class ModZhcnLangProvider extends LanguageProvider {

    public ModZhcnLangProvider(PackOutput pOutput) {
        super(pOutput, AdvancedMemoryCardMod.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // 物品相关
        add(Keys.ITEM_ADVANCED_MEMORY_CARD, "高级内存卡");
        
        // 物品组相关
        add(Keys.ITEM_GROUP_TAB, "高级内存卡");
        
        // 复制模式工具提示
        add(Keys.TOOLTIP_COPY_INFO, "复制模式\n");
        add(Keys.TOOLTIP_COPY_FIRST_POS, "第一个位置：%s\n");
        add(Keys.TOOLTIP_COPY_SECOND_POS, "第二个位置：%s\n");
        add(Keys.TOOLTIP_COPY_READY, "准备粘贴");
        
        // 配置模式显示
        add(Keys.CONFIG_MODE_SHOW, "配置模式");
        
        // 复制模式显示
        add(Keys.COPY_MODE_SHOW, "复制模式");
        
        // 复制模式玩家消息
        add(Keys.COPY_MODE_FIRST_POS_MARKED, "第一个位置[%s]已标记");
        add(Keys.COPY_MODE_SECOND_POS_MARKED, "第二个位置[%s]已标记，待复制区域大小:[%s,%s,%s]");
        add(Keys.COPY_MODE_TOO_LARGE, "范围过大(%s>%s)");
        add(Keys.COPY_MODE_COMPLETED, "已粘贴%s个方块的配置");
        add(Keys.COPY_MODE_ALREADY_MARKED, "区域已标记");
        add(Keys.COPY_MODE_FAILED, "你还没有标记第一个或第二个方块的位置，请先标记位置");
        
        // 配置模式工具提示
        add(Keys.TOOLTIP_CONFIG_INFO, "配置模式\n");
    }
}