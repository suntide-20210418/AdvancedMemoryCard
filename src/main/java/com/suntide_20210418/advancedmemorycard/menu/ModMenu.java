package com.suntide_20210418.advancedmemorycard.menu;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, AdvancedMemoryCardMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CopyModeMenu>> COPY_MODE_MENU =
            MENU_TYPES.register("card_menu", () -> IMenuTypeExtension.create(CopyModeMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ConfigModeMenu>> CONFIG_MODE_MENU =
            MENU_TYPES.register("config_menu", () -> IMenuTypeExtension.create(ConfigModeMenu::new));
}
