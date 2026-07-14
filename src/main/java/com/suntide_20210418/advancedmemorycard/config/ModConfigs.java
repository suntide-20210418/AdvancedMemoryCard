package com.suntide_20210418.advancedmemorycard.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Mod 配置注册中心
 * <p>
 * 客户端配置仅在物理客户端加载；服务端配置在服务端（物理服务端+单机逻辑服务端）加载。
 * </p>
 */
public class ModConfigs {
    // 注意：Forge 要求在 buildSpec 时就持有引用，所以这里用静态字段暂存
    private static ClientConfig clientConfig;
    private static ServerConfig serverConfig;

    private static net.minecraftforge.common.ForgeConfigSpec clientSpec;
    private static net.minecraftforge.common.ForgeConfigSpec serverSpec;

    /**
     * 构建所有配置规格，并向 Forge 注册。
     * 必须在 Mod 构造器中调用。
     * 使用 ModLoadingContext.get() 确保配置正确关联到当前模组的容器。
     */
    public static void register() {
        // 构建客户端配置
        var clientPair = new net.minecraftforge.common.ForgeConfigSpec.Builder()
                .configure(ClientConfig::new);
        clientConfig = clientPair.getKey();
        clientSpec = clientPair.getValue();

        // 构建服务端配置
        var serverPair = new net.minecraftforge.common.ForgeConfigSpec.Builder()
                .configure(ServerConfig::new);
        serverConfig = serverPair.getKey();
        serverSpec = serverPair.getValue();

        // 获取当前模组的 ModLoadingContext，确保配置关联到正确的模组容器
        var ctx = ModLoadingContext.get();

        // 向 Forge 注册客户端配置（仅在物理客户端注册）
        if (FMLEnvironment.dist.isClient()) {
            ctx.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        }

        // 向 Forge 注册服务端配置
        ctx.registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    public static ClientConfig getClientConfig() {
        return clientConfig;
    }

    public static ServerConfig getServerConfig() {
        return serverConfig;
    }
}
