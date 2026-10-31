package com.suntide_20210418.advancedmemorycard.client.key;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.network.ModeSwitchPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.network.OpenCopyGuiPacket;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/**
 * 客户端按键事件处理。以实例形式注册到 EVENT_BUS（1.7.10 无 @Mod.EventBusSubscriber）。
 */
public class KeyEvent {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        if (ModKey.MODE_SWITCH_KEY.isPressed()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null && held.getItem() == ModItems.ADVANCED_MEMORY_CARD) {
                NetworkHandler.sendToServer(new ModeSwitchPacket());
            }
        }

        if (ModKey.COPY_GUI_KEY.isPressed()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            // 只有手持高级内存卡且处于 Copy 模式时才允许请求开面板，服务端还会再复核一次
            if (held != null && held.getItem() == ModItems.ADVANCED_MEMORY_CARD
                && CardMode.of(held) instanceof CopyMode) {
                NetworkHandler.sendToServer(new OpenCopyGuiPacket());
            }
        }
    }
}
