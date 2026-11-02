package com.suntide_20210418.advancedmemorycard.client.renderer;

import appeng.parts.BusCollisionHelper;
import appeng.parts.p2p.PartP2PTunnel;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Side.CLIENT)
public class P2PRenderer {
    /** 描边线宽：2.0 太细，3.5 在方块边缘上明显可见。 */
    private static final float LINE_WIDTH = 3.5F;
    /** 基色向白色混合的比例：纯红/纯绿也能显得更亮。 */
    private static final float BRIGHTEN = 0.35F;
    /** 亮度脉冲周期（毫秒）：0.5 秒一档，50% / 100% 交替。 */
    private static final long PULSE_INTERVAL_MS = 500L;

    // 存储每个 P2P 部件的渲染开始时间和持续时间
    private final Map<PartP2PTunnel, RenderInfo> renderTimes = new WeakHashMap<>();

    private static P2PRenderer INSTANCE;

    private P2PRenderer() {
        INSTANCE = this;
    }

    /**
     * 渲染信息内部类
     */
    private static class RenderInfo {
        final long startTime;
        final long duration;
        final int color;

        RenderInfo(long startTime, long duration, int color) {
            this.startTime = startTime;
            this.duration = duration;
            this.color = color;
        }

        boolean isActive() {
            return System.currentTimeMillis() - startTime < duration;
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (INSTANCE != null) {
            INSTANCE.renderActiveP2Ps(event);
        }
    }

    /**
     * 触发 P2P 渲染，持续时间从客户端配置读取
     */
    public void triggerRender(PartP2PTunnel p2pPart, int color) {
        long durationMs = ModConfigs.getClientConfig().p2pHighlightDurationSec * 1000L;
        triggerRender(p2pPart, color, durationMs);
    }

    /**
     * 触发 P2P 渲染，指定持续时间
     */
    public void triggerRender(PartP2PTunnel p2pPart, int color, long durationMs) {
        if (p2pPart == null || isRendering(p2pPart)) return;
        renderTimes.put(p2pPart, new RenderInfo(System.currentTimeMillis(), durationMs, color));
    }

    public void clearRender(PartP2PTunnel p2pPart) {
        if (p2pPart == null || !renderTimes.containsKey(p2pPart)) return;
        renderTimes.remove(p2pPart);
    }

    public void clearAllRenders() {
        if (renderTimes.isEmpty()) return;
        renderTimes.clear();
    }

    /**
     * 主渲染方法 - 在 RenderWorldLastEvent 中调用
     */
    public void renderActiveP2Ps(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        double partialTicks = event.getPartialTicks();
        double interpX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double interpY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double interpZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        long currentTime = System.currentTimeMillis();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-interpX, -interpY, -interpZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(LINE_WIDTH);

        // 复制一份快照，避免遍历时 WeakHashMap 被其他线程修改导致 ConcurrentModificationException
        List<Map.Entry<PartP2PTunnel, RenderInfo>> entries = new ArrayList<>(renderTimes.entrySet());
        for (Map.Entry<PartP2PTunnel, RenderInfo> entry : entries) {
            PartP2PTunnel p2pPart = entry.getKey();
            RenderInfo info = entry.getValue();

            if (p2pPart == null || p2pPart.getTile() == null || p2pPart.getTile().isInvalid()) {
                renderTimes.remove(p2pPart);
                continue;
            }

            if (currentTime - info.startTime >= info.duration) {
                renderTimes.remove(p2pPart);
                continue;
            }

            // 全局时钟做脉冲：所有描边同步呼吸，重发高亮也不会重置相位。
            float brightness = ((currentTime / PULSE_INTERVAL_MS) & 1L) == 0L ? 1.0F : 0.5F;
            performRender(buffer, tessellator, p2pPart, info.color, brightness);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * 实际的渲染实现
     */
    private void performRender(BufferBuilder buffer, Tessellator tessellator, PartP2PTunnel p2pPart, int color,
                               float brightness) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        BusCollisionHelper collisionHelper = new BusCollisionHelper(boxes, p2pPart.getSide(), null, false);
        p2pPart.getBoxes(collisionHelper);

        if (boxes.isEmpty()) return;

        BlockPos p2pPos = p2pPart.getTile().getPos();

        // 先向白色混合提亮，再按 50% / 100% 脉冲缩放。
        float red = brighten(((color >> 16) & 0xFF) / 255.0F) * brightness;
        float green = brighten(((color >> 8) & 0xFF) / 255.0F) * brightness;
        float blue = brighten((color & 0xFF) / 255.0F) * brightness;
        float alpha = 1.0F;

        for (AxisAlignedBB box : boxes) {
            AxisAlignedBB localBox = box.offset(p2pPos);
            drawBox(buffer, tessellator, localBox, red, green, blue, alpha);
        }
    }

    private static float brighten(float channel) {
        return channel + (1.0F - channel) * BRIGHTEN;
    }

    /**
     * 绘制一个 AABB 的 12 条棱线
     */
    private void drawBox(BufferBuilder buffer, Tessellator tessellator, AxisAlignedBB box,
                         float r, float g, float b, float a) {
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        // 底边
        line(buffer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        // 顶边
        line(buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        // 竖边
        line(buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        tessellator.draw();
    }

    private void line(BufferBuilder buffer, double x1, double y1, double z1,
                      double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }

    public boolean isRendering(PartP2PTunnel p2pPart) {
        RenderInfo info = renderTimes.get(p2pPart);
        return info != null && info.isActive();
    }

    public static P2PRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new P2PRenderer();
        }
        return INSTANCE;
    }
}
