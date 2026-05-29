package com.suntide_20210418.advancedmemorycard.client.gui.menu;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 配置模式菜单
 * 负责 P2P 通道的频段配置、绑定管理
 * 服务端界面 menu，继承 AEBaseMenu，负责接收客户端数据并进行操作
 */
public class ConfigModeMenu extends AEBaseMenu {

    //action 标识符
    private static final String REFRESH_P2P = "refresh_p2p";
    private static final String BIND_FREQUENCY = "bind_frequency";
    private static final String SET_CHANNEL_ALIAS = "set_channel_alias";
    private static final String SET_P2P_ALIAS = "set_p2p_alias";
    private static final String SET_PENDING_BIND = "set_pending_bind";
    private static final String AUTO_CONFIG_IO = "auto_config_io";
    private static final String HIGHLIGHT_P2P = "highlight_p2p";
    private static final String HIGHLIGHT_P2P_TUNNEL = "highlight_p2p_tunnel";
    private static final String UPDATE_ITEM_INFO = "update_item_info";


    private ItemStack stack;
    private InteractionHand hand;
    private P2PManager p2pManager;
    private final HashMap<String, Short> p2pFrequencyAndAlias = new HashMap<>();
    private final HashMap<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap = new HashMap<>();
    private final HashMap<P2PTunnelPart<?>, P2PInfo> p2pInfoMap = new HashMap<>();
    private final HashMap<Short, ChannelInfo> channelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> p2pTypeInfoMap = new HashMap<>();

    // 客户端缓存数据（用于渲染）
    private final HashMap<String, Short> clientP2PFrequencyAndAlias = new HashMap<>();
    private final HashMap<P2PPosition, ResourceLocation> clientP2PDevicesMap = new HashMap<>();
    private final HashMap<P2PPosition, P2PInfo> clientP2PInfoMap = new HashMap<>();
    private final HashMap<Short, ChannelInfo> clientChannelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> clientP2PTypeInfoMap = new HashMap<>();



    @GuiSync(3)
    private Component mode;


    public ConfigModeMenu(int id, Inventory playerInventory, FriendlyByteBuf host) {
        super(ModMenu.CONFIG_MODE_MENU.get(), id, playerInventory, host);

        //action 注册，注意只能在这个构造函数注册
        registerClientAction(REFRESH_P2P, this::sendRefreshP2P);
        registerClientAction(BIND_FREQUENCY, Short.class, this::sendBindFrequency);
        registerClientAction(SET_CHANNEL_ALIAS, String.class, this::sendSetChannelAlias);
        registerClientAction(SET_P2P_ALIAS, String.class, this::sendSetP2PAlias);
        registerClientAction(SET_PENDING_BIND, P2PTunnelPart.class, this::sendSetPendingBind);
        registerClientAction(AUTO_CONFIG_IO, this::sendAutoConfigIO);
        registerClientAction(HIGHLIGHT_P2P, P2PTunnelPart.class, this::sendHighlightP2P);
        registerClientAction(HIGHLIGHT_P2P_TUNNEL, Short.class, this::sendHighlightP2PTunnel);
        registerClientAction(UPDATE_ITEM_INFO, this::sendUpdateItemInfo);
    }

