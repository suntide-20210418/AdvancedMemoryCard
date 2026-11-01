package com.suntide_20210418.advancedmemorycard.config;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class ModConfigs {
    private static ClientConfig clientConfig;
    private static ServerConfig serverConfig;

    public static void init(FMLPreInitializationEvent event) {
        File cfgDir = new File(event.getModConfigurationDirectory(), AdvancedMemoryCardMod.MOD_ID);
        Configuration clientCfg = new Configuration(new File(cfgDir, "client.cfg"));
        Configuration serverCfg = new Configuration(new File(cfgDir, "server.cfg"));

        clientConfig = new ClientConfig(clientCfg);
        serverConfig = new ServerConfig(serverCfg);

        if (clientCfg.hasChanged()) {
            clientCfg.save();
        }
        if (serverCfg.hasChanged()) {
            serverCfg.save();
        }
    }

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }

    public static ServerConfig getServerConfig() {
        return serverConfig;
    }

    /**
     * 是否允许传送至 P2P 设备（{@code server.allowTeleport}）。
     *
     * <p>配置尚未加载时按「不允许」处理；按钮显示与否走客户端缓存，
     * 服务端处理传送 action 时以这里的值为准（包可以伪造）。
     */
    public static boolean isTeleportAllowed() {
        return serverConfig != null && serverConfig.allowTeleport;
    }
}
