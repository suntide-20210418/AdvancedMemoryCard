package com.suntide_20210418.advancedmemorycard.client.renderer;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.*;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Side.CLIENT)
public class CopyModeRenderer {

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return;

        ItemStack mainHandItem = player.getHeldItemMainhand();
        ItemStack offHandItem = player.getHeldItemOffhand();

        CopyMode mainHand = getCopyModeFromStack(mainHandItem);
        CopyMode offHand = getCopyModeFromStack(offHandItem);

        if (mainHand != null) {
            renderSelectionBox(event, mainHand);
        } else if (offHand != null) {
            renderSelectionBox(event, offHand);
        }
    }

    private static CopyMode getCopyModeFromStack(ItemStack stack) {
        if (stack.getItem() instanceof com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem) {
            CardMode mode = CardMode.of(stack);
            return mode instanceof CopyMode ? (CopyMode) mode : null;
        }
        return null;
    }

    private static void renderSelectionBox(RenderWorldLastEvent event, CopyMode copyMode) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        BlockPos startPos = copyMode.getStartPos();
        BlockPos endPos = copyMode.getEndPos();
        BlockPos targetedPos = copyMode.getTargetedBlockPos(player);

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;
        double partialTicks = event.getPartialTicks();
        double interpX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double interpY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double interpZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-interpX, -interpY, -interpZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(2.0F);

        int color = getSelectionColor(copyMode);
        float[] rgb = RGB(color);
        float alpha = 1.0F;

        if (startPos != null) {
            renderBlock(buffer, tessellator, startPos, ModConfigs.getClientConfig().copyColorStartPos);
        }

        if (endPos != null) {
            renderBlock(buffer, tessellator, endPos, ModConfigs.getClientConfig().copyColorEndPos);
        } else {
            if (targetedPos != null) {
                renderBlock(buffer, tessellator, targetedPos, ModConfigs.getClientConfig().copyColorTargetedPos);
            }
        }

        AxisAlignedBB selectionBox = createAABB(startPos, endPos);
        AxisAlignedBB targetedBox = createAABB(startPos, targetedPos);
        if (selectionBox != null) {
            drawBox(buffer, tessellator, selectionBox, rgb[0], rgb[1], rgb[2], alpha);
        } else if (targetedBox != null) {
            int boxColor = color;
            float[] c = rgb;
            if (calculateVolume(targetedBox) > CopyMode.getMaxVolume()) {
                boxColor = ModConfigs.getClientConfig().copyColorOverLimit;
                c = RGB(boxColor);
            }
            drawBox(buffer, tessellator, targetedBox, c[0], c[1], c[2], alpha);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** 选择框状态颜色：仅客户端渲染使用，读取玩家本地客户端配置 */
    private static int getSelectionColor(CopyMode copyMode) {
        if (copyMode.isCopying()) {
            return ModConfigs.getClientConfig().copySelectionReady;
        } else if (copyMode.getEndPos() == null && copyMode.getStartPos() != null) {
            return ModConfigs.getClientConfig().copySelectionSecond;
        } else {
            return ModConfigs.getClientConfig().copySelectionFirst;
        }
    }

    private static void renderBlock(BufferBuilder buffer, Tessellator tessellator, BlockPos blockPos, int color) {
        float[] rgb = RGB(color);
        float alpha = 1.0F;
        AxisAlignedBB cornerBox = new AxisAlignedBB(
                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
        drawBox(buffer, tessellator, cornerBox, rgb[0], rgb[1], rgb[2], alpha);
    }

    private static void drawBox(BufferBuilder buffer, Tessellator tessellator, AxisAlignedBB box,
                                float r, float g, float b, float a) {
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        line(buffer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        line(buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        tessellator.draw();
    }

    private static void line(BufferBuilder buffer, double x1, double y1, double z1,
                             double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
