package com.suntide_20210418.advancedmemorycard.client.key;

import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.network.ModeSwitchPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.network.OpenGuiPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyEvent {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 按下期间 GLFW 会持续发 REPEAT，而原版对每个 REPEAT 都会 KeyMapping.click() 入队一次，
        // 不判 PRESS 的话按住 V 会疯狂切模式、按住 G 会反复请求打开界面。
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // 界面打开时原版根本不会给 KeyMapping 入队，这里再把事件本身挡掉，避免二次打开。
        if (player == null || minecraft.screen != null) {
            return;
        }

        if (ModKey.MODE_SWITCH_KEY.consumeClick()) {
            ItemStack offhandItem = player.getOffhandItem();
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.is(ModItems.ADVANCED_MEMORY_CARD.get())) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.MAIN_HAND));
            } else if (offhandItem.is(ModItems.ADVANCED_MEMORY_CARD.get())) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.OFF_HAND));
            }
        }

        if (ModKey.COPY_OPTIONS_KEY.consumeClick()) {
            InteractionHand hand = findCopyModeHand(player);
            if (hand != null) {
                NetworkHandler.sendToServer(new OpenGuiPacket(hand));
            }
        }
    }

    /**
     * 找出「手持高级内存卡且当前处于复制模式」的那只手，主手优先。
     *
     * <p>只在这些条件满足时才打开选项界面：选项是给粘贴用的，配置模式的卡打开它没有意义，
     * 不持卡时也没有可写的目标卡。服务端会在 {@code OpenGuiPacket} 里再校验一次。
     */
    private static InteractionHand findCopyModeHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.ADVANCED_MEMORY_CARD.get())
                    && CardMode.of(stack) instanceof CopyMode) {
                return hand;
            }
        }
        return null;
    }
}
