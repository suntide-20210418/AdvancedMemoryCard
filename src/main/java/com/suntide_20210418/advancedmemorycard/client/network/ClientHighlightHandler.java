package com.suntide_20210418.advancedmemorycard.client.network;

import appeng.parts.p2p.PartP2PTunnel;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
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

    /**
     * 客户端本地点亮一台 P2P（用于传送落地后的目标提示），不经过服务端。
     * 直接复用 {@link #findPart}：维度必须用 id 字符串，和同步包 map 键一致。
     */
    /**
     * 目标部件是否已经在客户端世界里可查（区块已加载、部件已下发）。
     * 传送刚落地时坐标可能先到、区块后到；只判坐标会在 findPart 拿到 null 时
     * 把高亮静默丢掉，所以延迟任务要把这个条件一起等。
     */
    public static boolean canHighlight(P2PInfo info) {
        if (info == null) return false;
        return findPart(new P2PPosition(info.position(), info.direction(), String.valueOf(info.dimensionId()))) != null;
    }

    public static void highlight(P2PInfo info) {
        if (info == null) return;
        PartP2PTunnel part = findPart(
            new P2PPosition(info.position(), info.direction(), String.valueOf(info.dimensionId())));
        if (part != null) {
            P2PRenderer.getInstance().triggerRender(part, resolveColor(HighlightPacket.TYPE_SELF));
        }
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
            // 必须用客户端当前世界。DimensionManager 是服务端注册表（setWorld 只在
            // WorldServer/MinecraftServer 里调用），客户端 loadWorld 不会把 ClientWorld
            // 注册进去：专用服务器客户端上 getWorld 恒为 null，单机则拿到服务端的
            // ServerWorld 与其部件 —— 两者都不是渲染用的那一份，高亮会静默不显示。
            // 跨维度的部件本来就看不见，这里只处理当前维度。
            World world = Minecraft.getMinecraft().world;
            if (world == null || world.provider.getDimension() != dimId) {
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
