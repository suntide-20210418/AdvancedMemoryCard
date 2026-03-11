package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ConfigModeScreen extends AEBaseScreen<ConfigModeMenu> {
    public ConfigModeScreen(ConfigModeMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
