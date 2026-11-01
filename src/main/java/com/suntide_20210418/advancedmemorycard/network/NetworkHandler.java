package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static int packetId = 0;
    /**
     * 通道协议版本。
     *
     * <p>1.3.0 的配置模式同步包加了配对源与状态码字段，与 1.2.0 的字节布局不兼容。
     * 升到 "2" 让旧客户端在握手期就被拒绝，而不是进游戏后在读包时错位解析。
     */
    private static final String PROTOCOL_VERSION = "2";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocationHelper.modLoc("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        INSTANCE.registerMessage(packetId++,
                ConfigModeSyncPacket.class,
                ConfigModeSyncPacket::encode,
                ConfigModeSyncPacket::decode,
                ConfigModeSyncPacket::handle);

        INSTANCE.registerMessage(packetId++,
                OpenGuiPacket.class,
                OpenGuiPacket::encode,
                OpenGuiPacket::decode,
                OpenGuiPacket::handle);

        INSTANCE.registerMessage(packetId++,
                ModeSwitchPacket.class,
                ModeSwitchPacket::encode,
                ModeSwitchPacket::decode,
                ModeSwitchPacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), message);
        }
    }

    public static <MSG> void sendToAllPlayers(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
