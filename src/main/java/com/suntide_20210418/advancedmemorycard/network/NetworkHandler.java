package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID)
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ConfigModeSyncPacket.TYPE,
                ConfigModeSyncPacket.STREAM_CODEC,
                ConfigModeSyncPacket::handleClient
        );

        registrar.playToServer(
                OpenGuiPacket.TYPE,
                OpenGuiPacket.STREAM_CODEC,
                OpenGuiPacket::handleServer
        );

        registrar.playToServer(
                ModeSwitchPacket.TYPE,
                ModeSwitchPacket.STREAM_CODEC,
                ModeSwitchPacket::handleServer
        );
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToPlayer(CustomPacketPayload message, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }
}
