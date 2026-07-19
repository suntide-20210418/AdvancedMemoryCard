package com.suntide_20210418.advancedmemorycard;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;

import cpw.mods.fml.common.network.IGuiHandler;

/**
 * 通用（服务端/客户端共用）GUI 处理器。
 *
 * <p>
 * 本类不引用任何客户端类：服务端容器（ConfigModeMenu / CopyModeMenu）均为服务端安全，
 * 而客户端屏幕的构造通过 {@link AdvancedMemoryCardMod#proxy}，
 * 因此专用服务端加载本类时不会触碰任何 net.minecraft.client 或 client 包下的类。
 * </p>
 */
public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == AdvancedMemoryCardMod.CONFIG_GUI_ID) {
            // 服务端持有真实 P2PManager，从 pending 缓存取出（1.7.10 无 EnumHand，直接用主手物品）
            P2PManager manager = CommonProxy.takePendingManager(player);
            if (manager == null) {
                return new ConfigModeMenu(player.inventory);
            }
            return new ConfigModeMenu(player.inventory, manager);
        } else if (ID == AdvancedMemoryCardMod.COPY_GUI_ID) {
            return new CopyModeMenu(player.inventory);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        // 仅在客户端执行：委托给 ClientProxy 构造真正的屏幕，避免本通用类引用客户端类。
        return AdvancedMemoryCardMod.proxy.openClientGui(ID, player, world, x, y, z);
    }
}
