package com.suntide_20210418.advancedmemorycard.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeActionPacket;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.network.ServerTaskQueue;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
import com.suntide_20210418.advancedmemorycard.utils.ActionStatus;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import com.suntide_20210418.advancedmemorycard.utils.DimensionHelper;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;

/**
 * 配置模式菜单（服务端容器）。
 * 对应 1.20.1 的 ConfigModeMenu（继承 AEBaseMenu + registerClientAction），
 * 在 1.12.2 中改为继承 vanilla Container，并通过 ConfigModeActionPacket（IActionMenu 回调）
 * 接收客户端操作；服务端构建数据后通过 ConfigModeSyncPacket 同步到客户端本地缓存。
 *
 * 1.7.10 / AE2 rv3 适配要点：
 * - 频段类型为 long（rv3），格式统一为 %016X，与 P2PManager 一致；
 * - getSide() 返回 ForgeDirection（rv3 无 AEPartLocation）；
 * - TileEntity 坐标用 xCoord/yCoord/zCoord，世界用 getWorldObj()；
 * - 自定义名 API 为 hasCustomName()/getCustomName()；
 * - 文本组件用 ChatComponentText / IChatComponent / ChatStyle / EnumChatFormatting；
 * - 移除 EnumHand，统一使用 player.getHeldItem()。
 */
public class ConfigModeMenu extends Container implements ConfigModeActionPacket.IActionMenu {

    // action 标识符
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

    /** 背包槽位的屏幕外坐标：既让 getSlotFromInventory 能命中，又不会被 GuiContainer 绘制。 */
    private static final int OFFSCREEN_SLOT = -10000;

    private final InventoryPlayer playerInventory;
    private P2PManager p2pManager;

    // 服务端数据
    private final HashMap<String, String> p2pFrequencyAndAlias = new HashMap<>();
    private final HashMap<PartP2PTunnel, String> p2pDevicesMap = new HashMap<>();
    private final HashMap<PartP2PTunnel, P2PInfo> p2pInfoMap = new HashMap<>();
    private final HashMap<String, ChannelInfo> channelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> p2pTypeInfoMap = new HashMap<>();

    // 客户端缓存数据（由 ConfigModeSyncPacket 填充，供 Screen 使用）
    private final HashMap<String, String> clientP2PFrequencyAndAlias = new HashMap<>();
    private final HashMap<P2PPosition, String> clientP2PDevicesMap = new HashMap<>();
    private final HashMap<P2PPosition, P2PInfo> clientP2PInfoMap = new HashMap<>();
    private final HashMap<String, ChannelInfo> clientChannelInfoMap = new HashMap<>();
    private final HashMap<String, P2PTypeInfo> clientP2PTypeInfoMap = new HashMap<>();

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

    // 服务端构造
    public ConfigModeMenu(InventoryPlayer playerInventory, P2PManager manager) {
        this.playerInventory = playerInventory;
        this.p2pManager = manager;
        bindOffscreenPlayerSlots(playerInventory);
        updateItemInfo();
    }

    // 客户端构造（无 P2PManager，仅持有本地缓存）
    public ConfigModeMenu(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
        bindOffscreenPlayerSlots(playerInventory);
    }

