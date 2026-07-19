package com.suntide_20210418.advancedmemorycard.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

import appeng.parts.BusCollisionHelper;
import appeng.parts.p2p.PartP2PTunnel;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class P2PRenderer {

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
    public void onRenderWorldLast(RenderWorldLastEvent event) {
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
        Entity view = mc.renderViewEntity;
        if (view == null) return;

        double partialTicks = event.partialTicks;
        double interpX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double interpY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double interpZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        long currentTime = System.currentTimeMillis();

        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) -interpX, (float) -interpY, (float) -interpZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        tessellator.startDrawing(GL11.GL_LINES);

        // 复制一份快照，避免遍历时 WeakHashMap 被其他线程修改导致 ConcurrentModificationException
        List<Map.Entry<PartP2PTunnel, RenderInfo>> entries = new ArrayList<>(renderTimes.entrySet());
        for (Map.Entry<PartP2PTunnel, RenderInfo> entry : entries) {
            PartP2PTunnel p2pPart = entry.getKey();
            RenderInfo info = entry.getValue();

            TileEntity te = p2pPart.getTile();
            if (p2pPart == null || te == null || te.isInvalid()) {
                renderTimes.remove(p2pPart);
                continue;
            }

            if (currentTime - info.startTime >= info.duration) {
                renderTimes.remove(p2pPart);
                continue;
            }

            performRender(tessellator, p2pPart, info.color);
        }

        tessellator.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    /**
     * 实际的渲染实现
     */
    private void performRender(Tessellator tessellator, PartP2PTunnel p2pPart, int color) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        BusCollisionHelper collisionHelper = new BusCollisionHelper(boxes, p2pPart.getSide(), null, false);
        p2pPart.getBoxes(collisionHelper);

        if (boxes.isEmpty()) return;

        TileEntity te = p2pPart.getTile();
        BlockPos p2pPos = new BlockPos(te.xCoord, te.yCoord, te.zCoord);

        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        for (AxisAlignedBB box : boxes) {
            AxisAlignedBB localBox = box.offset(p2pPos.getX(), p2pPos.getY(), p2pPos.getZ());
            drawBox(tessellator, localBox, red, green, blue);
        }
    }

    /**
     * 绘制一个 AABB 的 12 条棱线
     */
    private void drawBox(Tessellator tessellator, AxisAlignedBB box, float r, float g, float b) {
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        tessellator.setColorOpaque_F(r, g, b);
        // 底边
        line(tessellator, minX, minY, minZ, maxX, minY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
        line(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, minY, minZ);
        // 顶边
        line(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(tessellator, minX, maxY, maxZ, minX, maxY, minZ);
        // 竖边
        line(tessellator, minX, minY, minZ, minX, maxY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private void line(Tessellator tessellator, double x1, double y1, double z1, double x2, double y2, double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
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
