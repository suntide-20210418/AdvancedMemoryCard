package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 服务端 -> 客户端：同步 P2P 网络数据到配置模式 GUI 的本地缓存。
 * 本类（含 Handler）不引用任何客户端类，因此可在专用服务端安全加载。
 */
public class ConfigModeSyncPacket implements IMessage {
    private HashMap<String, String> p2pFrequencyAndAlias;
    private HashMap<P2PPosition, String> p2pDevicesMap;
    private HashMap<P2PPosition, P2PInfo> p2pInfoMap;
    private HashMap<String, ChannelInfo> channelInfoMap;
    private HashMap<String, P2PTypeInfo> p2pTypeInfoMap;

    public ConfigModeSyncPacket() {
    }

    public ConfigModeSyncPacket(HashMap<String, String> p2pFrequencyAndAlias,
            HashMap<P2PPosition, String> p2pDevicesMap,
            HashMap<P2PPosition, P2PInfo> p2pInfoMap,
            HashMap<String, ChannelInfo> channelInfoMap,
            HashMap<String, P2PTypeInfo> p2pTypeInfoMap) {
        this.p2pFrequencyAndAlias = p2pFrequencyAndAlias;
        this.p2pDevicesMap = p2pDevicesMap;
        this.p2pInfoMap = p2pInfoMap;
        this.channelInfoMap = channelInfoMap;
        this.p2pTypeInfoMap = p2pTypeInfoMap;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        writeStringMap(buf, p2pFrequencyAndAlias);
        writePosStringMap(buf, p2pDevicesMap);
        writePosP2PInfoMap(buf, p2pInfoMap);
        writeStringChannelMap(buf, channelInfoMap);
        writeStringTypeMap(buf, p2pTypeInfoMap);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        p2pFrequencyAndAlias = readStringMap(buf);
        p2pDevicesMap = readPosStringMap(buf);
        p2pInfoMap = readPosP2PInfoMap(buf);
        channelInfoMap = readStringChannelMap(buf);
        p2pTypeInfoMap = readStringTypeMap(buf);
    }

    // ==================== 访问器 ====================

    public HashMap<String, String> getP2PFrequencyAndAlias() {
        return p2pFrequencyAndAlias;
    }

    public HashMap<P2PPosition, String> getP2PDevicesMap() {
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

    // ==================== 编码辅助 ====================

    private static void writeStringMap(ByteBuf buf, HashMap<String, String> map) {
        buf.writeInt(map.size());
        for (HashMap.Entry<String, String> e : map.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, e.getKey());
            ByteBufUtils.writeUTF8String(buf, e.getValue());
        }
    }

    private static HashMap<String, String> readStringMap(ByteBuf buf) {
        HashMap<String, String> map = new HashMap<>();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            map.put(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        }
        return map;
    }

    private static void writePosStringMap(ByteBuf buf, HashMap<P2PPosition, String> map) {
        buf.writeInt(map.size());
        for (HashMap.Entry<P2PPosition, String> e : map.entrySet()) {
            writeP2PPosition(buf, e.getKey());
            ByteBufUtils.writeUTF8String(buf, e.getValue());
        }
    }

