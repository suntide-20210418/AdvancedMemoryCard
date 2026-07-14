package com.suntide_20210418.advancedmemorycard;

import appeng.client.gui.style.StyleManager;
import com.mojang.logging.LogUtils;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AdvancedMemoryCardMod.MOD_ID)
public class AdvancedMemoryCardMod {
    public static final String MOD_ID = "advanced_memory_card";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedMemoryCardMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        LOGGER.info("AdvancedMemoryCardMod is loading");

        // 注册配置文件（需在最早时机注册）
        ModConfigs.register();

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 注册事件监听器
        MinecraftForge.EVENT_BUS.register(this);
        ModMenu.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing card modes...");
        CardMode.initializeModes();
        LOGGER.info("Card modes initialized successfully");

        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.info("Network packets registered!");
        });
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        P2PRenderer.getInstance();
        event.enqueueWork(() -> {
            // 注册复制模式屏幕
            MenuScreens.register(
                    ModMenu.COPY_MODE_MENU.get(),
                    (CopyModeMenu menu,
                     Inventory inv,
                     Component title) ->
                            new CopyModeScreen(
                                    menu,
                                    inv,
                                    title,
                                    StyleManager.loadStyleDoc("/screens/copy_mode_menu.json")
                            )
            );
            
            // 注册配置模式屏幕
            MenuScreens.register(
                    ModMenu.CONFIG_MODE_MENU.get(),
                    (ConfigModeMenu menu,
                     Inventory inv,
                     Component title) ->
                            new ConfigModeScreen(
                                    menu,
                                    inv,
                                    title,
                                    StyleManager.loadStyleDoc("/screens/config_mode_menu.json")
                            )
            );
        });
    }
}