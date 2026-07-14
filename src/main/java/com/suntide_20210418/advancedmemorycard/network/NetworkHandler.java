package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static int packetId = 0;
    private static final String PROTOCOL_VERSION = "1";
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
