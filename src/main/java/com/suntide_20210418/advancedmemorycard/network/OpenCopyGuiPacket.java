package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.ModGuiHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * 客户端 -> 服务端：打开复制模式 GUI（G 键）。
 */
public class OpenCopyGuiPacket implements IMessage {

    public OpenCopyGuiPacket() {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<OpenCopyGuiPacket, IMessage> {
        @Override
        public IMessage onMessage(OpenCopyGuiPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            player.openGui(AdvancedMemoryCardMod.INSTANCE, ModGuiHandler.COPY_GUI_ID,
                    player.worldObj, 0, 0, 0);
            return null;
        }
    }
}
