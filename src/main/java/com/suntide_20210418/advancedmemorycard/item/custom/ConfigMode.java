package com.suntide_20210418.advancedmemorycard.item.custom;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.ConfigMode.*;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.*;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.BusCollisionHelper;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.InteractionUtil;
import appeng.util.LookDirection;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ConfigMode extends CardMode {
    private P2PManager currentP2PManager;

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "config");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();

        if (player != null && !level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(clickedPos);
            if (blockEntity instanceof IPartHost partHost) {
                // 使用 InteractionUtil 获取玩家视线
                LookDirection lookDir = InteractionUtil.getPlayerRay(player);
                Vec3 from = lookDir.getA().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
                Vec3 to = lookDir.getB().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());

                // 找到被射线击中的 P2P 部件
                P2PTunnelPart<?> hitP2P = findP2PPartByRay(partHost, from, to);

                if (hitP2P != null) {
                    currentP2PManager = new P2PManager(hitP2P, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 使用射线检测找到被击中的 P2P 部件
     */
    private P2PTunnelPart<?> findP2PPartByRay(IPartHost partHost, Vec3 from, Vec3 to) {
        P2PTunnelPart<?> hitPart = null;
        double closestDist = Double.MAX_VALUE;

        for (Direction face : Direction.values()) {
            IPart part = partHost.getPart(face);
            if (part instanceof P2PTunnelPart<?> p2pPart) {
                // 获取部件的碰撞箱
                List<AABB> boxes = new ArrayList<>();
                BusCollisionHelper helper = new BusCollisionHelper(boxes, face, false);
                p2pPart.getBoxes(helper);

                // 检查射线与碰撞箱的交点
                for (AABB box : boxes) {
                    Optional<Vec3> hitResult = box.clip(from, to);
                    if (hitResult.isPresent()) {
                        double dist = from.distanceToSqr(hitResult.get());
                        if (dist < closestDist) {
                            closestDist = dist;
                            hitPart = p2pPart;
                        }
                    }
                }
            }
        }

        return hitPart;
    }

    @Override
    public Component getName() {
        return show();
    }

    @Override
    protected Component getDescription() {
        return configInfo();
    }

    public P2PManager getCurrentP2PManager() {
        return currentP2PManager;
    }
}
