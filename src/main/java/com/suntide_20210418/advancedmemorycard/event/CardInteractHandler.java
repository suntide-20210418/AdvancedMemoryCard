package com.suntide_20210418.advancedmemorycard.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.network.CardInteractPacket;
import com.suntide_20210418.advancedmemorycard.network.CardUsePacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 高级内存卡的右键接管点。
 *
 * <p>
 * 为什么不放在 {@code Item#onItemUseFirst} 里：1.7.10 的这条链并不总是走到——
 * 客户端在 {@code PlayerControllerMP#onPlayerRightClick} 里先跑物品方法、再跑
 * {@code block.onBlockActivated}（AE2 的线缆/部件逻辑就在那里），而服务端是否把 C08
 * 落到 {@code activateBlockOrUseItem} 还受 {@code hasMoved}、距离、
 * {@code PlayerInteractEvent} 取消等前置条件影响。实测中 AE 方块的点击会整个绕过物品方法，
 * 卡片于是退化成原版 AE 内存卡。
 *
 * <p>
 * {@code PlayerInteractEvent} 在 {@code Minecraft#func_147121_ag} 里最先触发，比上面两条都早，
 * 所以：
 * <ul>
 * <li>客户端：发 {@link CardInteractPacket} 并取消事件 —— 既不会跑本地 {@code block.onBlockActivated}，
 * 也不会发出 C08；</li>
 * <li>服务端：作为兜底执行一次（客户端已取消事件时不会走到这里，所以不会重复执行）。</li>
 * </ul>
 *
 * <p>
 * 只处理方块右键；右键空气（粘贴）仍走 {@code Item#onItemRightClick}。
 */
public final class CardInteractHandler {

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        boolean onBlock = event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK;
        if (!onBlock && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        if (player == null || event.world == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof AdvancedMemoryCardItem)) {
            return;
        }

        if (event.world.isRemote) {
            if (onBlock) {
                NetworkHandler.sendToServer(
                    new CardInteractPacket(
                        event.x,
                        event.y,
                        event.z,
                        event.face,
                        // PlayerInteractEvent 不带命中点，用面中心；配置模式的部件查找本来就有
                        // 按面回退的路径，不依赖精确命中。
                        0.5F,
                        0.5F,
                        0.5F,
                        player.isSneaking()));
            } else {
                // 取消方块事件后原版仍会补发一次 RIGHT_CLICK_AIR（flag 仍为 true），
                // 那时射线依然指向方块。注意 1.7.10 里 objectMouseOver 永远不会是 null：
                // 看向空气时它是 typeOfHit == MISS 的 MovingObjectPosition，
                // 所以只能用 typeOfHit 判断，不能判 null（否则真正的空气右键会被吞掉）。
                net.minecraft.util.MovingObjectPosition hit = net.minecraft.client.Minecraft
                    .getMinecraft().objectMouseOver;
                if (hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                    event.setCanceled(true);
                    return;
                }
                NetworkHandler.sendToServer(new CardUsePacket());
            }
            // 必须两边都取消：只取消方块那半，原版会因 flag 仍为 true 再补发一次空气包。
            event.setCanceled(true);
            return;
        }

        if (onBlock) {
            ((AdvancedMemoryCardItem) held.getItem()).handleBlockInteract(
                player,
                event.world,
                event.x,
                event.y,
                event.z,
                event.face,
                0.5F,
                0.5F,
                0.5F,
                player.isSneaking());
        }
        event.setCanceled(true);
    }
}
