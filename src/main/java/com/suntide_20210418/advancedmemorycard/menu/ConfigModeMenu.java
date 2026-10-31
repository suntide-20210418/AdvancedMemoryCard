package com.suntide_20210418.advancedmemorycard.menu;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import com.mojang.logging.LogUtils;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import com.suntide_20210418.advancedmemorycard.utils.ActionStatus;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

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

    private static final Logger LOGGER = LogUtils.getLogger();

    //action 标识符
    private static final String REFRESH_P2P = "refresh_p2p";
    private static final String BIND_FREQUENCY = "bind_frequency";
    private static final String BIND_FREQUENCY_BATCH = "bind_frequency_batch";
    private static final String SET_CHANNEL_ALIAS = "set_channel_alias";
    private static final String SET_P2P_ALIAS = "set_p2p_alias";
    private static final String SET_PENDING_BIND = "set_pending_bind";
    private static final String CLEAR_PENDING_BIND = "clear_pending_bind";
    private static final String AUTO_CONFIG_IO = "auto_config_io";
    private static final String ASSIGN_FREQ = "assign_freq";
    private static final String SWITCH_TO_INPUT = "switch_to_input";
    private static final String HIGHLIGHT_P2P = "highlight_p2p";
    private static final String HIGHLIGHT_P2P_TUNNEL = "highlight_p2p_tunnel";
    private static final String TELEPORT_P2P = "teleport_p2p";
    private static final String UPDATE_ITEM_INFO = "update_item_info";


    private ItemStack stack;
    private InteractionHand hand;
    private P2PManager p2pManager;
    private final HashMap<String, String> p2pFrequencyAndAlias = new HashMap<>();
    private final HashMap<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap = new HashMap<>();
    private final HashMap<P2PTunnelPart<?>, P2PInfo> p2pInfoMap = new HashMap<>();
    private final HashMap<String, ChannelInfo> channelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> p2pTypeInfoMap = new HashMap<>();

    // 客户端缓存数据（用于渲染）
    private final HashMap<String, String> clientP2PFrequencyAndAlias = new HashMap<>();
    private final HashMap<P2PPosition, ResourceLocation> clientP2PDevicesMap = new HashMap<>();
    private final HashMap<P2PPosition, P2PInfo> clientP2PInfoMap = new HashMap<>();
    private final HashMap<String, ChannelInfo> clientChannelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> clientP2PTypeInfoMap = new HashMap<>();

    // 客户端缓存的当前待绑定 P2P 信息（从同步数据中提取，isPendingBind=true 的那一个）
    private P2PInfo clientPendingBindP2PInfo = null;
    private P2PPosition clientPendingBindPosition = null;
    private int clientStatusCode = ActionStatus.NONE;
    private String clientStatusParam = "";
    /** 每收到一次同步包 +1。客户端据此决定要不要重建树，避免每 tick 重建 20 次。 */
    private int syncVersion = 0;

    /**
     * 最近一次操作的結果码 + 参数，随同步包一起下发。
     * 客户端把它渲染成状态行文案；下次操作覆盖。
     */
    private int statusCode = ActionStatus.NONE;
    private String statusParam = "";

    @GuiSync(3)
    private Component mode;

    public ConfigModeMenu(int id, Inventory playerInventory, FriendlyByteBuf host) {
        super(ModMenu.CONFIG_MODE_MENU.get(), id, playerInventory, host);

        //action 注册，注意只能在这个构造函数注册
        registerClientAction(REFRESH_P2P, this::handleRefreshP2P);
        registerClientAction(BIND_FREQUENCY, String.class, this::handleBindFrequency);
        registerClientAction(BIND_FREQUENCY_BATCH, String.class, this::handleBindFrequencyBatch);
        registerClientAction(SET_CHANNEL_ALIAS, String.class, this::handleSetChannelAlias);
        registerClientAction(SET_P2P_ALIAS, String.class, this::handleSetP2PAlias);
        registerClientAction(SET_PENDING_BIND, String.class, this::handleSetPendingBind);
        registerClientAction(CLEAR_PENDING_BIND, this::handleClearPendingBind);
        registerClientAction(AUTO_CONFIG_IO, this::handleAutoConfigIO);
        registerClientAction(ASSIGN_FREQ, this::handleAssignFreq);
        registerClientAction(SWITCH_TO_INPUT, String.class, this::handleSwitchToInput);
        registerClientAction(HIGHLIGHT_P2P, String.class, this::handleHighlightP2P);
        registerClientAction(HIGHLIGHT_P2P_TUNNEL, String.class, this::handleHighlightP2PTunnel);
        registerClientAction(TELEPORT_P2P, String.class, this::handleTeleportP2P);
        registerClientAction(UPDATE_ITEM_INFO, this::handleUpdateItemInfo);
    }

    public ConfigModeMenu(int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);
        this.stack = this.getPlayer().getItemInHand(hand);
        this.mode = CardMode.of(stack).getName();
        this.hand = hand;
        updateItemInfo();
    }

    // ==================== 服务端 Action 处理（由 registerClientAction 回调触发） ====================

    private void handleRefreshP2P() {
        if (p2pManager == null) return;
        p2pManager.analysisP2P();
        statusCode = ActionStatus.REFRESHED;
        statusParam = "";
    }

    private void handleUpdateItemInfo() {
        updateItemInfo();
    }

    private void handleBindFrequency(String frequencyHex) {
        if (p2pManager == null) return;
        P2PManager.BindResult result = p2pManager.bind(frequencyHex);
        statusParam = frequencyHex == null ? "" : frequencyHex;
        statusCode = switch (result) {
            case OK -> ActionStatus.BIND_OK;
            case NO_TARGET -> ActionStatus.BIND_NO_TARGET;
            case INVALID_FREQUENCY -> ActionStatus.BIND_INVALID_FREQ;
            case NO_GRID, NO_SERVICE -> ActionStatus.BIND_NO_GRID;
        };
        // 立即回读一次真实状态：否则界面要等下一个轮询周期才更新，
        // 玩家会把"绑定失败/被拒绝"误读成绑定成功。
        updateItemInfo();
    }

    /**
     * 批量绑定：payload = "频段;位置1;位置2;…"，位置用 {@code x|y|z|side|dim}。
     *
     * <p>逐台调用 {@code bindTo}：角色仲裁在 P2PService 层面完成，
     * 第一台会成为该频段的输入端，其余成为输出端。
     */
    private void handleBindFrequencyBatch(String data) {
        if (p2pManager == null) return;
        String[] parts = data.split(";");
        if (parts.length < 2 || parts[0].isEmpty()) {
            statusCode = ActionStatus.BIND_NO_TARGET;
            statusParam = "";
            return;
        }
        String frequency = parts[0];
        int ok = 0;
        int fail = 0;
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            P2PTunnelPart<?> p2p = parsingP2P(parts[i]);
            if (p2p == null) {
                fail++;
                continue;
            }
            if (p2pManager.bindTo(p2p, frequency) == P2PManager.BindResult.OK) {
                ok++;
            } else {
                fail++;
            }
        }
        statusCode = fail == 0 ? ActionStatus.BIND_BATCH_OK : ActionStatus.BIND_BATCH_PARTIAL;
        statusParam = fail == 0
                ? (ok + "|" + frequency)
                : (ok + "|" + fail + "|" + frequency);
        updateItemInfo();
    }

    // 发送时传递复合参数（格式：frequency|alias）
    private void handleSetChannelAlias(String data) {
        setChannelAlias(data);
    }

    // 接收时解析并重新获取 P2P 部件（格式：x|y|z|side::alias）
    private void handleSetP2PAlias(String data) {
        setP2PAlias(data);
    }

    // 接收时解析 P2P 位置字符串并重新获取 P2P 部件（格式：x|y|z|side）
    private void handleSetPendingBind(String positionData) {
        P2PTunnelPart<?> p2p = parsingP2P(positionData);
        setPendingBind(p2p);
    }

    /** 把配对源退回“打开界面时右键的那一台”，而不是清成一个空指针。 */
    private void handleClearPendingBind() {
        if (p2pManager == null) return;
        p2pManager.setPendingBindTarget(p2pManager.getP2PTunnelPart());
        statusCode = ActionStatus.NONE;
        statusParam = "";
        sendSyncPacketToClient();
    }
    private void handleAutoConfigIO() {
        autoConfigIO();
    }

    private void handleAssignFreq() {
        assignFreq();
    }

    /**
     * 把选中的输出端设备改成它所在频段的输入端：原输入端会被降级为输出。
     *
     * <p>顺序由 {@code P2PManager.switchToInput} 保证（先降级、再提升），这里只负责
     * 把结果码翻成状态行文案，并立刻重扫一次 —— 角色变了，树里的输入/输出标记
     * 与频段汇总都依赖这次重扫，否则要等下一个轮询周期才更新。
     */
    private void handleSwitchToInput(String positionData) {
        if (p2pManager == null) return;
        P2PTunnelPart<?> p2p = parsingP2P(positionData);
        P2PManager.RoleResult result = p2pManager.switchToInput(p2p);
        statusParam = p2p == null ? "" : String.format("%04X", p2p.getFrequency() & 0xFFFF);
        statusCode = switch (result) {
            case OK -> ActionStatus.SWITCHED_TO_INPUT;
            case NO_CHANNEL -> ActionStatus.SWITCH_NO_CHANNEL;
            case ALREADY_INPUT -> ActionStatus.SWITCH_ALREADY_INPUT;
            case NO_TARGET, NO_GRID, NO_SERVICE -> ActionStatus.SWITCH_FAILED;
        };
        p2pManager.analysisP2P();
        updateItemInfo();
    }

    // 接收时解析 P2P 位置字符串并重新获取 P2P 部件（格式：x|y|z|side）
    private void handleHighlightP2P(String positionData) {
        P2PTunnelPart<?> p2pPart = parsingP2P(positionData);
        highlightP2P(p2pPart);
    }

    private void handleHighlightP2PTunnel(String frequencyHex) {
        highlightP2PTunnel(frequencyHex);
    }

    // 接收时解析 P2P 位置字符串并重新获取 P2P 部件（格式：x|y|z|side|dim）
    private void handleTeleportP2P(String positionData) {
        if (!ModConfigs.isTeleportAllowed()) {
            // 按钮只在允许时显示，但包可以伪造：服务端必须自己再判一次。
            return;
        }
        teleportTo(parsingP2P(positionData));
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
                        p2p.getLevel().dimension().location().toString()
                );
                positionDevicesMap.put(position, entry.getValue());
            }

            for (var entry : p2pInfoMap.entrySet()) {
                P2PTunnelPart<?> p2p = entry.getKey();
                P2PPosition position = new P2PPosition(
                        p2p.getBlockEntity().getBlockPos(),
                        p2p.getSide(),
                        p2p.getLevel().dimension().location().toString()
                );
                positionInfoMap.put(position, entry.getValue());
            }

            ConfigModeSyncPacket packet = new ConfigModeSyncPacket(
                    p2pFrequencyAndAlias,
                    positionDevicesMap,
                    positionInfoMap,
                    channelInfoMap,
                    p2pTypeInfoMap,
                    resolveBindTargetPosition(),
                    statusCode,
                    statusParam
            );

            NetworkHandler.sendToPlayer(packet, getPlayer());
        }
    }

    /** 把当前配对源设备换算成位置键；设备所在区块未加载时返回 null。 */
    private P2PPosition resolveBindTargetPosition() {
        if (p2pManager == null) return null;
        P2PTunnelPart<?> target = p2pManager.getBindTarget();
        if (target == null || target.getBlockEntity() == null) return null;
        var host = target.getBlockEntity();
        if (host.getLevel() == null) return null;
        return new P2PPosition(
                host.getBlockPos(),
                target.getSide(),
                host.getLevel().dimension().location().toString());
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

            this.syncVersion++;
            this.clientStatusCode = packet.getStatusCode();
            this.clientStatusParam = packet.getStatusParam();
            this.clientPendingBindPosition = packet.getPendingBindPosition();

            // 配对源直接按位置键查表：比“遍历找 isPendingBind”更可靠，
            // 而且切换配对源时服务端只需重发包，不必重跑全网格扫描。
            this.clientPendingBindP2PInfo = this.clientPendingBindPosition == null
                    ? null
                    : clientP2PInfoMap.get(this.clientPendingBindPosition);
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
        HashMap<String, String> managerFrequencyAlias = p2pManager.getP2PFrequencyAndAlias();
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
            boolean isPendingBind = Objects.equals(p2p, p2pManager.getBindTarget());
            int channel = isMEP2P ? p2p.getExternalFacingNode().getUsedChannels() : 0;
            int maxChannel = isMEP2P ? p2p.getExternalFacingNode().getMaxChannels() : 0;
            short rawFrequency = p2p.getFrequency();
            String frequency = String.format("%04X", rawFrequency & 0xFFFF);
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
        HashMap<String, ArrayList<P2PInfo>> frequencyGrouped = new HashMap<>();
        for (P2PInfo info : p2pInfoMap.values()) {
            frequencyGrouped.computeIfAbsent(info.frequency(), k -> new ArrayList<>()).add(info);
        }

        for (HashMap.Entry<String, ArrayList<P2PInfo>> entry : frequencyGrouped.entrySet()) {
            String frequency = entry.getKey();
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

    private @NotNull ChannelInfo getChannelInfo(Map.Entry<String, ArrayList<P2PInfo>> entry, String frequency) {
        ArrayList<P2PInfo> p2pList = entry.getValue();

        // 获取该频段的别名（即该频段中输入端 P2P 的名称）
        String alias;
        if ("0000".equals(frequency)) {
            alias = "unconnected";
        } else {
            // 查找该频段中第一个输入端 P2P 的名称作为别名
            String inputName = null;
            for (P2PInfo info : p2pList) {
                if (!info.isOutput()) {
                    inputName = info.name();
                    if (inputName != null && !inputName.isEmpty()) {
                        break;
                    }
                }
            }
            alias = (inputName != null && !inputName.isEmpty()) ? inputName : frequency;
        }

        int usedChannels = 0;
        int totalChannels = 0;

        for (P2PInfo info : p2pList) {
            if (info.isMEP2P() && !info.isOutput()) {
                 usedChannels = info.channel();
                 totalChannels = info.maxChannel();
                 break;
            }
        }
        int channelRemaining = totalChannels - usedChannels;

        // 获取 P2P 类型（取第一个设备的类型）
        String p2pType = p2pList.isEmpty() ? "unknown" : p2pList.get(0).p2pType();

        return new ChannelInfo(frequency, alias, p2pList.size(),
                totalChannels, channelRemaining, p2pType, p2pList);
    }

    // 接收时解析
    private void setChannelAlias(String data) {
        // 限制切分次数：名称里出现分隔符时不要把后半段截断
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) {
            LOGGER.warn("Malformed channel alias payload: {}", data);
            return;
        }
        String frequency = parts[0];
        String alias = parts[1];

        // 调用实际逻辑
        setChannelAlias(frequency, alias);
    }

    /**
     * 设置频段别名：将该频段中第一个输入端 P2P 的名称改为指定名称。
     * 频段别名现在指向该频段中输入端 P2P 设备的名称。
     */
    private void setChannelAlias(String frequency, String newName) {
        if (p2pManager != null) {
            p2pManager.setFrequencyAlias(newName, frequency);
            // 同 P2P 改名：重扫一次，别名立刻反映到树与检查器
            p2pManager.analysisP2P();
            updateItemInfo();
        }
    }

    // 接收时解析并重新获取 P2P 部件
    private void setP2PAlias(String data) {
        // 限制切分次数：名称里出现 "::" 时不要把后半段截断
        String[] parts = data.split("::", 2);
        if (parts.length < 2) {
            LOGGER.warn("Malformed P2P alias payload: {}", data);
            return;
        }
        String positionData = parts[0];
        String alias = parts[1];

        P2PTunnelPart<?> p2p = parsingP2P(positionData);
        setP2PAlias(p2p, alias);
    }

    private void setP2PAlias(P2PTunnelPart<?> p2p, String alias) {
        if (p2pManager == null) {
            return;
        }
        if (p2p == null) {
            // 不要静默失败：解析不到部件时界面只会"看起来没反应"，日志里至少要留证据
            LOGGER.warn("Cannot resolve P2P part for rename to '{}'", alias);
            return;
        }
        p2pManager.renameP2P(p2p, alias);
        // 回读校验：改名失败以前是彻底静默的（界面被乐观值掩盖，重开界面才发现名字没变）
        Component applied = p2p.getCustomName();
        boolean ok = applied != null && alias.equals(applied.getString());
        if (!ok) {
            LOGGER.warn("Rename did not apply to P2P at {}: expected '{}', got '{}'",
                    p2p.getBlockEntity() != null ? p2p.getBlockEntity().getBlockPos() : null,
                    alias,
                    applied != null ? applied.getString() : null);
        }
        statusCode = ok ? ActionStatus.RENAMED : ActionStatus.RENAME_FAILED;
        statusParam = ok ? alias : "";
        // 改完名重扫一次：树与检查器立刻拿到新名字，不用等下一轮轮询
        p2pManager.analysisP2P();
        updateItemInfo();
    }

    /**
     * 解析位置数据字符串，取出对应的 P2P 部件。
     * 支持两种格式：
     *   - 旧格式（向后兼容）: x|y|z|side
     *   - 新格式:            x|y|z|side|dimension （dimension 为完整 ResourceLocation，如 minecraft:overworld）
     *
     * <p>优先在本菜单已经扫描到的 {@link #p2pDevicesMap} 里匹配：那些就是网格里正在运行的
     * 部件实例，坐标/朝向/维度都来自服务端自己的扫描结果，不受"区块是否加载""getLevel() 是否
     * 拿得到目标维度"影响。只有匹配不到时才退回按坐标去世界里查 —— 那条路在跨维度、
     * 区块未加载等情况下会安静地返回 null，让调用方以为操作成功了。
     */
    private P2PTunnelPart<?> parsingP2P(String data) {
        String[] posParts = data.split("\\|");
        BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]),
                Integer.parseInt(posParts[1]),
                Integer.parseInt(posParts[2]));
        Direction side = Direction.values()[Integer.parseInt(posParts[3])];
        String dimension = posParts.length >= 5 ? posParts[4] : null;

        // 1. 先在本次扫描到的部件里找
        for (P2PTunnelPart<?> candidate : p2pDevicesMap.keySet()) {
            if (candidate == null || candidate.getSide() != side) {
                continue;
            }
            BlockEntity host = candidate.getBlockEntity();
            if (host == null || host.getLevel() == null || !host.getBlockPos().equals(pos)) {
                continue;
            }
            if (dimension != null
                    && !host.getLevel().dimension().location().toString().equals(dimension)) {
                continue;
            }
            return candidate;
        }

        // 2. 回退：按坐标去世界里查
        Player player = getPlayer();

        // 确定目标维度：优先使用编码中的维度，否则回退到玩家当前维度（向后兼容旧格式）
        Level targetLevel;
        if (posParts.length >= 5) {
            String dimStr = posParts[4];
            ResourceLocation dimLocation = ResourceLocation.parse(dimStr);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            targetLevel = player.getServer() != null ? player.getServer().getLevel(dimKey) : null;
            if (targetLevel == null) {
                // 目标维度不存在，回退到玩家维度
                targetLevel = player.level();
            }
        } else {
            // 旧格式无维度信息，使用玩家当前维度
            targetLevel = player.level();
        }

        BlockEntity blockEntity = targetLevel.getBlockEntity(pos);
        if (blockEntity instanceof IPartHost partHost) {
            IPart part = partHost.getPart(side);
            if (part instanceof P2PTunnelPart<?> p2pPart) {
                return p2pPart;
            }
        }
        return null;
    }

    private void setPendingBind(P2PTunnelPart<?> p2p) {
        if (p2pManager == null) return;
        if (p2p == null) {
            statusCode = ActionStatus.BIND_NO_TARGET;
            statusParam = "";
        } else {
            // 只改配对源，不动扫描锚点：否则“选一台设备”会顺带切掉整个网络视图
            p2pManager.setPendingBindTarget(p2p);
            Component custom = p2p.getCustomName();
            String name = custom == null ? "" : custom.getString();
            if (name.isEmpty() && p2p.getBlockEntity() != null) {
                name = p2p.getBlockEntity().getBlockPos().toShortString();
            }
            statusCode = ActionStatus.SOURCE_SELECTED;
            statusParam = name;
        }
        // 只是换配对源，没必要重跑 analysisP2P：把现有缓存重发一次即可
        sendSyncPacketToClient();
    }

    private void autoConfigIO() {
        p2pManager.autoConfigP2PIO();
    }

    private void assignFreq() {
        if (p2pManager == null) {
            return;
        }
        p2pManager.assignNewFrequency();
        P2PTunnelPart<?> target = p2pManager.getBindTarget();
        if (target != null) {
            statusCode = ActionStatus.ASSIGNED;
            statusParam = String.format("%04X", target.getFrequency() & 0xFFFF);
        }
        updateItemInfo();
    }

    private void highlightP2P(P2PTunnelPart<?> p2pPart) {
        // 根据服务端配置决定是否在聊天栏输出 P2P 位置信息
        if (!ModConfigs.getServerConfig().sendHighlightToChat.get()) {
            return;
        }

        if (p2pPart == null) {
            return;
        }

        Player player = getPlayer();
        BlockPos pos = p2pPart.getBlockEntity().getBlockPos();
        ResourceKey<Level> dimension = p2pPart.getBlockEntity().getLevel().dimension();
        String p2pName = p2pPart.getCustomName() != null ? p2pPart.getCustomName().getString() : "P2P";
        String dimId = dimension.location().toString();
        String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

        // 只报坐标，不再给可点击的传送链接：传送改由界面上的独立按钮（服务端执行）负责。
        player.sendSystemMessage(TranslateHelper.Chat.locationInfo(p2pName, dimId, coords));
    }

    private void highlightP2PTunnel(String frequencyHex) {
        // 根据服务端配置决定是否在聊天栏输出该频段下所有输入端 P2P 的位置信息
        if (!ModConfigs.getServerConfig().sendHighlightToChat.get()) {
            return;
        }

        Player player = getPlayer();
        if (player == null) return;

        ChannelInfo channelInfo = channelInfoMap.get(frequencyHex);
        if (channelInfo == null) return;

        ArrayList<P2PInfo> p2pList = channelInfo.p2pInfoList();
        if (p2pList == null || p2pList.isEmpty()) return;

        // 获取频段别名
        String freqAlias = getFrequencyAlias(frequencyHex);
        String freqDisplay = freqAlias.equals("frequency " + frequencyHex) ? frequencyHex : freqAlias + " (" + frequencyHex + ")";

        player.sendSystemMessage(TranslateHelper.Chat.freqHighlightHeader(freqDisplay));

        // 输入端与输出端都列出来：一条频段是「输入 → 输出」的配对关系，只报输入端时
        // 玩家看不到频道最终流向了哪几台设备。分两段输出，顺序与绑定方向一致。
        //
        // 角色标记传的是翻译键而不是已经翻译好的字符串：这段代码跑在服务端，服务端没有
        // 客户端语言环境，Component.getString() 只会拿回键名本身。把 TranslatableComponent
        // 作为参数塞进聊天消息，客户端收到后再按自己的语言显示。
        int index = 1;
        index = sendChannelMembers(player, p2pList, false, TranslateHelper.Keys.TREE_ROLE_INPUT, index);
        sendChannelMembers(player, p2pList, true, TranslateHelper.Keys.TREE_ROLE_OUTPUT, index);
    }

    /**
     * 输出频段中某一角色的成员位置。
     *
     * @param outputs true 只发输出端，false 只发输入端
     * @param roleKey 行首角色标记的翻译键（在客户端解析）
     * @param index   起始序号，返回下一个可用序号
     */
    private int sendChannelMembers(Player player, ArrayList<P2PInfo> p2pList, boolean outputs,
                                   String roleKey, int index) {
        for (P2PInfo info : p2pList) {
            if (info.isOutput() != outputs) continue;

            BlockPos pos = info.position();
            String dimId = info.dimension().location().toString();
            String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            String p2pName = info.name().isEmpty() ? info.toShortString() : info.name();

            MutableComponent label = Component.literal("[" + index + "] ")
                    .append(TranslateHelper.translate(roleKey))
                    .append(Component.literal(" " + p2pName));

            player.sendSystemMessage(TranslateHelper.Chat.locationInfo(label, dimId, coords));
            index++;
        }
        return index;
    }

    /**
     * 把玩家传送到目标 P2P 部件所在位置。
     *
     * <p>落点选择：部件所在的宿主方块如果没有碰撞箱（线缆、总线一类），直接落在方块中心；
     * 宿主是实心机器块时落到它的顶面 —— 否则玩家会被闷在方块里持续窒息。
     *
     * <p>跨维度也走这里：{@code teleportTo(ServerLevel, …)} 会自己处理维度切换与区块加载。
     */
    private void teleportTo(P2PTunnelPart<?> p2pPart) {
        if (p2pPart == null || p2pPart.getBlockEntity() == null) return;
        if (!(getPlayer() instanceof ServerPlayer player)) return;

        Level level = p2pPart.getBlockEntity().getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = p2pPart.getBlockEntity().getBlockPos();
        boolean collisionFree = serverLevel.getBlockState(pos)
                .getCollisionShape(serverLevel, pos)
                .isEmpty();
        double x = pos.getX() + 0.5;
        double y = collisionFree ? pos.getY() + 0.5 : pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        player.teleportTo(serverLevel, x, y, z, player.getYRot(), player.getXRot());
    }

    /**
     * 获取频段的别名（即该频段中第一个输入端 P2P 的名称）。
     */
    private String getFrequencyAlias(String frequency) {
        if (p2pManager != null) {
            String alias = p2pManager.getFrequencyAlias(frequency);
            if (!alias.equals(frequency)) {
                return alias;
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

    public HashMap<String, ChannelInfo> getClientChannelInfoMap() {
        return clientChannelInfoMap;
    }

    public HashMap<String, P2PTypeInfo> getClientP2PTypeInfoMap() {
        return clientP2PTypeInfoMap;
    }

    public HashMap<String, String> getClientP2PFrequencyAndAlias() {
        return clientP2PFrequencyAndAlias;
    }

    /**
     * 获取客户端缓存中当前待绑定（isPendingBind=true）的 P2P 信息。
     * 对应服务端 P2PManager.getP2PTunnelPart() 所指向的那个 P2P 设备。
     * 返回 null 表示当前没有待绑定的 P2P。
     */
    public P2PInfo getClientPendingBindP2PInfo() {
        return clientPendingBindP2PInfo;
    }

    /** 当前配对源设备的位置键，未选中时为 null。 */
    public P2PPosition getClientPendingBindPosition() {
        return clientPendingBindPosition;
    }

    /** 最近一次操作的结果码，见 {@link ActionStatus}。 */
    public int getClientStatusCode() {
        return clientStatusCode;
    }

    public String getClientStatusParam() {
        return clientStatusParam;
    }

    public int getSyncVersion() {
        return syncVersion;
    }

    /**
     * 供 Screen 调用的客户端 action 分发方法。
     * 将 action 从客户端发送到服务端，服务端收到后会触发对应的 registerClientAction 回调。
     */
    public void dispatchClientAction(String action) {
        sendClientAction(action);
    }

    public <T> void dispatchClientAction(String action, T param) {
        sendClientAction(action, param);
    }

    // 在类内部添加（例如靠近字段列表或方法末尾）
    public void setP2PManager(P2PManager manager) {
        this.p2pManager = manager;
        // 立刻更新一次信息（在服务端构造时会触发）
        updateItemInfo();
    }
}
