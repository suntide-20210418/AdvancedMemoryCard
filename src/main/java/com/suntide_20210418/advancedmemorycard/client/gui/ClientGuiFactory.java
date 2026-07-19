package com.suntide_20210418.advancedmemorycard.client.gui;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端专用的 GUI 创建工厂。引用客户端 Screen 类的代码集中在此，
 * 使双端加载的 {@link ModGuiHandler} 不再直接引用任何客户端类。
 * 本类只会由 {@code ModGuiHandler.getClientGuiElement} 调用，而该方法仅由客户端触发，
 * 因此专用服务端不会加载本类。
 */
@SideOnly(Side.CLIENT)
public final class ClientGuiFactory {

    private ClientGuiFactory() {
    }

    public static Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ModGuiHandler.CONFIG_GUI_ID) {
            ConfigModeMenu menu = new ConfigModeMenu(player.inventory, net.minecraft.util.EnumHand.values()[x]);
            return new ConfigModeScreen(menu, player);
        } else if (id == ModGuiHandler.COPY_GUI_ID) {
            CopyModeMenu menu = new CopyModeMenu(player.inventory, net.minecraft.util.EnumHand.values()[x]);
            return new CopyModeScreen(menu, player);
        }
        return null;
    }
}
