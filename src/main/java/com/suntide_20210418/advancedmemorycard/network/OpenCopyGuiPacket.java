package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.ModGuiHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务端：打开复制模式 GUI（G 键）。
 */
public class OpenCopyGuiPacket implements IMessage {
    private EnumHand hand;

    public OpenCopyGuiPacket() {
    }

    public OpenCopyGuiPacket(EnumHand hand) {
        this.hand = hand;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(hand.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        hand = EnumHand.values()[buf.readByte()];
    }

    public static class Handler implements IMessageHandler<OpenCopyGuiPacket, IMessage> {
        @Override
        public IMessage onMessage(OpenCopyGuiPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> player.openGui(
                    AdvancedMemoryCardMod.INSTANCE, ModGuiHandler.COPY_GUI_ID,
                    player.world, msg.hand.ordinal(), 0, 0));
            return null;
        }
    }
}