    public ConfigModeMenu(int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);
        this.stack = this.getPlayer().getItemInHand(hand);
        this.mode = CardMode.of(stack).getName();
        this.hand = hand;
        updateItemInfo();
    }

    public void sendRefreshP2P() {
        if (this.isClientSide()) {
            sendClientAction(REFRESH_P2P);
        } else {
            p2pManager.analysisP2P();
        }
    }

    public void sendUpdateItemInfo() {
        if (this.isClientSide()) {
            sendClientAction(UPDATE_ITEM_INFO);
        } else {
            updateItemInfo();
        }
    }

    public void sendBindFrequency(short frequency) {
        if (this.isClientSide()) {
            sendClientAction(BIND_FREQUENCY, frequency);
        } else {
            p2pManager.bind(frequency);
        }
    }

    // 发送时传递复合参数
    public void sendSetChannelAlias(String data) {
        if (this.isClientSide()) {
            sendClientAction(SET_CHANNEL_ALIAS, data);
        } else {
            setChannelAlias(data);
        }
    }

    public void sendSetP2PAlias(String data) {
        if (this.isClientSide()) {
            sendClientAction(SET_P2P_ALIAS, data);
        } else {
            setP2PAlias(data);
        }
    }

    public void sendSetPendingBind(P2PTunnelPart<?> p2p) {
        if (this.isClientSide()) {
            sendClientAction(SET_PENDING_BIND, p2p);
        } else {
            setPendingBind(p2p);
        }
    }

    public void sendAutoConfigIO() {
        if (this.isClientSide()) {
            sendClientAction(AUTO_CONFIG_IO);
        } else {
            autoConfigIO();
        }
    }

    public void sendHighlightP2P(P2PTunnelPart<?> p2pPart) {
        if (this.isClientSide()) {
            sendClientAction(HIGHLIGHT_P2P, p2pPart);
        } else {
            highlightP2P(p2pPart);
        }
    }

    public void sendHighlightP2PTunnel(short frequency) {
        if (this.isClientSide()) {
            sendClientAction(HIGHLIGHT_P2P_TUNNEL);
        } else {
            highlightP2PTunnel(frequency);
        }
    }

    /**
     * 服务端：构建并发送同步数据包
     */
    private void sendSyncPacketToClient() {
        if (this.isServerSide()) {
            // 转换 P2PTunnelPart 为 P2PPosition
            HashMap<P2PPosition, ResourceLocation> positionDevicesMap = new HashMap<>();
            HashMap<P2PPosition, P2PInfo> positionInfoMap = new HashMap<>();

            for (var entry : p2pDevicesMap.entrySet()) {
                P2PTunnelPart<?> p2p = entry.getKey();
                P2PPosition position = new P2PPosition(
                        p2p.getBlockEntity().getBlockPos(),
                        p2p.getSide(),
                        p2p.getLevel().dimension().location().getPath()
                );
                positionDevicesMap.put(position, entry.getValue());
            }

            for (var entry : p2pInfoMap.entrySet()) {
                P2PTunnelPart<?> p2p = entry.getKey();
                P2PPosition position = new P2PPosition(
                        p2p.getBlockEntity().getBlockPos(),
                        p2p.getSide(),
                        p2p.getLevel().dimension().location().getPath()
                );
                positionInfoMap.put(position, entry.getValue());
            }

            ConfigModeSyncPacket packet = new ConfigModeSyncPacket(
                    p2pFrequencyAndAlias,
                    positionDevicesMap,
                    positionInfoMap,
                    channelInfoMap,
                    p2pTypeInfoMap
            );

            NetworkHandler.sendToPlayer(packet, getPlayer());
        }
    }

    /**
     * 客户端：接收同步数据
     */
    public void receiveSyncData(ConfigModeSyncPacket packet) {
        if (this.isClientSide()) {
            // 更新客户端缓存
            this.clientP2PFrequencyAndAlias.clear();
            this.clientP2PFrequencyAndAlias.putAll(packet.getP2PFrequencyAndAlias());

            this.clientP2PDevicesMap.clear();
            this.clientP2PDevicesMap.putAll(packet.getP2PDevicesMap());

            this.clientP2PInfoMap.clear();
            this.clientP2PInfoMap.putAll(packet.getP2PInfoMap());

            this.clientChannelInfoMap.clear();
            this.clientChannelInfoMap.putAll(packet.getChannelInfoMap());

            this.clientP2PTypeInfoMap.clear();
            this.clientP2PTypeInfoMap.putAll(packet.getP2PTypeInfoMap());
        }
    }

    public void updateItemInfo(){
        if (p2pManager == null) {
            return;
        }

        // 刷新 P2P 数据
        p2pManager.analysisP2P();

        // 清空所有 Map
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        p2pInfoMap.clear();
        channelInfoMap.clear();
        p2pTypeInfoMap.clear();

        // 1. 填充 p2pFrequencyAndAlias 和 p2pDevicesMap
        HashMap<String, Short> managerFrequencyAlias = p2pManager.getP2PFrequencyAndAlias();
        HashMap<P2PTunnelPart<?>, ResourceLocation> managerP2PDevices = p2pManager.getP2PDevicesMap();
            
        p2pFrequencyAndAlias.putAll(managerFrequencyAlias);
        p2pDevicesMap.putAll(managerP2PDevices);
            
        // 2. 构建 p2pInfoMap，收集每个 P2P 的详细信息
        for (HashMap.Entry<P2PTunnelPart<?>, ResourceLocation> entry : p2pDevicesMap.entrySet()) {
            P2PTunnelPart<?> p2p = entry.getKey();
            ResourceLocation p2pType = entry.getValue();
                
            boolean isActive = p2p.isActive();
            boolean isOutput = p2p.isOutput();
            boolean isConnected = P2PManager.isConnected(p2p);
            boolean isMEP2P = p2p instanceof MEP2PTunnelPart;
            boolean isPendingBind = Objects.equals(p2p, p2pManager.getP2PTunnelPart());
            int channel = isMEP2P ? p2p.getExternalFacingNode().getUsedChannels() : 0;
            int maxChannel = isMEP2P ? p2p.getExternalFacingNode().getMaxChannels() : 0;
            short frequency = p2p.getFrequency();
            String name = p2p.getCustomName() == null ? "" : p2p.getCustomName().getString();
            String p2pTypeName = p2pType != null ? p2pType.getPath() : "unknown";
            BlockPos pos = p2p.getBlockEntity().getBlockPos();
            Direction side = p2p.getSide();
            ResourceKey<Level> dimension = p2p.getBlockEntity().getLevel().dimension();
                
            P2PInfo info = new P2PInfo(isActive, isOutput, isConnected, isMEP2P, 
                    isPendingBind, channel, maxChannel, frequency, name, p2pTypeName,
                    dimension, pos, side);
                
            p2pInfoMap.put(p2p, info);
        }

        // 3. 按频率分组，构建 channelInfoMap
        HashMap<Short, ArrayList<P2PInfo>> frequencyGrouped = new HashMap<>();
        for (P2PInfo info : p2pInfoMap.values()) {
            frequencyGrouped.computeIfAbsent(info.frequency(), k -> new ArrayList<>()).add(info);
        }

        for (HashMap.Entry<Short, ArrayList<P2PInfo>> entry : frequencyGrouped.entrySet()) {
            short frequency = entry.getKey();
            ChannelInfo channelInfo = getChannelInfo(entry, frequency);
            channelInfoMap.put(frequency, channelInfo);
        }
            
        // 4. 按 P2P 类型分组，构建 p2pTypeInfoMap
        HashMap<String, ArrayList<ChannelInfo>> typeGroupedChannels = new HashMap<>();
        HashMap<String, ArrayList<P2PInfo>> typeGroupedP2Ps = new HashMap<>();
            
        for (ChannelInfo chInfo : channelInfoMap.values()) {
            typeGroupedChannels.computeIfAbsent(chInfo.p2pType(), k -> new ArrayList<>()).add(chInfo);
        }
            
        for (P2PInfo info : p2pInfoMap.values()) {
            typeGroupedP2Ps.computeIfAbsent(info.p2pType(), k -> new ArrayList<>()).add(info);
        }
            
        for (String p2pType : typeGroupedChannels.keySet()) {
            ArrayList<ChannelInfo> chList = typeGroupedChannels.get(p2pType);
            ArrayList<P2PInfo> p2pList = typeGroupedP2Ps.getOrDefault(p2pType, new ArrayList<>());
                
            // 计算该类型的总频道数和总 P2P 数
            int channelCount = chList.size();
            int p2pCount = p2pList.size();
                
            P2PTypeInfo typeInfo = new P2PTypeInfo(p2pType, channelCount, p2pCount
                    , chList, p2pList);
            p2pTypeInfoMap.put(p2pType, typeInfo);
        }

        broadcastChanges();

        // 发送同步包到客户端
        sendSyncPacketToClient();
    }

    private @NotNull ChannelInfo getChannelInfo(Map.Entry<Short, ArrayList<P2PInfo>> entry, short frequency) {
        ArrayList<P2PInfo> p2pList = entry.getValue();

        // 获取该频段的别名
        String alias = "frequency " + frequency;
        for (HashMap.Entry<String, Short> freqEntry : p2pFrequencyAndAlias.entrySet()) {
            if (freqEntry.getValue() == frequency) {
                alias = freqEntry.getKey();
                break;
            }
        }

        int usedChannels = 0;
        int totalChannels = 0;

        for (P2PInfo info : p2pList) {
            if (info.isMEP2P() || !info.isOutput()) {
                 usedChannels = info.channel();
                 totalChannels = info.maxChannel();
            }
        }
        int channelRemaining = totalChannels - usedChannels;

        // 获取 P2P 类型（取第一个设备的类型）
        String p2pType = p2pList.isEmpty() ? "unknown" : p2pList.get(0).p2pType();

        return new ChannelInfo(frequency, alias, p2pList.size(),
                channelRemaining, p2pType, p2pList);
    }

    // ==================== 以下方法需要具体实现 ====================

    // 接收时解析
    private void setChannelAlias(String data) {
        String[] parts = data.split("\\|");
        short frequency = Short.parseShort(parts[0]);
        String alias = parts[1];

        // 调用实际逻辑
        setChannelAlias(frequency, alias);
    }

    private void setChannelAlias(short frequency, String alias) {
        if (p2pManager != null) {
            p2pManager.setFrequencyAlias(alias, frequency);
            updateItemInfo();
        }
    }

    // 接收时解析并重新获取 P2P 部件
    private void setP2PAlias(String data) {
        String[] parts = data.split("::");
        String positionData = parts[0];
        String alias = parts[1];

        P2PTunnelPart<?> p2p = parsingP2P(positionData);
        setP2PAlias(p2p, alias);
    }

    private void setP2PAlias(P2PTunnelPart<?> p2p, String alias) {
        if (p2p != null && p2pManager != null) {
            p2pManager.renameP2P(p2p, alias);
            updateItemInfo();
        }
    }

    private P2PTunnelPart<?> parsingP2P(String data) {
        String[] posParts = data.split("\\|");
        BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]),
                Integer.parseInt(posParts[1]),
                Integer.parseInt(posParts[2]));
        Direction side = Direction.values()[Integer.parseInt(posParts[3])];

        // 从世界中重新获取 P2P 部件
        Player player = getPlayer();
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof IPartHost partHost) {
            IPart part = partHost.getPart(side);
            if (part instanceof P2PTunnelPart<?> p2pPart) {
                return p2pPart;
            }
        }
        return null;
    }

    private void setPendingBind(P2PTunnelPart<?> p2p) {
        p2pManager.setP2PTunnelPart(p2p);
    }

    private void autoConfigIO() {
        p2pManager.autoConfigP2PIO();
    }

    private void highlightP2P(P2PTunnelPart<?> p2pPart) {
        p2pManager.renderP2P(p2pPart);
    }

    private void highlightP2PTunnel(short frequency) {
        p2pManager.renderP2P(frequency);
    }

    /**
     * 获取频段的别名
     */
    private String getFrequencyAlias(short frequency) {
        for (HashMap.Entry<String, Short> entry : p2pFrequencyAndAlias.entrySet()) {
            if (entry.getValue() == frequency) {
                return entry.getKey();
            }
        }
        return "frequency " + frequency;
    }

    public Component getMode() {
        return mode;
    }

    // Getter 方法供 Screen 使用
    public HashMap<P2PPosition, P2PInfo> getClientP2PInfoMap() {
        return clientP2PInfoMap;
    }

    public HashMap<Short, ChannelInfo> getClientChannelInfoMap() {
        return clientChannelInfoMap;
    }

    public HashMap<String, P2PTypeInfo> getClientP2PTypeInfoMap() {
        return clientP2PTypeInfoMap;
    }

    public HashMap<String, Short> getClientP2PFrequencyAndAlias() {
        return clientP2PFrequencyAndAlias;
    }

    // 在类内部添加（例如靠近字段列表或方法末尾）
    public void setP2PManager(P2PManager manager) {
        this.p2pManager = manager;
        // 立刻更新一次信息（在服务端构造时会触发）
        updateItemInfo();
    }
}
