package com.suntide_20210418.advancedmemorycard.client.gui;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModGuiHandler implements IGuiHandler {
    public static final int CONFIG_GUI_ID = 0;
    public static final int COPY_GUI_ID = 1;

    private static final Map<UUID, P2PManager> pendingManagers = new HashMap<>();

    public static void putPendingManager(EntityPlayer player, P2PManager manager) {
        pendingManagers.put(player.getUniqueID(), manager);
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        ItemStack held = player.getHeldItem();
        if (ID == CONFIG_GUI_ID) {
            // 服务端持有真实 P2PManager，从 pending 缓存取出（1.7.10 无 EnumHand，直接用主手物品）
            P2PManager manager = pendingManagers.remove(player.getUniqueID());
            if (manager == null) {
                return new ConfigModeMenu(player.inventory);
            }
            return new ConfigModeMenu(player.inventory, manager);
        } else if (ID == COPY_GUI_ID) {
            return new CopyModeMenu(player.inventory);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == CONFIG_GUI_ID) {
            ConfigModeMenu menu = new ConfigModeMenu(player.inventory);
            return new ConfigModeScreen(menu, player);
        } else if (ID == COPY_GUI_ID) {
            CopyModeMenu menu = new CopyModeMenu(player.inventory);
            return new CopyModeScreen(menu, player);
        }
        return null;
    }
}
