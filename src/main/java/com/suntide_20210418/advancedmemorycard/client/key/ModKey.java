package com.suntide_20210418.advancedmemorycard.client.key;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * 客户端按键绑定。本类仅在客户端被加载（由 KeyEvent 的 @Mod.EventBusSubscriber(value = CLIENT) 触发）。
 */
public class ModKey {
    public static final String KEY_CATEGORY = "key.categories.advanced_memory_card";

    // 切换记忆卡模式（对应 1.20.1 的 V 键）
    public static final KeyBinding MODE_SWITCH_KEY =
            new KeyBinding("key.advancedmemorycard.switch", Keyboard.KEY_V, KEY_CATEGORY);

    // 打开复制模式 GUI（1.20.1 中 OpenGuiPacket 对应功能，这里绑定到 G 键）
//    public static final KeyBinding COPY_GUI_KEY =
//            new KeyBinding("key.advancedmemorycard.copy_gui", Keyboard.KEY_G, KEY_CATEGORY);

    static {
        ClientRegistry.registerKeyBinding(MODE_SWITCH_KEY);
//        ClientRegistry.registerKeyBinding(COPY_GUI_KEY);
    }
}
