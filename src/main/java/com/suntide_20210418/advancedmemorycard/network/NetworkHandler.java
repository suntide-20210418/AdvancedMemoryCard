package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(ResourceLocationHelper.modLoc("main").toString());

    // 客户端同步数据包队列：服务端发送的 ConfigModeSyncPacket 先入队，
    // 由客户端的 ConfigModeMenu 在每 tick 抽取并应用到本地缓存（避免在数据包处理线程中触碰客户端类）。
    public static final ConcurrentLinkedQueue<ConfigModeSyncPacket> syncQueue = new ConcurrentLinkedQueue<>();

    private static int packetId = 0;

    public static void register() {
        // 服务端处理的消息（客户端 -> 服务端）
        INSTANCE.registerMessage(ModeSwitchPacket.Handler.class, ModeSwitchPacket.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(ConfigModeActionPacket.Handler.class, ConfigModeActionPacket.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(CopyModePacket.Handler.class, CopyModePacket.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(OpenCopyGuiPacket.Handler.class, OpenCopyGuiPacket.class, packetId++, Side.SERVER);
        // 客户端处理的消息（服务端 -> 客户端）。其 Handler 仅入队，不引用任何客户端类，故服务端也可安全加载。
        INSTANCE.registerMessage(ConfigModeSyncPacket.Handler.class, ConfigModeSyncPacket.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(HighlightPacket.Handler.class, HighlightPacket.class, packetId++, Side.CLIENT);
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
