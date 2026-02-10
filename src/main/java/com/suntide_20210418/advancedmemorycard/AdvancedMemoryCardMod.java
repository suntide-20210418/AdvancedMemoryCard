package com.suntide_20210418.advancedmemorycard;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.advancedmemorycard.client.renderer.CopyModeRenderer;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AdvancedMemoryCardMod.MOD_ID)
public class AdvancedMemoryCardMod {
    public static final String MOD_ID = "advanced_memory_card";

    private static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedMemoryCardMod(IEventBus modEventBus) {
        LOGGER.info("AdvancedMemoryCardMod is loading");

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 在公共设置事件中初始化模式
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing card modes...");
        CardMode.initializeModes();
        LOGGER.info("Card modes initialized successfully");
    }
}
