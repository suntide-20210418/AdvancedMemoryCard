package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务端：切换记忆卡模式（V 键）。
 */
public class ModeSwitchPacket implements IMessage {
    private EnumHand hand;

    public ModeSwitchPacket() {
    }

    public ModeSwitchPacket(EnumHand hand) {
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

    public static class Handler implements IMessageHandler<ModeSwitchPacket, IMessage> {
        @Override
        public IMessage onMessage(ModeSwitchPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                ItemStack stack = msg.hand == EnumHand.MAIN_HAND
                        ? player.getHeldItemMainhand()
                        : player.getHeldItemOffhand();
                if (stack.getItem() instanceof AdvancedMemoryCardItem) {
                    ((AdvancedMemoryCardItem) stack.getItem()).cycleMode(player, stack);
                }
            });
            return null;
        }
    }
}
