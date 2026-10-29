package com.suntide_20210418.advancedmemorycard.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：右键空气使用高级内存卡（粘贴等）。
 *
 * <p>
 * 为什么需要它：{@code CardInteractHandler} 在客户端取消了右键事件，原版就不会发
 * C08(255)；如果只取消方块那一半，原版会因为 {@code flag} 仍为 true 而补发一次空气包，
 * 变成「点方块却触发粘贴」。所以空气侧也走自己的包，两边行为一致且只有一次执行。
 */
public class CardUsePacket implements IMessage {

    public CardUsePacket() {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<CardUsePacket, IMessage> {

        @Override
        public IMessage onMessage(CardUsePacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof AdvancedMemoryCardItem)) {
                return null;
            }
            CardMode.of(held)
                .onItemUse(player.worldObj, player);
            return null;
        }
    }
}
