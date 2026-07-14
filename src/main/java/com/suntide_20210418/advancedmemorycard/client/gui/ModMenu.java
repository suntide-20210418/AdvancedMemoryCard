package com.suntide_20210418.advancedmemorycard.client.gui;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, AdvancedMemoryCardMod.MOD_ID);

    public static final RegistryObject<MenuType<CopyModeMenu>> COPY_MODE_MENU =
            MENU_TYPES.register("card_menu", () -> IForgeMenuType.create(CopyModeMenu::new));

    public static final RegistryObject<MenuType<ConfigModeMenu>> CONFIG_MODE_MENU =
            MENU_TYPES.register("config_menu", () -> IForgeMenuType.create(ConfigModeMenu::new));

}
