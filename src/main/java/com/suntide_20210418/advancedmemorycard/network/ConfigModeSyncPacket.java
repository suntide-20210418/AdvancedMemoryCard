package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

public class ConfigModeSyncPacket {
    private final HashMap<String, String> p2pFrequencyAndAlias;
    private final HashMap<P2PPosition, ResourceLocation> p2pDevicesMap;
    private final HashMap<P2PPosition, P2PInfo> p2pInfoMap;
    private final HashMap<String, ChannelInfo> channelInfoMap;
    private final HashMap<String, P2PTypeInfo> p2pTypeInfoMap;

    public ConfigModeSyncPacket(
            HashMap<String, String> p2pFrequencyAndAlias,
            HashMap<P2PPosition, ResourceLocation> p2pDevicesMap,
            HashMap<P2PPosition, P2PInfo> p2pInfoMap,
            HashMap<String, ChannelInfo> channelInfoMap,
            HashMap<String, P2PTypeInfo> p2pTypeInfoMap) {
        this.p2pFrequencyAndAlias = p2pFrequencyAndAlias;
        this.p2pDevicesMap = p2pDevicesMap;
        this.p2pInfoMap = p2pInfoMap;
        this.channelInfoMap = channelInfoMap;
        this.p2pTypeInfoMap = p2pTypeInfoMap;
    }

    public static void encode(ConfigModeSyncPacket msg, FriendlyByteBuf buffer) {
        // Encode p2pFrequencyAndAlias
        buffer.writeInt(msg.p2pFrequencyAndAlias.size());
        for (var entry : msg.p2pFrequencyAndAlias.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }

        // Encode p2pDevicesMap
        buffer.writeInt(msg.p2pDevicesMap.size());
        for (var entry : msg.p2pDevicesMap.entrySet()) {
            buffer.writeBlockPos(entry.getKey().position());
            buffer.writeEnum(entry.getKey().direction());
            buffer.writeUtf(entry.getKey().dimension());
            buffer.writeResourceLocation(entry.getValue());
        }

        // Encode p2pInfoMap
        buffer.writeInt(msg.p2pInfoMap.size());
        for (var entry : msg.p2pInfoMap.entrySet()) {
            buffer.writeBlockPos(entry.getKey().position());
            buffer.writeEnum(entry.getKey().direction());
            buffer.writeUtf(entry.getKey().dimension());
            writeP2PInfo(buffer, entry.getValue());
        }

        // Encode channelInfoMap
        buffer.writeInt(msg.channelInfoMap.size());
        for (var entry : msg.channelInfoMap.entrySet()) {
            buffer.writeUtf(entry.getKey());
            writeChannelInfo(buffer, entry.getValue());
        }

        // Encode p2pTypeInfoMap
        buffer.writeInt(msg.p2pTypeInfoMap.size());
        for (var entry : msg.p2pTypeInfoMap.entrySet()) {
            buffer.writeUtf(entry.getKey());
            writeP2PTypeInfo(buffer, entry.getValue());
        }
    }

    public static ConfigModeSyncPacket decode(FriendlyByteBuf buffer) {
        // Decode p2pFrequencyAndAlias
        HashMap<String, String> p2pFrequencyAndAlias = new HashMap<>();
        int size1 = buffer.readInt();
        for (int i = 0; i < size1; i++) {
            String key = buffer.readUtf();
            String value = buffer.readUtf();
            p2pFrequencyAndAlias.put(key, value);
        }

        // Decode p2pDevicesMap
        HashMap<P2PPosition, ResourceLocation> p2pDevicesMap = new HashMap<>();
        int size2 = buffer.readInt();
        for (int i = 0; i < size2; i++) {
            BlockPos pos = buffer.readBlockPos();
            Direction direction = buffer.readEnum(Direction.class);
            String dimension = buffer.readResourceLocation().getPath();
            P2PPosition position = new P2PPosition(pos, direction, dimension);
            ResourceLocation location = buffer.readResourceLocation();
            p2pDevicesMap.put(position, location);
        }

        // Decode p2pInfoMap
        HashMap<P2PPosition, P2PInfo> p2pInfoMap = new HashMap<>();
        int size3 = buffer.readInt();
        for (int i = 0; i < size3; i++) {
            BlockPos pos = buffer.readBlockPos();
            Direction direction = buffer.readEnum(Direction.class);
            String dimension = buffer.readResourceLocation().getPath();
            P2PPosition position = new P2PPosition(pos, direction, dimension);
            P2PInfo info = readP2PInfo(buffer);
            p2pInfoMap.put(position, info);
        }

        // Decode channelInfoMap
        HashMap<String, ChannelInfo> channelInfoMap = new HashMap<>();
        int size4 = buffer.readInt();
        for (int i = 0; i < size4; i++) {
            String frequency = buffer.readUtf();
            ChannelInfo info = readChannelInfo(buffer);
            channelInfoMap.put(frequency, info);
        }

        // Decode p2pTypeInfoMap
        HashMap<String, P2PTypeInfo> p2pTypeInfoMap = new HashMap<>();
        int size5 = buffer.readInt();
        for (int i = 0; i < size5; i++) {
            String type = buffer.readUtf();
            P2PTypeInfo info = readP2PTypeInfo(buffer);
            p2pTypeInfoMap.put(type, info);
        }

        return new ConfigModeSyncPacket(p2pFrequencyAndAlias,
                p2pDevicesMap,
                p2pInfoMap,
                channelInfoMap,
                p2pTypeInfoMap);
    }

