package com.suntide_20210418.advancedmemorycard.client.key;

import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.network.ModeSwitchPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class KeyEvent {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKey.MODE_SWITCH_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack offhandItem = player.getOffhandItem();
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.getItem().equals(ModItems.ADVANCED_MEMORY_CARD.get())){
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.MAIN_HAND));
            } else if (offhandItem.getItem().equals(ModItems.ADVANCED_MEMORY_CARD.get())){
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.OFF_HAND));
            }
        }
    }
}
