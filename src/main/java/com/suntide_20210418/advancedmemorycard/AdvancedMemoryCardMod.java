package com.suntide_20210418.advancedmemorycard;

import com.suntide_20210418.advancedmemorycard.client.gui.ModGuiHandler;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(modid = AdvancedMemoryCardMod.MOD_ID, name = AdvancedMemoryCardMod.NAME, version = AdvancedMemoryCardMod.VERSION)
public class AdvancedMemoryCardMod {
    public static final String MOD_ID = "advanced_memory_card";
    public static final String NAME = "Advanced Memory Card";
    public static final String VERSION = "1.0.0";

    public static AdvancedMemoryCardMod INSTANCE;
    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        logger = event.getModLog();
        ModConfigs.init(event);
        NetworkHandler.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        MinecraftForge.EVENT_BUS.register(this);
        CardMode.initializeModes();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("AdvancedMemoryCardMod initialized");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new com.suntide_20210418.advancedmemorycard.command.TeleportCommand());
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        ModItems.register(event.getRegistry());
    }

    public static Logger getLogger() {
        return logger;
    }
}
