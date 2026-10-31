package com.suntide_20210418.advancedmemorycard.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：打开复制模式 GUI（G 键）。
 */
public class OpenCopyGuiPacket implements IMessage {

    public OpenCopyGuiPacket() {}

    @Override
    public void toBytes(ByteBuf buf) {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<OpenCopyGuiPacket, IMessage> {

        @Override
        public IMessage onMessage(OpenCopyGuiPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            // 服务端复核：包里没有任何载荷，不能让客户端凭一个空包让服务端给别人开容器。
            // 注意 CardMode.of 对「没有 mode_data 的物品」会默认返回 CopyMode，所以必须先验物品类型。
            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof AdvancedMemoryCardItem)
                || !(CardMode.of(held) instanceof CopyMode)) {
                return null;
            }
            player.openGui(AdvancedMemoryCardMod.INSTANCE, AdvancedMemoryCardMod.COPY_GUI_ID, player.worldObj, 0, 0, 0);
            return null;
        }
    }
}
