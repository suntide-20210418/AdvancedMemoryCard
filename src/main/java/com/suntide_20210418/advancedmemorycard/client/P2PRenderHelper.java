package com.suntide_20210418.advancedmemorycard.client;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.p2p.P2PTunnelPart;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 客户端专用的 P2P 高亮渲染助手。
 * <p>
 * 原渲染逻辑位于服务端 {@code P2PManager} 中，会直接引用客户端渲染器，
 * 导致专用服务端在触发高亮时因类加载失败而崩溃。现将渲染彻底迁移到客户端：
 * 由界面在玩家点击高亮按钮时调用本类，依据已同步到客户端的 P2P 数据触发渲染。
 */
public final class P2PRenderHelper {
    private P2PRenderHelper() {
    }

    /**
     * 高亮单个 P2P 设备及其所在频段（输入/输出端）。
     * self 以 self 颜色渲染，其余设备按输入/输出端着色。
     */
    public static void renderP2P(P2PInfo self, ChannelInfo channel) {
        P2PRenderer renderer = P2PRenderer.getInstance();
        renderer.clearAllRenders();

        int selfColor = ModConfigs.getClientConfig().highlightColorSelf.get();
        int inputColor = ModConfigs.getClientConfig().highlightColorInput.get();
        int outputColor = ModConfigs.getClientConfig().highlightColorOutput.get();

        P2PTunnelPart<?> selfPart = resolvePart(self);
        if (selfPart != null) {
            renderer.triggerRender(selfPart, selfColor);
        }

        if (channel != null) {
            for (P2PInfo info : channel.p2pInfoList()) {
                if (samePart(info, self)) {
                    continue;
                }
                P2PTunnelPart<?> part = resolvePart(info);
                if (part != null) {
                    renderer.triggerRender(part, info.isOutput() ? outputColor : inputColor);
                }
            }
        }
    }

    /**
     * 高亮整个频段（输入/输出端），无 self 高亮。
     */
    public static void renderP2PTunnel(ChannelInfo channel) {
        P2PRenderer renderer = P2PRenderer.getInstance();
        renderer.clearAllRenders();

        int inputColor = ModConfigs.getClientConfig().highlightColorInput.get();
        int outputColor = ModConfigs.getClientConfig().highlightColorOutput.get();

        if (channel != null) {
            for (P2PInfo info : channel.p2pInfoList()) {
                P2PTunnelPart<?> part = resolvePart(info);
                if (part != null) {
                    renderer.triggerRender(part, info.isOutput() ? outputColor : inputColor);
                }
            }
        }
    }

    private static boolean samePart(P2PInfo a, P2PInfo b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.position().equals(b.position()) && a.direction() == b.direction();
    }

    /**
     * 从客户端当前维度世界中解析 P2P 部件。跨维度目标在客户端不可见时返回 null（跳过渲染）。
     */
    private static P2PTunnelPart<?> resolvePart(P2PInfo info) {
        if (info == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return null;
        }
        BlockPos pos = info.position();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IPartHost host) {
            IPart part = host.getPart(info.direction());
            if (part instanceof P2PTunnelPart<?> p2p) {
                return p2p;
            }
        }
        return null;
    }
}
