package com.suntide_20210418.advancedmemorycard.client.network;

import appeng.parts.p2p.PartP2PTunnel;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;

/**
 * 客户端专用的 {@link HighlightPacket} 处理器。
 * 在这里按玩家本地客户端配置解析高亮颜色，并调度到客户端主线程执行渲染。
 */
@SideOnly(Side.CLIENT)
public final class ClientHighlightHandler {

    private ClientHighlightHandler() {
    }

    public static void handle(HighlightPacket msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            P2PRenderer renderer = P2PRenderer.getInstance();
            renderer.clearAllRenders();
            ArrayList<P2PPosition> positions = msg.getPositions();
            ArrayList<Integer> types = msg.getTypes();
            for (int i = 0; i < positions.size(); i++) {
                PartP2PTunnel part = findPart(positions.get(i));
                if (part != null) {
                    renderer.triggerRender(part, resolveColor(types.get(i)));
                }
            }
        });
    }

    /** 按客户端本地配置解析高亮类型对应的颜色 */
    private static int resolveColor(int type) {
        switch (type) {
            case HighlightPacket.TYPE_SELF:
                return ModConfigs.getClientConfig().highlightColorSelf;
            case HighlightPacket.TYPE_INPUT:
                return ModConfigs.getClientConfig().highlightColorInput;
            case HighlightPacket.TYPE_OUTPUT:
            default:
                return ModConfigs.getClientConfig().highlightColorOutput;
        }
    }

    private static PartP2PTunnel findPart(P2PPosition p) {
        try {
            int dimId = Integer.parseInt(p.dimension);
            World world = DimensionManager.getWorld(dimId);
            if (world == null) {
                return null;
            }
            TileEntity te = world.getTileEntity(p.position);
            if (te instanceof appeng.api.parts.IPartHost) {
                appeng.api.parts.IPartHost host = (appeng.api.parts.IPartHost) te;
                appeng.api.parts.IPart part = host.getPart(p.direction);
                if (part instanceof PartP2PTunnel) {
                    return (PartP2PTunnel) part;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}
