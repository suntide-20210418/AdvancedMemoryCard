package com.suntide_20210418.advancedmemorycard.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod 配置注册中心
 * <p>
 * 客户端配置仅在物理客户端加载；服务端配置在服务端（物理服务端+单机逻辑服务端）加载。
 * </p>
 */
public class ModConfigs {
    private static ClientConfig clientConfig;
    private static ServerConfig serverConfig;

    private static ModConfigSpec clientSpec;
    private static ModConfigSpec serverSpec;

    /**
     * 构建所有配置规格，并向 NeoForge 注册。
     * 必须在 Mod 构造器中调用。
     * 使用 ModContainer 确保配置正确关联到当前模组的容器。
     */
    public static void register(ModContainer container) {
        // 构建客户端配置
        var clientPair = new ModConfigSpec.Builder()
                .configure(ClientConfig::new);
        clientConfig = clientPair.getKey();
        clientSpec = clientPair.getValue();

        // 构建服务端配置
        var serverPair = new ModConfigSpec.Builder()
                .configure(ServerConfig::new);
        serverConfig = serverPair.getKey();
        serverSpec = serverPair.getValue();

        // 向 NeoForge 注册客户端配置（仅在物理客户端注册）
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        }

        // 向 NeoForge 注册服务端配置
        container.registerConfig(ModConfig.Type.SERVER, serverSpec);
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
     * <p>服务端配置由 NeoForge 同步到客户端，所以客户端也读同一个值来决定「传送」按钮
     * 是否显示；但客户端不可信，服务端处理传送 action 时会再判一次。
     *
     * <p>配置尚未加载时按「不允许」处理：{@code ConfigValue#get()} 在 spec 未加载时
     * 会抛 {@link IllegalStateException}，而界面可能在世界刚载入时就被打开。
     */
    public static boolean isTeleportAllowed() {
        if (serverConfig == null || serverSpec == null || !serverSpec.isLoaded()) {
            return false;
        }
        return serverConfig.allowTeleport.get();
    }
}
