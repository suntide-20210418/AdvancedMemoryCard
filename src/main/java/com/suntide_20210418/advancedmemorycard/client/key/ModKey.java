package com.suntide_20210418.advancedmemorycard.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID)
public class ModKey {
    // 按键分类（自定义）
    public static final String KEY_CATEGORY = "key.categories.advanced_memory_card";

    // 1. 创建按键绑定
    public static final KeyMapping MODE_SWITCH_KEY = new KeyMapping(
            "key.advancedmemorycard.switch", // 按键翻译键
            InputConstants.Type.KEYSYM,      // 按键类型
            GLFW.GLFW_KEY_V,                 // 默认键位（V键）
            KEY_CATEGORY            // 按键分类
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(MODE_SWITCH_KEY);
    }
}
