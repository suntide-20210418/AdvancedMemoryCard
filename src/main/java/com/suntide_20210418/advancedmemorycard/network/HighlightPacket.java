package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;

/**
 * 服务端 -> 客户端：请求高亮渲染一组 P2P 设备（由服务端分析后下发位置与高亮类型）。
 *
 * <p>本类（含 Handler）不引用任何客户端类，可在专用服务端安全加载。
 * 颜色不在服务端解析：专用服务端读到的是服务端本地的客户端配置默认值，
 * 而非玩家自己的客户端设置。因此只下发 {@link #TYPE_SELF}/{@link #TYPE_INPUT}/{@link #TYPE_OUTPUT}
 * 语义类型，由客户端在
 * {@link com.suntide_20210418.advancedmemorycard.client.network.ClientHighlightHandler}
 * 中按玩家本地配置解析颜色。</p>
 */
public class HighlightPacket implements IMessage {
    /** 高亮类型：自身（玩家点击的那个 P2P） */
    public static final int TYPE_SELF = 0;
    /** 高亮类型：输入端 */
    public static final int TYPE_INPUT = 1;
    /** 高亮类型：输出端 */
    public static final int TYPE_OUTPUT = 2;

    private ArrayList<P2PPosition> positions;
    private ArrayList<Integer> types;

    public HighlightPacket() {
    }

    public HighlightPacket(ArrayList<P2PPosition> positions, ArrayList<Integer> types) {
        this.positions = positions;
        this.types = types;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            P2PPosition p = positions.get(i);
            buf.writeLong(p.position.toLong());
            buf.writeByte(p.direction.ordinal());
            ByteBufUtils.writeUTF8String(buf, p.dimension);
            buf.writeByte(types.get(i));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = buf.readInt();
        positions = new ArrayList<>();
        types = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            net.minecraft.util.EnumFacing direction = net.minecraft.util.EnumFacing.values()[buf.readByte()];
            String dimension = ByteBufUtils.readUTF8String(buf);
            positions.add(new P2PPosition(pos, appeng.api.util.AEPartLocation.fromFacing(direction), dimension));
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
        public IMessage onMessage(HighlightPacket msg, MessageContext ctx) {
            // 此 Handler 仅在客户端收到包时被调用（注册 Side.CLIENT），
            // 因此只有此时才会加载客户端专用的 ClientHighlightHandler，专用服务端不会触达。
            com.suntide_20210418.advancedmemorycard.client.network.ClientHighlightHandler.handle(msg);
            return null;
        }
    }
}
