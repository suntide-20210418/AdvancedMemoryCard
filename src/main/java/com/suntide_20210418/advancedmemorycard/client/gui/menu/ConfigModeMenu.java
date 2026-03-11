package com.suntide_20210418.advancedmemorycard.client.gui.menu;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;


/**
 * 配置模式菜单
 * 负责P2P通道的频段配置、绑定管理
 */
public class ConfigModeMenu extends AEBaseMenu {


    public ConfigModeMenu(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }
}