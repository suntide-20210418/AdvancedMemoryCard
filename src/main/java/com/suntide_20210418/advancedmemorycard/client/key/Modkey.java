package com.suntide_20210418.advancedmemorycard.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Modkey {
    // 按键分类（自定义）
    public static final String KEY_CATEGORY = "key.categories.advanced_memory_card";

    // 1. 创建按键绑定
    public static final KeyMapping CUSTOM_KEY = new KeyMapping(
            "key.advancedmemorycard.custom", // 按键翻译键
            InputConstants.Type.KEYSYM,      // 按键类型
            GLFW.GLFW_KEY_R,                 // 默认键位（R键）
            KEY_CATEGORY            // 按键分类
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(CUSTOM_KEY);
    }
}
