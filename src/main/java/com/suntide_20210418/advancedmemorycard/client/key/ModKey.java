package com.suntide_20210418.advancedmemorycard.client.key;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * 客户端按键绑定。本类仅在客户端被加载。
 */
public class ModKey {

    public static final String KEY_CATEGORY = "key.categories.advanced_memory_card";

    // 切换记忆卡模式（对应 1.20.1 的 V 键）
    public static final KeyBinding MODE_SWITCH_KEY = new KeyBinding(
        "key.advancedmemorycard.switch",
        Keyboard.KEY_V,
        KEY_CATEGORY);

    // 打开粘贴选项面板（对应 1.12.2 的 G 键）
    public static final KeyBinding COPY_GUI_KEY = new KeyBinding(
        "key.advancedmemorycard.copy_options",
        Keyboard.KEY_G,
        KEY_CATEGORY);

    public static void register() {
        ClientRegistry.registerKeyBinding(MODE_SWITCH_KEY);
        ClientRegistry.registerKeyBinding(COPY_GUI_KEY);
    }
}
