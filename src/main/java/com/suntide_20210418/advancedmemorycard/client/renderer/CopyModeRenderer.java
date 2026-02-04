package com.suntide_20210418.advancedmemorycard.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.*;

@Mod.EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CopyModeRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        // 检查主手或副手是否持有高级内存卡
        CopyMode mainHand = getCopyModeFromStack(mainHandItem);
        CopyMode offHand = getCopyModeFromStack(offHandItem);

        if (mainHand != null) {
            renderSelectionBox(event, mainHand);
        } else if (offHand != null) {
            renderSelectionBox(event, offHand);
        }
    }

    private static CopyMode getCopyModeFromStack(ItemStack stack) {
        if (stack.getItem() instanceof AdvancedMemoryCardItem) {
            return CardMode.of(stack) instanceof CopyMode ? (CopyMode) CardMode.of(stack) : null;
        }
        return null;
    }

    private static void renderSelectionBox(RenderLevelStageEvent event, CopyMode copyMode) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        BlockPos startPos = copyMode.getStartPos();
        BlockPos endPos = copyMode.getEndPos();
        BlockPos targetedPos = copyMode.getTargetedBlockPos(player);

        PoseStack poseStack = event.getPoseStack();
        AABB selectionBox = createAABB(startPos, endPos);
        AABB targetedBox = createAABB(startPos, targetedPos);

        poseStack.pushPose();

        // 重要：将渲染位置转换到摄像机位置
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());


        // 使用正确的 RenderType
        VertexConsumer vertexConsumer = mc.renderBuffers().bufferSource()
                .getBuffer(RenderType.LINES);

        int color = copyMode.getSelectionColor();
        float alpha = 1.0F;

        if (startPos != null) {
            renderBlock(poseStack, startPos, 0xFF0000);
        }

        if (endPos != null) {
            renderBlock(poseStack, endPos, 0xFFFF00);
        } else {
            if (targetedPos != null) {
                renderBlock(poseStack, targetedPos, 0x00FF00);
            }
        }

        if (selectionBox != null) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    vertexConsumer,
                    selectionBox,
                    RGB(color)[0], RGB(color)[1], RGB(color)[2], alpha
            );
        } else if (targetedBox != null) {
            if (calculateVolume(targetedBox) > CopyMode.getMaxVolume()) {
                color = 0xFF0000;
            }
            LevelRenderer.renderLineBox(
                    poseStack,
                    vertexConsumer,
                    targetedBox,
                    RGB(color)[0], RGB(color)[1], RGB(color)[2], alpha
            );
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.LINES);

        poseStack.popPose();
    }

    private static void renderBlock(PoseStack poseStack, BlockPos firstPos, int color) {
        Minecraft mc = Minecraft.getInstance();
        VertexConsumer vertexConsumer = mc.renderBuffers().bufferSource()
                .getBuffer(RenderType.LINES);

        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = 1.0F;

        // 使用与角落方块相同的渲染方法
        renderCorner(poseStack, vertexConsumer, firstPos, red, green, blue, alpha);
    }

    private static void renderCorner(PoseStack poseStack, VertexConsumer vertexConsumer,
                                     BlockPos pos, float red, float green, float blue, float alpha) {
        AABB cornerBox = new AABB(pos);
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, cornerBox, red, green, blue, alpha);
    }
}
