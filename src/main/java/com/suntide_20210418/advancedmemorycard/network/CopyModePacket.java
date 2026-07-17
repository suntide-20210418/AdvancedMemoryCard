package com.suntide_20210418.advancedmemorycard.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 -> 服务端：复制模式 GUI 中的操作（清除、修改起止点、刷新）。
 */
public class CopyModePacket implements IMessage {

    public interface ICopyMenu {
        void handleCopyAction(String action, BlockPos pos);
    }

    private String action;
    private BlockPos pos;

    public CopyModePacket() {
    }

    public CopyModePacket(String action) {
        this(action, null);
    }

    public CopyModePacket(String action, BlockPos pos) {
        this.action = action;
        this.pos = pos;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        buf.writeBoolean(pos != null);
        if (pos != null) {
            buf.writeLong(pos.toLong());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        pos = buf.readBoolean() ? BlockPos.fromLong(buf.readLong()) : null;
    }

    public String getAction() {
        return action;
    }

    public BlockPos getPos() {
        return pos;
    }

    public static class Handler implements IMessageHandler<CopyModePacket, IMessage> {
        @Override
        public IMessage onMessage(CopyModePacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                Container c = player.openContainer;
                if (c instanceof ICopyMenu) {
                    ((ICopyMenu) c).handleCopyAction(msg.getAction(), msg.getPos());
                }
            });
            return null;
        }
    }
}