    /**
     * 把玩家背包槽位挂到容器上，但坐标放在屏幕外（不会被绘制）。
     *
     * <p>
     * 这不是装饰：1.7.10 的 {@code NetHandlerPlayServer#processPlayerBlockPlacement} 末尾会执行
     * {@code openContainer.getSlotFromInventory(player.inventory, currentItem).slotNumber}，
     * 而对零槽位容器该方法返回 null，随后直接 NPE 崩服。任何在本模组 GUI 打开期间
     * （或容器未被正确关闭时）发生的方块右键都会踩到它。
     */
    private void bindOffscreenPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(inventory, col + row * 9 + 9, OFFSCREEN_SLOT, OFFSCREEN_SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(inventory, col, OFFSCREEN_SLOT, OFFSCREEN_SLOT));
        }
    }

    // ==================== IActionMenu 回调（服务端执行） ====================

    @Override
    public void handleAction(String action, String param) {
        if (p2pManager == null) return;
        switch (action) {
            case REFRESH_P2P:
                p2pManager.analysisP2P();
                statusCode = ActionStatus.REFRESHED;
                statusParam = "";
                updateItemInfo();
                break;
            case UPDATE_ITEM_INFO:
                updateItemInfo();
                break;
            case BIND_FREQUENCY:
                handleBindFrequency(param);
                break;
            case BIND_FREQUENCY_BATCH:
                handleBindFrequencyBatch(param);
                break;
            case SET_CHANNEL_ALIAS:
                setChannelAlias(param);
                break;
            case SET_P2P_ALIAS:
                setP2PAlias(param);
                break;
            case SET_PENDING_BIND: {
                PartP2PTunnel p2p = resolveP2P(param);
                setPendingBind(p2p);
                break;
            }
            case CLEAR_PENDING_BIND:
                // 配对源退回「打开界面时右键的那一台」，而不是清成空指针
                p2pManager.setPendingBindTarget(p2pManager.getP2PTunnelPart());
                statusCode = ActionStatus.NONE;
                statusParam = "";
                sendSyncPacketToClient();
                break;
            case AUTO_CONFIG_IO:
                p2pManager.autoConfigP2PIO();
                updateItemInfo();
                break;
            case ASSIGN_FREQ:
                p2pManager.assignNewFrequency();
                updateItemInfo();
                break;
            case SWITCH_TO_INPUT:
                handleSwitchToInput(param);
                break;
            case TELEPORT_P2P:
                handleTeleportP2P(param);
                break;
            case HIGHLIGHT_P2P: {
                PartP2PTunnel p2p = resolveP2P(param);
                highlightP2P(p2p);
                break;
            }
            case HIGHLIGHT_P2P_TUNNEL:
                highlightP2PTunnel(param);
                break;
        }
    }

    /**
     * 绑定后立即回读一次真实状态：否则界面要等下一个轮询周期才更新，
     * 玩家会把「绑定失败/被拒绝」误读成绑定成功。
     */
    private void handleBindFrequency(String frequencyHex) {
        P2PManager.BindResult result = p2pManager.bind(frequencyHex);
        statusParam = frequencyHex == null ? "" : frequencyHex;
        if (result == P2PManager.BindResult.OK) {
            statusCode = ActionStatus.BIND_OK;
        } else if (result == P2PManager.BindResult.NO_TARGET) {
            statusCode = ActionStatus.BIND_NO_TARGET;
        } else if (result == P2PManager.BindResult.INVALID_FREQUENCY) {
            statusCode = ActionStatus.BIND_INVALID_FREQ;
        } else {
            statusCode = ActionStatus.BIND_NO_GRID;
        }
        updateItemInfo();
    }

    /**
     * 批量绑定：payload = "频段;位置1;位置2;…"。
     *
     * <p>
     * 逐台调用 {@code bindTo}：角色仲裁在 P2PCache 层面完成，第一台成为输入端，其余成为输出端。
     */
    private void handleBindFrequencyBatch(String data) {
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
            PartP2PTunnel p2p = parsingP2P(parts[i]);
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
        statusParam = fail == 0 ? (ok + "|" + frequency) : (ok + "|" + fail + "|" + frequency);
        updateItemInfo();
    }

    /**
     * 把选中的输出端设备改成它所在频段的输入端：原输入端会被降级为输出。
     *
     * <p>
     * 顺序由 {@code P2PManager.switchToInput} 保证（先降级、再提升），这里立刻重扫一次。
     */
    private void handleSwitchToInput(String positionData) {
        PartP2PTunnel p2p = resolveP2P(positionData);
        P2PManager.RoleResult result = p2pManager.switchToInput(p2p);
        statusParam = p2p == null ? "" : String.format("%016X", p2p.freq);
        if (result == P2PManager.RoleResult.OK) {
            statusCode = ActionStatus.SWITCHED_TO_INPUT;
        } else if (result == P2PManager.RoleResult.NO_CHANNEL) {
            statusCode = ActionStatus.SWITCH_NO_CHANNEL;
        } else if (result == P2PManager.RoleResult.ALREADY_INPUT) {
            statusCode = ActionStatus.SWITCH_ALREADY_INPUT;
        } else {
            statusCode = ActionStatus.SWITCH_FAILED;
        }
        p2pManager.analysisP2P();
        updateItemInfo();
    }

    private void handleTeleportP2P(String positionData) {
        if (!ModConfigs.isTeleportAllowed()) {
            // 按钮只在允许时显示，但包可以伪造：服务端必须自己再判一次。
            return;
        }
        teleportTo(resolveP2P(positionData));
    }

    /**
     * 把玩家送到 P2P 部件所在位置（支持跨维度）。
     *
     * <p>
     * 1.7.10 的跨维度传送用 {@code travelToDimension}；落点取方块中心，
     * 有碰撞体积时抬高一格。
     */
    private void teleportTo(PartP2PTunnel p2pPart) {
        if (p2pPart == null || p2pPart.getTile() == null) return;
        if (!(playerInventory.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) playerInventory.player;
        TileEntity tile = p2pPart.getTile();
        int targetDim = tile.getWorldObj().provider.dimensionId;
        int x = tile.xCoord;
        int y = tile.yCoord;
        int z = tile.zCoord;

        // 同维度不要走 transferPlayerToDimension：1.7.10 的实现不判断 j == dimension，
        // 会先把玩家 danger-remove 再传回，白跑一遍重生/区块流程，还可能冲掉落点。
        if (player.dimension != targetDim) {
            player.mcServer.getConfigurationManager()
                .transferPlayerToDimension(player, targetDim);
        }
        player.setPositionAndUpdate(x + 0.5D, y + 1.0D, z + 0.5D);
        player.playerNetServerHandler
            .setPlayerLocation(x + 0.5D, y + 1.0D, z + 0.5D, player.rotationYaw, player.rotationPitch);

        // 立刻发一次：同维度时客户端收到包就能画；跨维度时下面 2 秒 / 4 秒的补发兜底。
        if (p2pManager != null) p2pManager.renderP2P(p2pPart);

        // 客户端渲染高亮要等目标区块加载；传送刚完成时可能还没到。2 秒 / 4 秒各补发
        // 一次 HighlightPacket —— 走的是与「高亮」按钮完全相同的路径，不依赖客户端 tick 任务。
        final PartP2PTunnel target = p2pPart;
        ServerTaskQueue.enqueueDelayed(new Runnable() {

            @Override
            public void run() {
                if (p2pManager != null) p2pManager.renderP2P(target);
            }
        }, 40);
        ServerTaskQueue.enqueueDelayed(new Runnable() {

            @Override
            public void run() {
                if (p2pManager != null) p2pManager.renderP2P(target);
            }
        }, 80);
    }

    // ==================== 构建并发送同步数据包 ====================

    private void sendSyncPacketToClient() {
        EntityPlayerMP player = (EntityPlayerMP) playerInventory.player;
        if (player == null) return;

        HashMap<P2PPosition, String> positionDevicesMap = new HashMap<>();
        HashMap<P2PPosition, P2PInfo> positionInfoMap = new HashMap<>();

        for (Map.Entry<PartP2PTunnel, String> entry : p2pDevicesMap.entrySet()) {
            PartP2PTunnel p2p = entry.getKey();
            TileEntity te = p2p.getTile();
            String dim = String.valueOf(te.getWorldObj().provider.dimensionId);
            positionDevicesMap.put(
                new P2PPosition(new BlockPos(te.xCoord, te.yCoord, te.zCoord), p2p.getSide(), dim),
                entry.getValue());
        }

        for (Map.Entry<PartP2PTunnel, P2PInfo> entry : p2pInfoMap.entrySet()) {
            PartP2PTunnel p2p = entry.getKey();
            TileEntity te = p2p.getTile();
            String dim = String.valueOf(te.getWorldObj().provider.dimensionId);
            positionInfoMap.put(
                new P2PPosition(new BlockPos(te.xCoord, te.yCoord, te.zCoord), p2p.getSide(), dim),
                entry.getValue());
        }

        ConfigModeSyncPacket packet = new ConfigModeSyncPacket(
            p2pFrequencyAndAlias,
            positionDevicesMap,
            positionInfoMap,
            channelInfoMap,
            p2pTypeInfoMap,
            resolveBindTargetPosition(),
            statusCode,
            statusParam);
        NetworkHandler.sendToPlayer(packet, player);
    }

    /** 把当前配对源设备换算成位置键；设备所在区块未加载时返回 null。 */
    private P2PPosition resolveBindTargetPosition() {
        if (p2pManager == null) return null;
        PartP2PTunnel target = p2pManager.getBindTarget();
        if (target == null || target.getTile() == null) return null;
        TileEntity te = target.getTile();
        return new P2PPosition(
            new BlockPos(te.xCoord, te.yCoord, te.zCoord),
            target.getSide(),
            String.valueOf(te.getWorldObj().provider.dimensionId));
    }

    // ==================== 客户端接收同步数据 ====================

    public void receiveSyncData(ConfigModeSyncPacket packet) {
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

        // 配对源直接按位置键查表，比「遍历找 isPendingBind」更可靠
        this.clientPendingBindP2PInfo = this.clientPendingBindPosition == null ? null
            : clientP2PInfoMap.get(this.clientPendingBindPosition);
    }

    public void updateItemInfo() {
        if (p2pManager == null) return;

        p2pManager.analysisP2P();

        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        p2pInfoMap.clear();
        channelInfoMap.clear();
        p2pTypeInfoMap.clear();

        HashMap<String, String> managerFrequencyAlias = p2pManager.getP2PFrequencyAndAlias();
        HashMap<PartP2PTunnel, String> managerP2PDevices = p2pManager.getP2PDevicesMap();

        p2pFrequencyAndAlias.putAll(managerFrequencyAlias);
        p2pDevicesMap.putAll(managerP2PDevices);

        for (Map.Entry<PartP2PTunnel, String> entry : p2pDevicesMap.entrySet()) {
            PartP2PTunnel p2p = entry.getKey();
            String p2pType = entry.getValue();

            boolean isActive = p2p.isActive();
            boolean isOutput = p2p.isOutput();
            boolean isConnected = p2pManager.isConnected(p2p);
            boolean isMEP2P = p2p instanceof PartP2PTunnelME;
            boolean isPendingBind = Objects.equals(p2p, p2pManager.getBindTarget());
            int channel = 0;
            int maxChannel = 0;
            if (isMEP2P) {
                // ME P2P 通过 outerProxy 连接外部网络，外部节点的 usedChannels() 即隧道承载的频道数。
                // ME P2P 的 outerProxy 使用 DENSE_CAPACITY，外部网络最多承载 32 频道。
                channel = getMEP2PExternalUsedChannels((PartP2PTunnelME) p2p);
                maxChannel = 32;
            }
            long rawFrequency = p2p.getFrequency();
            String frequency = String.format("%016X", rawFrequency);
            String name = p2p.hasCustomName() ? p2p.getCustomName() : "";
            String p2pTypeName = p2pType != null ? p2pType : "unknown";
            TileEntity te = p2p.getTile();
            BlockPos pos = new BlockPos(te.xCoord, te.yCoord, te.zCoord);
            ForgeDirection side = p2p.getSide();
            String dimension = te.getWorldObj().provider.getDimensionName();
            int dimensionId = te.getWorldObj().provider.dimensionId;

            P2PInfo info = new P2PInfo(
                isActive,
                isOutput,
                isConnected,
                isMEP2P,
                isPendingBind,
                channel,
                maxChannel,
                frequency,
                name,
                p2pTypeName,
                dimension,
                dimensionId,
                pos,
                side);
            p2pInfoMap.put(p2p, info);
        }

        // 按 (类型, 频率) 两级分组构建。
        // 关键：AE2 中不同 P2P 类型可以共用同一个频率数值（隧道连接按类型隔离），
        // 若仅按频率分组会把不同类型合并到同一 channel，导致树形结构把所有 P2P 错误归入某一类型节点。
        HashMap<String, HashMap<String, ArrayList<P2PInfo>>> typeFreqGrouped = new HashMap<>();
        for (P2PInfo info : p2pInfoMap.values()) {
            typeFreqGrouped.computeIfAbsent(info.p2pType, k -> new HashMap<>())
                .computeIfAbsent(info.frequency, k -> new ArrayList<>())
                .add(info);
        }

        // 构建 channelInfoMap，key 改为 frequency|p2pType，保证每个 channel 仅含单一类型
        channelInfoMap.clear();
        for (Map.Entry<String, HashMap<String, ArrayList<P2PInfo>>> typeEntry : typeFreqGrouped.entrySet()) {
            for (Map.Entry<String, ArrayList<P2PInfo>> freqEntry : typeEntry.getValue()
                .entrySet()) {
                String frequency = freqEntry.getKey();
                ChannelInfo channelInfo = getChannelInfo(freqEntry, frequency);
                channelInfoMap.put(frequency + "|" + typeEntry.getKey(), channelInfo);
            }
        }

        // 按 P2P 类型分组构建 p2pTypeInfoMap（每个类型下仅含该类型的 channel 与 p2p）
        for (Map.Entry<String, HashMap<String, ArrayList<P2PInfo>>> typeEntry : typeFreqGrouped.entrySet()) {
            String p2pType = typeEntry.getKey();
            ArrayList<ChannelInfo> chList = new ArrayList<>();
            ArrayList<P2PInfo> p2pList = new ArrayList<>();
            for (Map.Entry<String, ArrayList<P2PInfo>> freqEntry : typeEntry.getValue()
                .entrySet()) {
                chList.add(getChannelInfo(freqEntry, freqEntry.getKey()));
                p2pList.addAll(freqEntry.getValue());
            }
            P2PTypeInfo typeInfo = new P2PTypeInfo(p2pType, chList.size(), p2pList.size(), chList, p2pList);
            p2pTypeInfoMap.put(p2pType, typeInfo);
        }

        sendSyncPacketToClient();
    }

    private ChannelInfo getChannelInfo(Map.Entry<String, ArrayList<P2PInfo>> entry, String frequency) {
        ArrayList<P2PInfo> p2pList = entry.getValue();

        String alias;
        if ("0000000000000000".equals(frequency)) {
            alias = "unconnected";
        } else {
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

        String p2pType = p2pList.isEmpty() ? "unknown"
            : p2pList.get(0)
                .p2pType();

        return new ChannelInfo(frequency, alias, p2pList.size(), totalChannels, channelRemaining, p2pType, p2pList);
    }

    // ==================== 操作实现 ====================

    private void setChannelAlias(String data) {
        // 限制为 2 段：别名本身可能包含分隔符
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) {
            return;
        }
        String frequency = parts[0];
        String alias = parts[1];
        if (p2pManager != null) {
            p2pManager.setFrequencyAlias(alias, frequency);
            updateItemInfo();
        }
    }

    private void setP2PAlias(String data) {
        String[] parts = data.split("::", 2);
        if (parts.length < 2) {
            return;
        }
        String positionData = parts[0];
        String alias = parts[1];
        PartP2PTunnel p2p = resolveP2P(positionData);
        if (p2p != null && p2pManager != null) {
            p2pManager.renameP2P(p2p, alias);
            updateItemInfo();
        }
    }

    /**
     * 解析位置数据字符串，从对应维度的世界中重新获取 P2P 部件。
     * 格式：x|y|z|sideOrdinal|dimension（dimension 为 int 维度 ID 字符串）
     */
    private PartP2PTunnel parsingP2P(String data) {
        String[] posParts = data.split("\\|");
        BlockPos pos = new BlockPos(
            Integer.parseInt(posParts[0]),
            Integer.parseInt(posParts[1]),
            Integer.parseInt(posParts[2]));
        ForgeDirection side = ForgeDirection.values()[Integer.parseInt(posParts[3])];

        EntityPlayer player = playerInventory.player;

        World targetWorld;
        if (posParts.length >= 5) {
            int dimId = Integer.parseInt(posParts[4]);
            targetWorld = DimensionManager.getWorld(dimId);
            if (targetWorld == null) {
                targetWorld = player.worldObj;
            }
        } else {
            targetWorld = player.worldObj;
        }

        TileEntity blockEntity = targetWorld.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        if (blockEntity instanceof IPartHost) {
            IPart part = ((IPartHost) blockEntity).getPart(side);
            if (part instanceof PartP2PTunnel) {
                return (PartP2PTunnel) part;
            }
        }
        return null;
    }

    /**
     * 位置串 → 部件。先走世界侧 {@code getTileEntity}；区块未加载或朝向索引对不上时，
     * 退回已分析部件表。改名早就这样兜底了，传送 / 高亮 / 切换输入端同理 ——
     * 否则这些操作会静默失败，玩家只看到按钮没反应。
     */
    private PartP2PTunnel resolveP2P(String data) {
        PartP2PTunnel p2p = parsingP2P(data);
        if (p2p == null && p2pManager != null) {
            p2p = p2pManager.findP2PByPositionString(data);
        }
        return p2p;
    }

    private void setPendingBind(PartP2PTunnel p2p) {
        if (p2p == null) {
            statusCode = ActionStatus.BIND_NO_TARGET;
            statusParam = "";
        } else {
            // 只改配对源，不动扫描锚点：否则「选一台设备」会顺带切掉整个网络视图
            p2pManager.setPendingBindTarget(p2p);
            String name = p2p.hasCustomName() ? p2p.getCustomName() : "";
            if (name.isEmpty() && p2p.getTile() != null) {
                name = p2p.getTile().xCoord + ", " + p2p.getTile().yCoord + ", " + p2p.getTile().zCoord;
            }
            statusCode = ActionStatus.SOURCE_SELECTED;
            statusParam = name;
        }
        // 只是换配对源，没必要重跑 analysisP2P：把现有缓存重发一次即可
        sendSyncPacketToClient();
    }

    private void highlightP2P(PartP2PTunnel p2pPart) {
        p2pManager.renderP2P(p2pPart);

        if (!ModConfigs.getServerConfig().sendHighlightToChat) {
            return;
        }
        if (p2pPart == null) return;

        EntityPlayer player = playerInventory.player;
        TileEntity te = p2pPart.getTile();
        BlockPos pos = new BlockPos(te.xCoord, te.yCoord, te.zCoord);
        int dimId = te.getWorldObj().provider.dimensionId;
        String dimName = DimensionHelper.getDimensionName(dimId);
        String p2pName = p2pPart.hasCustomName() ? p2pPart.getCustomName() : "P2P";
        String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

        String locationInfo = TranslateHelper.Chat.locationInfo(p2pName, dimName, coords);
        player.addChatMessage(new ChatComponentText(locationInfo));

        // 另起一行：可点击的传送链接（点击后执行 /amc_tp 传送至该 P2P 的维度与坐标）
        player.addChatMessage(buildTeleportLink(dimId, pos, p2pPart.getSide()));
    }

    private void highlightP2PTunnel(String frequencyHex) {
        p2pManager.renderP2P(frequencyHex);

        if (!ModConfigs.getServerConfig().sendHighlightToChat) {
            return;
        }
        EntityPlayer player = playerInventory.player;
        if (player == null) return;

        ArrayList<P2PInfo> p2pList = new ArrayList<>();
        for (Map.Entry<String, ChannelInfo> e : channelInfoMap.entrySet()) {
            if (e.getKey()
                .startsWith(frequencyHex + "|")) {
                ArrayList<P2PInfo> list = e.getValue().p2pInfoList;
                if (list != null) p2pList.addAll(list);
            }
        }
        if (p2pList.isEmpty()) return;

        String freqAlias = getFrequencyAlias(frequencyHex);
        String freqDisplay = freqAlias.equals("frequency " + frequencyHex) ? frequencyHex
            : freqAlias + " (" + frequencyHex + ")";

        player.addChatMessage(new ChatComponentText(TranslateHelper.Chat.freqHighlightHeader(freqDisplay)));

        int index = 1;
        for (P2PInfo info : p2pList) {
            if (info.isOutput()) continue;

            BlockPos pos = info.position();
            String dimId = info.dimension();
            String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            String p2pName = info.name()
                .isEmpty() ? info.toShortString() : info.name();

            String locationInfo = TranslateHelper.Chat.locationInfo("[" + index + "] " + p2pName, dimId, coords);
            player.addChatMessage(new ChatComponentText(locationInfo));

            // 另起一行：可点击的传送链接（点击后传送至该 P2P 的维度与坐标）
            player.addChatMessage(buildTeleportLink(info.dimensionId(), pos, info.direction()));
            index++;
        }
    }

    /**
     * 构建一个可点击的传送链接文本组件。
     * 点击后执行 /amc_tp 指令，由服务端通过 Forge API 将玩家传送至目标维度与坐标
     * （取 P2P 部件朝向的相邻方块中心，避免卡入方块内部）。
     */
    private IChatComponent buildTeleportLink(int dimId, BlockPos pos, ForgeDirection side) {
        double tx = pos.getX() + 0.5 + side.offsetX;
        double ty = pos.getY() + 0.5 + side.offsetY;
        double tz = pos.getZ() + 0.5 + side.offsetZ;

        String dimName = DimensionHelper.getDimensionName(dimId);
        String displayCoords = (int) Math.floor(tx) + ", " + (int) Math.floor(ty) + ", " + (int) Math.floor(tz);
        String command = "/amc_tp " + dimId + " " + tx + " " + ty + " " + tz;

        IChatComponent link = new ChatComponentText(TranslateHelper.Chat.clickToTeleport());
        ChatStyle style = link.getChatStyle();
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        style.setChatHoverEvent(
            new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(TranslateHelper.Chat.teleportHover(dimName, displayCoords))));
        style.setColor(EnumChatFormatting.AQUA);
        return link;
    }

    private String getFrequencyAlias(String frequency) {
        if (p2pManager != null) {
            String alias = p2pManager.getFrequencyAlias(frequency);
            if (!alias.equals(frequency)) {
                return alias;
            }
        }
        return "frequency " + frequency;
    }

    /**
     * 获取 ME P2P 隧道承载的外部网络频道使用数。
     * ME P2P 通过 outerProxy 连接外部网络，其外部节点（GridNode）的 usedChannels()
     * 即该隧道从外部网络拉取/承载的频道数量。rv3 的 IGridNode 未暴露该方法，故通过反射调用。
     */
    private int getMEP2PExternalUsedChannels(PartP2PTunnelME meP2P) {
        if (meP2P == null) return 0;
        try {
            appeng.api.networking.IGridNode externalNode = meP2P.getExternalFacingNode();
            if (externalNode == null) return 0;
            java.lang.reflect.Method m = externalNode.getClass()
                .getMethod("usedChannels");
            m.setAccessible(true);
            Object result = m.invoke(externalNode);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception e) {
            // 反射失败则回退为 0
        }
        return 0;
    }

    // ==================== Getter（供 Screen 使用） ====================

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

    public P2PInfo getClientPendingBindP2PInfo() {
        return clientPendingBindP2PInfo;
    }

    // ==================== 客户端 action 分发 ====================

    public void dispatchClientAction(String action) {
        NetworkHandler.sendToServer(new ConfigModeActionPacket(action));
    }

    public void dispatchClientAction(String action, String param) {
        NetworkHandler.sendToServer(new ConfigModeActionPacket(action, param));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    /** 客户端缓存中的配对源位置；null 表示未选择。 */
    public P2PPosition getClientPendingBindPosition() {
        return clientPendingBindPosition;
    }

    public int getClientStatusCode() {
        return clientStatusCode;
    }

    public String getClientStatusParam() {
        return clientStatusParam;
    }

    /**
     * 同步版本号：每收到一个同步包 +1。
     * 界面用它做节流，避免每 tick 重建整棵树。
     */
    public int getSyncVersion() {
        return syncVersion;
    }
}
