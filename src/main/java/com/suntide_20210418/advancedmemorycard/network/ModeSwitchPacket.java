package com.suntide_20210418.advancedmemorycard.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：切换记忆卡模式（V 键）。
 */
public class ModeSwitchPacket implements IMessage {

    public ModeSwitchPacket() {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<ModeSwitchPacket, IMessage> {

        @Override
        public IMessage onMessage(ModeSwitchPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ItemStack stack = player.getHeldItem();
            if (stack != null && stack.getItem() instanceof AdvancedMemoryCardItem) {
                ((AdvancedMemoryCardItem) stack.getItem()).cycleMode(player, stack);
            }
            return null;
        }
    }
}
