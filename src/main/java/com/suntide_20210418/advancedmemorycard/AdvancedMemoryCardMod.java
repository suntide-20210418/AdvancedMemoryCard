package com.suntide_20210418.advancedmemorycard;

import org.apache.logging.log4j.Logger;

import com.suntide_20210418.advancedmemorycard.command.TeleportCommand;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(
    modid = AdvancedMemoryCardMod.MOD_ID,
    name = AdvancedMemoryCardMod.NAME,
    version = AdvancedMemoryCardMod.VERSION,
    dependencies = "required-after:appliedenergistics2;")
public class AdvancedMemoryCardMod {

    public static final String MOD_ID = "advanced_memory_card";
    public static final String NAME = "Advanced Memory Card";
    // Tags 由 GTNH 构建约定在 injectTags 源集里生成（gradle.properties: generateGradleTokenClass），
    // 这样 @Mod(version) 与 mcmod.info 的 ${modVersion} 才来自同一个 gradle 版本号。
    public static final String VERSION = Tags.VERSION;

    // GUI 标识（服务端/客户端共用，避免通用代码引用 client 包下的类）
    public static final int CONFIG_GUI_ID = 0;
    public static final int COPY_GUI_ID = 1;

    public static AdvancedMemoryCardMod INSTANCE;
    @SidedProxy(
        clientSide = "com.suntide_20210418.advancedmemorycard.client.ClientProxy",
        serverSide = "com.suntide_20210418.advancedmemorycard.CommonProxy")
    public static CommonProxy proxy;
    private static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        INSTANCE = this;
        logger = event.getModLog();
        ModConfigs.init(event);
        NetworkHandler.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        CardMode.initializeModes();
        ModItems.register();
        // 右键接管：PlayerInteractEvent 早于 PlayerControllerMP#onPlayerRightClick 里的物品分派，
        // 是 1.7.10 上唯一稳定的拦截点。
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.suntide_20210418.advancedmemorycard.event.CardInteractHandler());
        // 临时探针：必须在 Mod 入口注册而不是 proxy.preInit —— ClientProxy 覆写了 preInit
        // 且没有调用 super，放在 proxy 里客户端根本收不到。
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.suntide_20210418.advancedmemorycard.debug.InteractionProbe());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("AdvancedMemoryCardMod initialized");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new TeleportCommand());
    }

    public static Logger getLogger() {
        return logger;
    }
}
