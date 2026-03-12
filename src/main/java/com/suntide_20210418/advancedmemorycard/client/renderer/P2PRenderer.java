package com.suntide_20210418.advancedmemorycard.client.renderer;

import appeng.parts.BusCollisionHelper;
import appeng.parts.p2p.P2PTunnelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(
        modid = AdvancedMemoryCardMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class P2PRenderer {
    // 存储每个P2P部件的渲染开始时间和持续时间
    private final Map<P2PTunnelPart<?>, RenderInfo> renderTimes = new WeakHashMap<>();

    // 默认渲染持续时间（5秒 = 5000毫秒）
    private static final long DEFAULT_DURATION_MS = 5000;

    private static P2PRenderer INSTANCE;

    private P2PRenderer() {
        INSTANCE = this;
    }

    /**
     * @param startTime 开始渲染的时间戳（毫秒）
     * @param duration  持续时间（毫秒）
     * @param color     颜色
     */ // 渲染信息内部类
        private record RenderInfo(long startTime, long duration, int color) {

            // 检查是否仍在渲染时间内
            boolean isActive() {
                return System.currentTimeMillis() - startTime < duration;
            }
        }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS && INSTANCE != null) {
            INSTANCE.renderActiveP2Ps(event);
        }
    }


    /**
     * 触发P2P渲染，持续5秒
     * @param p2pPart P2P隧道部件
     * @param color 颜色
     */
    public void triggerRender(P2PTunnelPart<?> p2pPart, int color) {
        triggerRender(p2pPart, color, DEFAULT_DURATION_MS);
    }

    /**
     * 触发P2P渲染，指定持续时间
     * @param p2pPart P2P隧道部件
     * @param color 颜色
     * @param durationMs 持续时间（毫秒）
     */
    public void triggerRender(P2PTunnelPart<?> p2pPart, int color, long durationMs) {
        if (p2pPart == null || isRendering(p2pPart)) return;
        renderTimes.put(p2pPart, new RenderInfo(System.currentTimeMillis(), durationMs, color));
    }

    /**
     * 清除指定P2P的渲染状态
     * @param p2pPart P2P隧道部件
     */
    public void clearRender(P2PTunnelPart<?> p2pPart) {
        if (p2pPart == null || renderTimes.containsKey(p2pPart)) return;
        renderTimes.remove(p2pPart);
    }

    /**
     * 清除所有P2P的渲染状态
     */
    public void clearAllRenders() {
        if (renderTimes.isEmpty()) return;
        renderTimes.clear();
    }

    /**
     * 主渲染方法 - 应该在每一帧被调用
     * 修改：接受事件中的 PoseStack、投影矩阵和相机
     */
    public void renderActiveP2Ps(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        if (mc.player == null || mc.level == null) return;

        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // 保存原始的 PoseStack 状态
        poseStack.pushPose();

        // 重要：将渲染位置转换到摄像机位置
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        // 使用正确的 RenderType
        VertexConsumer vertexConsumer =
                mc.renderBuffers().bufferSource().getBuffer(RenderType.LINES);

        // 遍历所有需要渲染的P2P
        renderTimes.entrySet().removeIf(entry -> {
            P2PTunnelPart<?> p2pPart = entry.getKey();
            RenderInfo info = entry.getValue();

            // 检查P2P部件是否仍然有效
            if (p2pPart == null || p2pPart.getBlockEntity() == null || p2pPart.getBlockEntity().isRemoved()) {
                return true; // 移除无效的部件
            }

            // 检查是否超过持续时间
            if (currentTime - info.startTime >= info.duration) {
                return true; // 超过时间，移除
            }

            // 执行渲染 - 传入已经应用了相机变换的 poseStack
            performRender(poseStack, vertexConsumer, p2pPart, info.color);

            return false; // 保留仍在渲染时间内的部件
        });

        // 恢复原始的 PoseStack 状态
        poseStack.popPose();

        mc.renderBuffers().bufferSource().endBatch(RenderType.LINES);
    }


    /**
     * 实际的渲染实现
     * 修改：接受已经应用了相机变换的 PoseStack 和相机
     */
    private void performRender(PoseStack poseStack, VertexConsumer vertexConsumer, P2PTunnelPart<?> p2pPart, int color) {
        List<AABB> boxes = new ArrayList<>();
        Direction side = p2pPart.getSide();
        BlockPos p2pPos = p2pPart.getBlockEntity().getBlockPos();

        BusCollisionHelper collisionHelper = new BusCollisionHelper(
                boxes,
                side,
                false
        );

        p2pPart.getBoxes(collisionHelper);

        if (boxes.isEmpty()) return;

        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        // 渲染每个盒子
        for (AABB box : boxes) {
            AABB localBox = box.move(p2pPos);
            // 在世界空间中渲染盒子（因为我们已经应用了相机变换）
            LevelRenderer.renderLineBox(
                    poseStack,
                    vertexConsumer,
                    localBox,
                    red, green, blue, 1.0F
            );
        }
    }

    /**
     * 检查指定P2P是否正在渲染
     */
    public boolean isRendering(P2PTunnelPart<?> p2pPart) {
        RenderInfo info = renderTimes.get(p2pPart);
        return info != null && info.isActive();
    }

    // 获取单例的静态方法
    public static P2PRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new P2PRenderer();
        }
        return INSTANCE;
    }
}
