package com.suntide_20210418.advancedmemorycard.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 临时探针：客户端手持物发生变化时打一行。
 *
 * <p>用来确认点击发生时手里到底是哪张卡——AE2 原生内存卡和本模组的卡在快捷栏里长得一样，
 * 拿错卡会表现为「完全是原版 AE 行为，且没有任何本模组日志」。
 *
 * <p>定位完成后整类删除。
 */
public final class HeldItemTracker {

    private String last = "?";

    /**
     * 鼠标层面探针：无论物品方法有没有被调用，右键按下都会在这里留一行。
     * 这一行配合 {@code [amc] click} 可以判断「鼠标事件没到交互逻辑」还是「到了但没分派给物品」。
     */
    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (org.lwjgl.input.Mouse.getEventButton() != 1 || !org.lwjgl.input.Mouse.getEventButtonState()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        String item = held == null ? "null"
            : held.getItem()
                .getClass()
                .getSimpleName();
        String target = mc.objectMouseOver == null ? "null" : mc.objectMouseOver.typeOfHit.name();
        int x = mc.objectMouseOver == null ? 0 : mc.objectMouseOver.blockX;
        int y = mc.objectMouseOver == null ? 0 : mc.objectMouseOver.blockY;
        int z = mc.objectMouseOver == null ? 0 : mc.objectMouseOver.blockZ;
        if (AdvancedMemoryCardMod.getLogger() != null) {
            AdvancedMemoryCardMod.getLogger()
                .info("[amc] mouse right held={} target={} pos={},{},{}", item, target, x, y, z);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        String now = held == null ? "none"
            : held.getItem()
                .getClass()
                .getSimpleName() + "#"
                + Item.getIdFromItem(held.getItem());
        if (now.equals(last)) {
            return;
        }
        last = now;
        if (AdvancedMemoryCardMod.getLogger() != null) {
            AdvancedMemoryCardMod.getLogger()
                .info("[amc] held -> {}", now);
        }
    }
}
