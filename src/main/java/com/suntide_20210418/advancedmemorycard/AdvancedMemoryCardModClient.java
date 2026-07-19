package com.suntide_20210418.advancedmemorycard;

import appeng.client.gui.style.StyleManager;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专属初始化类。
 * 仅作为物理客户端的 MOD 总线事件订阅者加载，确保其中引用的所有客户端类
 * （渲染器、屏幕、StyleManager 等）永远不会在专用服务器（无客户端类）上被加载。
 */
@Mod.EventBusSubscriber(
        modid = AdvancedMemoryCardMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class AdvancedMemoryCardModClient {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
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
