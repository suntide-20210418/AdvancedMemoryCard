package com.suntide_20210418.advancedmemorycard.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ClientConfig;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.p2p.PartP2PTunnel;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 客户端侧高亮处理：把服务端下发的「语义类型」解析成本地配置里的颜色，再交给渲染器。
 *
 * <p>
 * 颜色必须在这里解析而不是在服务端：专用服务端读的是它自己的 {@code client.cfg}，
 * 与玩家本地设置无关，以前那版就是拿服务端的颜色值直接渲染。
 */
@SideOnly(Side.CLIENT)
public final class ClientHighlightHandler {

    private ClientHighlightHandler() {}

    public static void handle(HighlightPacket msg) {
        P2PRenderer renderer = P2PRenderer.getInstance();
        renderer.clearAllRenders();

        for (int i = 0; i < msg.getPositions()
            .size(); i++) {
            P2PPosition position = msg.getPositions()
                .get(i);
            PartP2PTunnel part = findPart(position);
            if (part != null) {
                renderer.triggerRender(
                    part,
                    resolveColor(
                        msg.getTypes()
                            .get(i)));
            }
        }
    }

    /**
     * 客户端本地点亮一台 P2P（用于传送落地后的目标提示），不经过服务端。
     * 直接复用 {@link #findPart}：维度必须用 id 字符串，和同步包 map 键一致。
     */
    public static void highlight(P2PInfo info) {
        if (info == null) return;
        PartP2PTunnel part = findPart(
            new P2PPosition(info.position(), info.direction(), String.valueOf(info.dimensionId())));
        if (part != null) {
            P2PRenderer.getInstance()
                .triggerRender(part, resolveColor(HighlightPacket.TYPE_SELF));
        }
    }

    private static int resolveColor(int type) {
        ClientConfig config = ModConfigs.getClientConfig();
        switch (type) {
            case HighlightPacket.TYPE_SELF:
                return config.highlightColorSelf;
            case HighlightPacket.TYPE_INPUT:
                return config.highlightColorInput;
            default:
                return config.highlightColorOutput;
        }
    }

    private static PartP2PTunnel findPart(P2PPosition position) {
        try {
            int dimId = Integer.parseInt(position.dimension);
            // 必须用客户端当前世界。DimensionManager 是服务端注册表（setWorld 只在
            // WorldServer/MinecraftServer 里调用），客户端 loadWorld 不会把 WorldClient
            // 注册进去：专用服务器客户端上 getWorld 恒为 null，单机则拿到服务端的
            // WorldServer 与其部件 —— 两者都不是渲染用的那一份，高亮会静默不显示。
            // 跨维度的部件本来就看不见，这里只处理当前维度。
            World world = Minecraft.getMinecraft().theWorld;
            if (world == null || world.provider.dimensionId != dimId) {
                return null;
            }
            TileEntity te = world.getTileEntity(position.position.x, position.position.y, position.position.z);
            if (te instanceof IPartHost) {
                IPart part = ((IPartHost) te).getPart(position.direction);
                if (part instanceof PartP2PTunnel) {
                    return (PartP2PTunnel) part;
                }
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
