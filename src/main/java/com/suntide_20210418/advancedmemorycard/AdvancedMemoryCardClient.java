package com.suntide_20210418.advancedmemorycard;

import appeng.client.gui.style.StyleManager;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = AdvancedMemoryCardMod.MOD_ID, dist = Dist.CLIENT)
public class AdvancedMemoryCardClient {
    public AdvancedMemoryCardClient(ModContainer container) {
        // 注册 P2PRenderer 事件（通过 @SubscribeEvent 注解已注册，但需要确保实例化）
        P2PRenderer.getInstance();

        // 注册 Screen
        container.getEventBus().addListener(this::registerScreens);
    }

    @SubscribeEvent
    public void registerScreens(final RegisterMenuScreensEvent event) {
        // 屏幕构造器需要 ScreenStyle，由 StyleManager 从样式 JSON 加载
        event.<CopyModeMenu, CopyModeScreen>register(ModMenu.COPY_MODE_MENU.get(),
                (menu, inv, title) -> new CopyModeScreen(menu, inv, title,
                        StyleManager.loadStyleDoc("/screens/copy_mode_menu.json")));
        event.<ConfigModeMenu, ConfigModeScreen>register(ModMenu.CONFIG_MODE_MENU.get(),
                (menu, inv, title) -> new ConfigModeScreen(menu, inv, title,
                        StyleManager.loadStyleDoc("/screens/config_mode_menu.json")));
    }
}
