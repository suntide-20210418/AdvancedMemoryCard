package com.suntide_20210418.advancedmemorycard.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.CommonProxy;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.ConfigModeScreen;
import com.suntide_20210418.advancedmemorycard.client.gui.screen.CopyModeScreen;
import com.suntide_20210418.advancedmemorycard.client.key.KeyEvent;
import com.suntide_20210418.advancedmemorycard.client.key.ModKey;
import com.suntide_20210418.advancedmemorycard.client.renderer.CopyModeRenderer;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 客户端代理：负责模型注册、按键绑定、渲染器与客户端事件注册，以及客户端 GUI 屏幕的构造。
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // 按键绑定仅在客户端注册
        ModKey.register();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        ClientModelRegistry.registerModels();
        // 注意：KeyInputEvent 由 FML 派发到 FMLCommonHandler 自己的事件总线（与 MinecraftForge.EVENT_BUS 是不同实例），
        // 因此按键处理必须注册到 FMLCommonHandler.instance().bus()，否则 V 键切换模式不会生效。
        FMLCommonHandler.instance()
            .bus()
            .register(new KeyEvent());
        MinecraftForge.EVENT_BUS.register(P2PRenderer.getInstance());
        MinecraftForge.EVENT_BUS.register(new CopyModeRenderer());
    }

    @Override
    public Object openClientGui(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == AdvancedMemoryCardMod.CONFIG_GUI_ID) {
            ConfigModeMenu menu = new ConfigModeMenu(player.inventory);
            return new ConfigModeScreen(menu, player);
        } else if (ID == AdvancedMemoryCardMod.COPY_GUI_ID) {
            CopyModeMenu menu = new CopyModeMenu(player.inventory);
            return new CopyModeScreen(menu, player);
        }
        return null;
    }
}
