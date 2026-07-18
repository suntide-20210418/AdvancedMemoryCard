package com.suntide_20210418.advancedmemorycard.client.key;

import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * 客户端按键绑定。本类仅在客户端被加载。
 */
public class ModKey {
    public static final String KEY_CATEGORY = "key.categories.advanced_memory_card";

    // 切换记忆卡模式（对应 1.20.1 的 V 键）
    public static final KeyBinding MODE_SWITCH_KEY =
            new KeyBinding("key.advancedmemorycard.switch", Keyboard.KEY_V, KEY_CATEGORY);

    public static void register() {
        ClientRegistry.registerKeyBinding(MODE_SWITCH_KEY);
    }
}
