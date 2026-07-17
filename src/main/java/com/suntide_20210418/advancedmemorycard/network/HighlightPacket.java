package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

/**
 * 服务端 -> 客户端：请求高亮渲染一组 P2P 设备（由服务端分析后下发位置与颜色）。
 */
public class HighlightPacket implements IMessage {
    private ArrayList<P2PPosition> positions;
    private ArrayList<Integer> colors;

    public HighlightPacket() {
    }

    public HighlightPacket(ArrayList<P2PPosition> positions, ArrayList<Integer> colors) {
        this.positions = positions;
        this.colors = colors;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            P2PPosition p = positions.get(i);
            buf.writeLong(p.position.toLong());
            buf.writeByte(p.direction.ordinal());
            ByteBufUtils.writeUTF8String(buf, p.dimension);
            buf.writeInt(colors.get(i));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = buf.readInt();
        positions = new ArrayList<>();
        colors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            net.minecraft.util.EnumFacing direction = net.minecraft.util.EnumFacing.values()[buf.readByte()];
            String dimension = ByteBufUtils.readUTF8String(buf);
            positions.add(new P2PPosition(pos, appeng.api.util.AEPartLocation.fromFacing(direction), dimension));
            colors.add(buf.readInt());
        }
    }

    public ArrayList<P2PPosition> getPositions() {
        return positions;
    }

    public ArrayList<Integer> getColors() {
        return colors;
    }

    public static class Handler implements IMessageHandler<HighlightPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(HighlightPacket msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                P2PRenderer renderer = P2PRenderer.getInstance();
                renderer.clearAllRenders();
                for (int i = 0; i < msg.positions.size(); i++) {
                    P2PPosition p = msg.positions.get(i);
                    appeng.parts.p2p.PartP2PTunnel part = findPart(p);
                    if (part != null) {
                        renderer.triggerRender(part, msg.colors.get(i));
                    }
                }
            });
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static appeng.parts.p2p.PartP2PTunnel findPart(P2PPosition p) {
            try {
                int dimId = Integer.parseInt(p.dimension);
                net.minecraft.world.World world = DimensionManager.getWorld(dimId);
                if (world == null) {
                    return null;
                }
                net.minecraft.tileentity.TileEntity te = world.getTileEntity(p.position);
                if (te instanceof appeng.api.parts.IPartHost) {
                    appeng.api.parts.IPartHost host = (appeng.api.parts.IPartHost) te;
                    appeng.api.parts.IPart part = host.getPart(p.direction);
                    if (part instanceof appeng.parts.p2p.PartP2PTunnel) {
                        return (appeng.parts.p2p.PartP2PTunnel) part;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
    }
}
