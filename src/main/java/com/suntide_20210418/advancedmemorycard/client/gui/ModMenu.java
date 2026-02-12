package com.suntide_20210418.advancedmemorycard.client.gui;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.AdvancedMemoryCardMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModMenu {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, AdvancedMemoryCardMod.MOD_ID);

    public static final RegistryObject<MenuType<AdvancedMemoryCardMenu>> ADVANCED_MEMORY_CARD_MENU =
            MENU_TYPES.register("wireless_menu", () -> IForgeMenuType.create(AdvancedMemoryCardMenu::new));



}