    private static void writeP2PInfo(FriendlyByteBuf buffer, P2PInfo info) {
        buffer.writeBoolean(info.isActive());
        buffer.writeBoolean(info.isOutput());
        buffer.writeBoolean(info.isConnected());
        buffer.writeBoolean(info.isMEP2P());
        buffer.writeBoolean(info.isPendingBind());
        buffer.writeInt(info.channel());
        buffer.writeInt(info.maxChannel());
        buffer.writeUtf(info.frequency());
        buffer.writeUtf(info.name());
        buffer.writeUtf(info.p2pType());
        buffer.writeResourceKey(info.dimension());
        buffer.writeBlockPos(info.position());
        buffer.writeEnum(info.direction());
    }

    private static P2PInfo readP2PInfo(FriendlyByteBuf buffer) {
        boolean isActive = buffer.readBoolean();
        boolean isOutput = buffer.readBoolean();
        boolean isConnected = buffer.readBoolean();
        boolean isMEP2P = buffer.readBoolean();
        boolean isPendingBind = buffer.readBoolean();
        int channel = buffer.readInt();
        int maxChannel = buffer.readInt();
        String frequency = buffer.readUtf();
        String name = buffer.readUtf();
        String p2pType = buffer.readUtf();
        ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
        BlockPos position = buffer.readBlockPos();
        Direction side = buffer.readEnum(Direction.class);

        return new P2PInfo(isActive, isOutput, isConnected, isMEP2P,
                isPendingBind, channel, maxChannel, frequency, name, p2pType,
                dimension, position, side);
    }

    private static void writeChannelInfo(FriendlyByteBuf buffer, ChannelInfo info) {
        buffer.writeUtf(info.frequency());
        buffer.writeUtf(info.alias());
        buffer.writeInt(info.p2pCount());
        buffer.writeInt(info.maxChannel());
        buffer.writeInt(info.channelRemaining());
        buffer.writeUtf(info.p2pType());

        // Write P2PInfo list
        buffer.writeInt(info.p2pInfoList().size());
        for (P2PInfo p2pInfo : info.p2pInfoList()) {
            writeP2PInfo(buffer, p2pInfo);
        }
    }

    private static ChannelInfo readChannelInfo(FriendlyByteBuf buffer) {
        String frequency = buffer.readUtf();
        String alias = buffer.readUtf();
        int p2pCount = buffer.readInt();
        int maxChannel = buffer.readInt();
        int channelRemaining = buffer.readInt();
        String p2pType = buffer.readUtf();

        int size = buffer.readInt();
        ArrayList<P2PInfo> p2pList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            p2pList.add(readP2PInfo(buffer));
        }

        return new ChannelInfo(frequency, alias, p2pCount, maxChannel, channelRemaining, p2pType, p2pList);
    }

    private static void writeP2PTypeInfo(FriendlyByteBuf buffer, P2PTypeInfo info) {
        buffer.writeUtf(info.p2pType());
        buffer.writeInt(info.channelCount());
        buffer.writeInt(info.p2pCount());

        // Write ChannelInfo list
        buffer.writeInt(info.channelInfoList().size());
        for (ChannelInfo chInfo : info.channelInfoList()) {
            writeChannelInfo(buffer, chInfo);
        }

        // Write P2PInfo list
        buffer.writeInt(info.p2pInfoList().size());
        for (P2PInfo p2pInfo : info.p2pInfoList()) {
            writeP2PInfo(buffer, p2pInfo);
        }
    }

    private static P2PTypeInfo readP2PTypeInfo(FriendlyByteBuf buffer) {
        String p2pType = buffer.readUtf();
        int channelCount = buffer.readInt();
        int p2pCount = buffer.readInt();

        int chSize = buffer.readInt();
        ArrayList<ChannelInfo> channelList = new ArrayList<>();
        for (int i = 0; i < chSize; i++) {
            channelList.add(readChannelInfo(buffer));
        }

        int p2pSize = buffer.readInt();
        ArrayList<P2PInfo> p2pList = new ArrayList<>();
        for (int i = 0; i < p2pSize; i++) {
            p2pList.add(readP2PInfo(buffer));
        }

        return new P2PTypeInfo(p2pType, channelCount, p2pCount, channelList, p2pList);
    }

    public static void handle(ConfigModeSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 获取当前打开的 Menu
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 客户端处理
                var player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof ConfigModeMenu menu) {
                    menu.receiveSyncData(msg);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // getter 方法
    public HashMap<String, String> getP2PFrequencyAndAlias() {
        return p2pFrequencyAndAlias;
    }

    public HashMap<P2PPosition, ResourceLocation> getP2PDevicesMap() {
        return p2pDevicesMap;
    }

    public HashMap<P2PPosition, P2PInfo> getP2PInfoMap() {
        return p2pInfoMap;
    }

    public HashMap<String, ChannelInfo> getChannelInfoMap() {
        return channelInfoMap;
    }

    public HashMap<String, P2PTypeInfo> getP2PTypeInfoMap() {
        return p2pTypeInfoMap;
    }
}