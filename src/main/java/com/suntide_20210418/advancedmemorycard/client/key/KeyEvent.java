package com.suntide_20210418.advancedmemorycard.client.key;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.network.ModeSwitchPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;

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
    }
}
