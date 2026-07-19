package com.suntide_20210418.advancedmemorycard.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;

import appeng.api.AEApi;
import appeng.api.definitions.Items;
import appeng.api.definitions.Materials;
import cpw.mods.fml.common.registry.GameRegistry;

public class ModItems {

    public static final Item ADVANCED_MEMORY_CARD = new AdvancedMemoryCardItem();

    public static void register() {
        GameRegistry.registerItem(ADVANCED_MEMORY_CARD, "advanced_memory_card");
        registerRecipes();
    }

    /**
     * 1.7.10 没有 JSON 配方系统，使用 GameRegistry 添加配方。
     * 对应原 advanced_memory_card.json 的无序合成：
     * AE2 内存卡 + 逻辑处理器 + 计算处理器 + 工程处理器 -> 高级内存卡
     */
    private static void registerRecipes() {
        final Items items = AEApi.instance()
            .items();
        final Materials materials = AEApi.instance()
            .materials();

        final ItemStack memoryCard = items.itemMemoryCard.stack(1);
        final ItemStack logicProcessor = materials.materialLogicProcessor.stack(1);
        final ItemStack calcProcessor = materials.materialCalcProcessor.stack(1);
        final ItemStack engProcessor = materials.materialEngProcessor.stack(1);

        // 任一原料不可用（例如对应 AE2 特性被禁用）时跳过，避免崩溃
        if (memoryCard == null || logicProcessor == null || calcProcessor == null || engProcessor == null) {
            return;
        }

        GameRegistry.addShapelessRecipe(
            new ItemStack(ADVANCED_MEMORY_CARD),
            memoryCard,
            logicProcessor,
            calcProcessor,
            engProcessor);
    }
}