    private static HashMap<P2PPosition, String> readPosStringMap(ByteBuf buf) {
        HashMap<P2PPosition, String> map = new HashMap<>();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            map.put(readP2PPosition(buf), ByteBufUtils.readUTF8String(buf));
        }
        return map;
    }

    private static void writePosP2PInfoMap(ByteBuf buf, HashMap<P2PPosition, P2PInfo> map) {
        buf.writeInt(map.size());
        for (HashMap.Entry<P2PPosition, P2PInfo> e : map.entrySet()) {
            writeP2PPosition(buf, e.getKey());
            writeP2PInfo(buf, e.getValue());
        }
    }

    private static HashMap<P2PPosition, P2PInfo> readPosP2PInfoMap(ByteBuf buf) {
        HashMap<P2PPosition, P2PInfo> map = new HashMap<>();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            map.put(readP2PPosition(buf), readP2PInfo(buf));
        }
        return map;
    }

    private static void writeStringChannelMap(ByteBuf buf, HashMap<String, ChannelInfo> map) {
        buf.writeInt(map.size());
        for (HashMap.Entry<String, ChannelInfo> e : map.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, e.getKey());
            writeChannelInfo(buf, e.getValue());
        }
    }

    private static HashMap<String, ChannelInfo> readStringChannelMap(ByteBuf buf) {
        HashMap<String, ChannelInfo> map = new HashMap<>();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            map.put(ByteBufUtils.readUTF8String(buf), readChannelInfo(buf));
        }
        return map;
    }

    private static void writeStringTypeMap(ByteBuf buf, HashMap<String, P2PTypeInfo> map) {
        buf.writeInt(map.size());
        for (HashMap.Entry<String, P2PTypeInfo> e : map.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, e.getKey());
            writeP2PTypeInfo(buf, e.getValue());
        }
    }

    private static HashMap<String, P2PTypeInfo> readStringTypeMap(ByteBuf buf) {
        HashMap<String, P2PTypeInfo> map = new HashMap<>();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            map.put(ByteBufUtils.readUTF8String(buf), readP2PTypeInfo(buf));
        }
        return map;
    }

    private static void writeP2PPosition(ByteBuf buf, P2PPosition p) {
        buf.writeInt(p.position.x);
        buf.writeInt(p.position.y);
        buf.writeInt(p.position.z);
        buf.writeByte(p.direction.ordinal());
        ByteBufUtils.writeUTF8String(buf, p.dimension);
    }

    private static P2PPosition readP2PPosition(ByteBuf buf) {
        BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        ForgeDirection direction = ForgeDirection.values()[buf.readByte()];
        String dimension = ByteBufUtils.readUTF8String(buf);
        return new P2PPosition(pos, direction, dimension);
    }

    private static void writeP2PInfo(ByteBuf buf, P2PInfo info) {
        buf.writeBoolean(info.isActive);
        buf.writeBoolean(info.isOutput);
        buf.writeBoolean(info.isConnected);
        buf.writeBoolean(info.isMEP2P);
        buf.writeBoolean(info.isPendingBind);
        buf.writeInt(info.channel);
        buf.writeInt(info.maxChannel);
        ByteBufUtils.writeUTF8String(buf, info.frequency);
        ByteBufUtils.writeUTF8String(buf, info.name);
        ByteBufUtils.writeUTF8String(buf, info.p2pType);
        ByteBufUtils.writeUTF8String(buf, info.dimension);
        buf.writeInt(info.dimensionId);
        buf.writeInt(info.position.x);
        buf.writeInt(info.position.y);
        buf.writeInt(info.position.z);
        buf.writeByte(info.direction.ordinal());
    }

    private static P2PInfo readP2PInfo(ByteBuf buf) {
        boolean isActive = buf.readBoolean();
        boolean isOutput = buf.readBoolean();
        boolean isConnected = buf.readBoolean();
        boolean isMEP2P = buf.readBoolean();
        boolean isPendingBind = buf.readBoolean();
        int channel = buf.readInt();
        int maxChannel = buf.readInt();
        String frequency = ByteBufUtils.readUTF8String(buf);
        String name = ByteBufUtils.readUTF8String(buf);
        String p2pType = ByteBufUtils.readUTF8String(buf);
        String dimension = ByteBufUtils.readUTF8String(buf);
        int dimensionId = buf.readInt();
        BlockPos position = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        ForgeDirection direction = ForgeDirection.values()[buf.readByte()];
        return new P2PInfo(isActive, isOutput, isConnected, isMEP2P, isPendingBind, channel, maxChannel,
                frequency, name, p2pType, dimension, dimensionId, position, direction);
    }

    private static void writeChannelInfo(ByteBuf buf, ChannelInfo info) {
        ByteBufUtils.writeUTF8String(buf, info.frequency);
        ByteBufUtils.writeUTF8String(buf, info.alias);
        buf.writeInt(info.p2pCount);
        buf.writeInt(info.maxChannel);
        buf.writeInt(info.channelRemaining);
        ByteBufUtils.writeUTF8String(buf, info.p2pType);
        buf.writeInt(info.p2pInfoList.size());
        for (P2PInfo p : info.p2pInfoList) {
            writeP2PInfo(buf, p);
        }
    }

    private static ChannelInfo readChannelInfo(ByteBuf buf) {
        String frequency = ByteBufUtils.readUTF8String(buf);
        String alias = ByteBufUtils.readUTF8String(buf);
        int p2pCount = buf.readInt();
        int maxChannel = buf.readInt();
        int channelRemaining = buf.readInt();
        String p2pType = ByteBufUtils.readUTF8String(buf);
        int n = buf.readInt();
        ArrayList<P2PInfo> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(readP2PInfo(buf));
        }
        return new ChannelInfo(frequency, alias, p2pCount, maxChannel, channelRemaining, p2pType, list);
    }

    private static void writeP2PTypeInfo(ByteBuf buf, P2PTypeInfo info) {
        ByteBufUtils.writeUTF8String(buf, info.p2pType);
        buf.writeInt(info.channelCount);
        buf.writeInt(info.p2pCount);
        buf.writeInt(info.channelInfoList.size());
        for (ChannelInfo c : info.channelInfoList) {
            writeChannelInfo(buf, c);
        }
        buf.writeInt(info.p2pInfoList.size());
        for (P2PInfo p : info.p2pInfoList) {
            writeP2PInfo(buf, p);
        }
    }

    private static P2PTypeInfo readP2PTypeInfo(ByteBuf buf) {
        String p2pType = ByteBufUtils.readUTF8String(buf);
        int channelCount = buf.readInt();
        int p2pCount = buf.readInt();
        int cn = buf.readInt();
        ArrayList<ChannelInfo> channels = new ArrayList<>();
        for (int i = 0; i < cn; i++) {
            channels.add(readChannelInfo(buf));
        }
        int pn = buf.readInt();
        ArrayList<P2PInfo> p2ps = new ArrayList<>();
        for (int i = 0; i < pn; i++) {
            p2ps.add(readP2PInfo(buf));
        }
        return new P2PTypeInfo(p2pType, channelCount, p2pCount, channels, p2ps);
    }

    public static class Handler implements IMessageHandler<ConfigModeSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(ConfigModeSyncPacket msg, MessageContext ctx) {
            // 仅入队，由客户端 ConfigModeMenu 在每 tick 抽取，避免在处理线程中触碰客户端类
            NetworkHandler.syncQueue.offer(msg);
            return null;
        }
    }
}
