package com.suntide_20210418.advancedmemorycard.network;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;

import com.suntide_20210418.advancedmemorycard.client.network.ClientHighlightHandler;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * 服务端 -> 客户端：请求高亮渲染一组 P2P 设备。
 *
 * <p>
 * 只下发「语义类型」，不下发颜色：颜色属于客户端配置，服务端读自己的 {@code client.cfg}
 * 在专用服务端上必然与玩家本地的设置不一致（以前那版就是这样，导致颜色不对）。
 */
public class HighlightPacket implements IMessage {

    public static final int TYPE_SELF = 0;
    public static final int TYPE_INPUT = 1;
    public static final int TYPE_OUTPUT = 2;

    private ArrayList<P2PPosition> positions;
    private ArrayList<Integer> types;

    public HighlightPacket() {}

    public HighlightPacket(ArrayList<P2PPosition> positions, ArrayList<Integer> types) {
        this.positions = positions;
        this.types = types;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            P2PPosition p = positions.get(i);
            buf.writeInt(p.position.x);
            buf.writeInt(p.position.y);
            buf.writeInt(p.position.z);
            buf.writeByte(p.direction.ordinal());
            ByteBufUtils.writeUTF8String(buf, p.dimension);
            // 1 字节语义类型。顺序必须与 fromBytes 完全一致，否则后面的项会整体错位。
            buf.writeByte(types.get(i));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = buf.readInt();
        positions = new ArrayList<>();
        types = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            net.minecraftforge.common.util.ForgeDirection direction = net.minecraftforge.common.util.ForgeDirection
                .values()[buf.readByte()];
            String dimension = ByteBufUtils.readUTF8String(buf);
            positions.add(new P2PPosition(pos, direction, dimension));
            types.add((int) buf.readByte());
        }
    }

    public ArrayList<P2PPosition> getPositions() {
        return positions;
    }

    public ArrayList<Integer> getTypes() {
        return types;
    }

    public static class Handler implements IMessageHandler<HighlightPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(HighlightPacket msg, MessageContext ctx) {
            // 只做「切回客户端主线程 + 委托」，不在这里碰渲染状态
            Minecraft.getMinecraft()
                .func_152344_a(() -> ClientHighlightHandler.handle(msg));
            return null;
        }
    }
}
