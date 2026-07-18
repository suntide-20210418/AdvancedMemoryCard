package com.suntide_20210418.advancedmemorycard.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import cpw.mods.fml.common.network.ByteBufUtils;

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
            buf.writeInt(pos.x);
            buf.writeInt(pos.y);
            buf.writeInt(pos.z);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        pos = buf.readBoolean() ? new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()) : null;
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
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container c = player.openContainer;
            if (c instanceof ICopyMenu) {
                ((ICopyMenu) c).handleCopyAction(msg.getAction(), msg.getPos());
            }
            return null;
        }
    }
}
