package com.suntide_20210418.advancedmemorycard.debug;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * 临时探针：不依赖本模组的物品，直接观察服务端是否收到了右键交互。
 *
 * <p>用途是把「点击没反应」拆成两种情况：
 * <ul>
 * <li>服务端连 {@code PlayerInteractEvent} 都没有 → C08 根本没到（客户端没发、hasMoved 门禁、
 * 或被别的模组取消）；</li>
 * <li>事件到了但物品方法没被调用 → 分派链路的问题。</li>
 * </ul>
 *
 * <p>只在服务端记录，定位完成后整类删除。
 */
public final class InteractionProbe {

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (event.world == null) {
            return;
        }
        EntityPlayer player = event.entityPlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getHeldItem();
        String item = held == null ? "null"
            : held.getItem()
                .getClass()
                .getSimpleName();
        org.apache.logging.log4j.Logger logger = AdvancedMemoryCardMod.getLogger();
        if (logger != null) {
            logger.info(
                "[amc] interact {} {} held={} pos={},{},{} canceled={}",
                event.world.isRemote ? "client" : "server",
                event.action,
                item,
                event.x,
                event.y,
                event.z,
                event.isCanceled());
        }
    }
}
