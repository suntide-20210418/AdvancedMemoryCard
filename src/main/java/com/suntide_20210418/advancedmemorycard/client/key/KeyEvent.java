package com.suntide_20210418.advancedmemorycard.client.key;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.network.ModeSwitchPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Side.CLIENT)
public class KeyEvent {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        if (ModKey.MODE_SWITCH_KEY.isPressed()) {
            ItemStack main = mc.player.getHeldItemMainhand();
            ItemStack off = mc.player.getHeldItemOffhand();
            if (main.getItem() == ModItems.ADVANCED_MEMORY_CARD) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(EnumHand.MAIN_HAND));
            } else if (off.getItem() == ModItems.ADVANCED_MEMORY_CARD) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(EnumHand.OFF_HAND));
            }
        }

//        if (ModKey.COPY_GUI_KEY.isPressed()) {
//            ItemStack main = mc.player.getHeldItemMainhand();
//            ItemStack off = mc.player.getHeldItemOffhand();
//            if (main.getItem() == ModItems.ADVANCED_MEMORY_CARD) {
//                NetworkHandler.sendToServer(new OpenCopyGuiPacket(EnumHand.MAIN_HAND));
//            } else if (off.getItem() == ModItems.ADVANCED_MEMORY_CARD) {
//                NetworkHandler.sendToServer(new OpenCopyGuiPacket(EnumHand.OFF_HAND));
//            }
//        }
    }
}
