package com.suntide_20210418.advancedmemorycard.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

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
}
