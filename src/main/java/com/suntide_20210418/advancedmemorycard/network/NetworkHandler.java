package com.suntide_20210418.advancedmemorycard.network;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {

    public static SimpleNetworkWrapper INSTANCE;

    // 客户端同步数据包队列：服务端发送的 ConfigModeSyncPacket 先入队，
    // 由客户端的 ConfigModeMenu 在每 tick 抽取并应用到本地缓存（避免在数据包处理线程中触碰客户端类）。
    public static final ConcurrentLinkedQueue<ConfigModeSyncPacket> syncQueue = new ConcurrentLinkedQueue<>();

    private static int discriminator = 0;

    public static void register() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("advanced_memory_card");

        // 服务端处理的消息（客户端 -> 服务端）
        INSTANCE.registerMessage(ModeSwitchPacket.Handler.class, ModeSwitchPacket.class, discriminator++, Side.SERVER);
        INSTANCE.registerMessage(
            ConfigModeActionPacket.Handler.class,
            ConfigModeActionPacket.class,
            discriminator++,
            Side.SERVER);
        INSTANCE.registerMessage(CopyModePacket.Handler.class, CopyModePacket.class, discriminator++, Side.SERVER);
        INSTANCE
            .registerMessage(OpenCopyGuiPacket.Handler.class, OpenCopyGuiPacket.class, discriminator++, Side.SERVER);
        // 客户端处理的消息（服务端 -> 客户端）。其 Handler 仅入队/调度，不引用任何客户端类，故服务端也可安全加载。
        INSTANCE.registerMessage(
            ConfigModeSyncPacket.Handler.class,
            ConfigModeSyncPacket.class,
            discriminator++,
            Side.CLIENT);
        INSTANCE.registerMessage(HighlightPacket.Handler.class, HighlightPacket.class, discriminator++, Side.CLIENT);
    }

    public static void sendToServer(IMessage msg) {
        INSTANCE.sendToServer(msg);
    }

    public static void sendToPlayer(IMessage msg, EntityPlayerMP player) {
        INSTANCE.sendTo(msg, player);
    }

    public static void sendToAll(IMessage msg) {
        INSTANCE.sendToAll(msg);
    }
}
