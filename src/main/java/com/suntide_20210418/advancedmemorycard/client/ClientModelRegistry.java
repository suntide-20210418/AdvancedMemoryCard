package com.suntide_20210418.advancedmemorycard.client;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 客户端模型注册器。
 *
 * <p>显式注册 Advanced Memory Card 的物品模型，确保物品动态贴图（.mcmeta 动画）能被
 * Forge 正确加载与烘焙。仅通过 {@link net.minecraft.item.Item#setRegistryName} 的
 * 隐式模型回退在某些情况下无法正确应用动画贴图，因此需要在
 * {@link ModelRegistryEvent} 中显式注册。</p>
 */
@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Side.CLIENT)
public class ClientModelRegistry {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItemModel(ModItems.ADVANCED_MEMORY_CARD);
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
