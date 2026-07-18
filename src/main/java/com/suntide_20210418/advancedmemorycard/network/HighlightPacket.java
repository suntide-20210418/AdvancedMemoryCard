package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.ByteBufUtils;

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
            buf.writeInt(p.position.x);
            buf.writeInt(p.position.y);
            buf.writeInt(p.position.z);
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
            BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
            ForgeDirection direction = ForgeDirection.values()[buf.readByte()];
            String dimension = ByteBufUtils.readUTF8String(buf);
            positions.add(new P2PPosition(pos, direction, dimension));
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
            Minecraft.getMinecraft().func_152344_a(() -> {
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
                World world = DimensionManager.getWorld(dimId);
                if (world == null) {
                    return null;
                }
                net.minecraft.tileentity.TileEntity te = world
                        .getTileEntity(p.position.x, p.position.y, p.position.z);
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
