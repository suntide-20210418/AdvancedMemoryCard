package com.suntide_20210418.advancedmemorycard.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Dist.CLIENT)
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

    // 2. 打开复制模式选项界面（默认 G 键）
    //    键位可在「选项 → 控制 → 按键绑定 → 高级内存卡」中修改，
    //    因此代码里只通过 KeyMapping 判断，不要直接比较 GLFW 键码。
    public static final KeyMapping COPY_OPTIONS_KEY = new KeyMapping(
            "key.advancedmemorycard.copy_options",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(MODE_SWITCH_KEY);
        event.register(COPY_OPTIONS_KEY);
    }
}
