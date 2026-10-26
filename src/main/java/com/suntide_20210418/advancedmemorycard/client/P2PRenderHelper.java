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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 客户端专用的 P2P 高亮渲染助手。
 *
 * <p>渲染逻辑原先挂在服务端 {@code P2PManager} 上（用 {@code DistExecutor} 包一层客户端调用），
 * 那意味着服务端类要引用客户端渲染器，专用服加载这个类时随时可能踩到类加载问题。
 * 现在渲染彻底下沉到客户端：由界面在玩家点击高亮按钮时调用本类，
 * 依据<b>已经同步到客户端的</b> {@link P2PInfo} 反查部件实例并触发渲染；
 * 服务端只负责发聊天坐标与传送链接。
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

    /** 高亮整个频段（输入/输出端），无 self 高亮。 */
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
