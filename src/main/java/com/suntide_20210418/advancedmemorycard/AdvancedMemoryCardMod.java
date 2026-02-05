package com.suntide_20210418.advancedmemorycard;

import com.mojang.logging.LogUtils;
import com.suntide_20210418.advancedmemorycard.client.renderer.CopyModeRenderer;
import com.suntide_20210418.advancedmemorycard.item.ModCreativeModeTabs;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AdvancedMemoryCardMod.MOD_ID)
public class AdvancedMemoryCardMod {
  public static final String MOD_ID = "advanced_memory_card";

  private static final Logger LOGGER = LogUtils.getLogger();

  public AdvancedMemoryCardMod(FMLJavaModLoadingContext context) {
    IEventBus modEventBus = context.getModEventBus();
    LOGGER.info("AdvancedMemoryCardMod is loading");

    ModItems.register(modEventBus);
    ModCreativeModeTabs.register(modEventBus);

    // 在公共设置事件中初始化模式
    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(CopyModeRenderer.class);
    modEventBus.addListener(this::commonSetup);
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    LOGGER.info("Initializing card modes...");
    CardMode.initializeModes();
    LOGGER.info("Card modes initialized successfully");
  }
}
