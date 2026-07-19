package com.suntide_20210418.advancedmemorycard.client.renderer;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CopyModeRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;

        ItemStack heldItem = player.getHeldItem();
        CopyMode copyMode = getCopyModeFromStack(heldItem);

        if (copyMode != null) {
            renderSelectionBox(event, copyMode);
        }
    }

    private static CopyMode getCopyModeFromStack(ItemStack stack) {
        if (stack != null
            && stack.getItem() instanceof com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem) {
            CardMode mode = CardMode.of(stack);
            return mode instanceof CopyMode ? (CopyMode) mode : null;
        }
        return null;
    }

    private static void renderSelectionBox(RenderWorldLastEvent event, CopyMode copyMode) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;

        BlockPos startPos = copyMode.getStartPos();
        BlockPos endPos = copyMode.getEndPos();
        BlockPos targetedPos = copyMode.getTargetedBlockPos(player);

        Entity view = mc.renderViewEntity;
        if (view == null) return;
        double partialTicks = event.partialTicks;
        double interpX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double interpY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double interpZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) -interpX, (float) -interpY, (float) -interpZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);

        int color = copyMode.getSelectionColor();
        float[] rgb = RGB(color);
        float alpha = 1.0F;

        tessellator.startDrawing(GL11.GL_LINES);

        if (startPos != null) {
            renderBlock(tessellator, startPos, ModConfigs.getClientConfig().copyColorStartPos);
        }

        if (endPos != null) {
            renderBlock(tessellator, endPos, ModConfigs.getClientConfig().copyColorEndPos);
        } else {
            if (targetedPos != null) {
                renderBlock(tessellator, targetedPos, ModConfigs.getClientConfig().copyColorTargetedPos);
            }
        }

        AxisAlignedBB selectionBox = createAABB(startPos, endPos);
        AxisAlignedBB targetedBox = createAABB(startPos, targetedPos);
        if (selectionBox != null) {
            drawBox(tessellator, selectionBox, rgb[0], rgb[1], rgb[2]);
        } else if (targetedBox != null) {
            int boxColor = color;
            float[] c = rgb;
            if (calculateVolume(targetedBox) > CopyMode.getMaxVolume()) {
                boxColor = ModConfigs.getClientConfig().copyColorOverLimit;
                c = RGB(boxColor);
            }
            drawBox(tessellator, targetedBox, c[0], c[1], c[2]);
        }

        tessellator.draw();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private static void renderBlock(Tessellator tessellator, BlockPos blockPos, int color) {
        float[] rgb = RGB(color);
        AxisAlignedBB cornerBox = AxisAlignedBB.getBoundingBox(
            blockPos.getX(),
            blockPos.getY(),
            blockPos.getZ(),
            blockPos.getX() + 1,
            blockPos.getY() + 1,
            blockPos.getZ() + 1);
        drawBox(tessellator, cornerBox, rgb[0], rgb[1], rgb[2]);
    }

    private static void drawBox(Tessellator tessellator, AxisAlignedBB box, float r, float g, float b) {
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        tessellator.setColorOpaque_F(r, g, b);
        line(tessellator, minX, minY, minZ, maxX, minY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
        line(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, minY, minZ);
        line(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(tessellator, minX, maxY, maxZ, minX, maxY, minZ);
        line(tessellator, minX, minY, minZ, minX, maxY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void line(Tessellator tessellator, double x1, double y1, double z1, double x2, double y2,
        double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }
}
