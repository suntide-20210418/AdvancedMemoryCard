package com.suntide_20210418.advancedmemorycard.client.gui.menu;

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
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import com.suntide_20210418.advancedmemorycard.p2p.P2PPosition;
import com.suntide_20210418.advancedmemorycard.p2p.P2PTypeInfo;
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
    private static final String SET_CHANNEL_ALIAS = "set_channel_alias";
    private static final String SET_P2P_ALIAS = "set_p2p_alias";
    private static final String SET_PENDING_BIND = "set_pending_bind";
    private static final String AUTO_CONFIG_IO = "auto_config_io";
    private static final String ASSIGN_FREQ = "assign_freq";
    private static final String HIGHLIGHT_P2P = "highlight_p2p";
    private static final String HIGHLIGHT_P2P_TUNNEL = "highlight_p2p_tunnel";
    private static final String UPDATE_ITEM_INFO = "update_item_info";

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

    // 服务端构造
    public ConfigModeMenu(InventoryPlayer playerInventory, P2PManager manager) {
        this.playerInventory = playerInventory;
        this.p2pManager = manager;
        updateItemInfo();
    }

    // 客户端构造（无 P2PManager，仅持有本地缓存）
    public ConfigModeMenu(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
    }

    // ==================== IActionMenu 回调（服务端执行） ====================

    @Override
    public void handleAction(String action, String param) {
        if (p2pManager == null) return;
        switch (action) {
            case REFRESH_P2P:
                p2pManager.analysisP2P();
                updateItemInfo();
                break;
            case UPDATE_ITEM_INFO:
                updateItemInfo();
                break;
            case BIND_FREQUENCY:
                p2pManager.bind(param);
                updateItemInfo();
                break;
            case SET_CHANNEL_ALIAS:
                setChannelAlias(param);
                break;
            case SET_P2P_ALIAS:
                setP2PAlias(param);
                break;
            case SET_PENDING_BIND: {
                PartP2PTunnel p2p = parsingP2P(param);
                setPendingBind(p2p);
                break;
            }
            case AUTO_CONFIG_IO:
                p2pManager.autoConfigP2PIO();
                updateItemInfo();
                break;
            case ASSIGN_FREQ:
                p2pManager.assignNewFrequency();
                updateItemInfo();
                break;
            case HIGHLIGHT_P2P: {
                PartP2PTunnel p2p = parsingP2P(param);
                highlightP2P(p2p);
                break;
            }
            case HIGHLIGHT_P2P_TUNNEL:
                highlightP2PTunnel(param);
                break;
        }
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
            p2pTypeInfoMap);
        NetworkHandler.sendToPlayer(packet, player);
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

        this.clientPendingBindP2PInfo = null;
        for (P2PInfo info : clientP2PInfoMap.values()) {
            if (info.isPendingBind()) {
                this.clientPendingBindP2PInfo = info;
                break;
            }
        }
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
            boolean isPendingBind = Objects.equals(p2p, p2pManager.getP2PTunnelPart());
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
        String[] parts = data.split("\\|");
        String frequency = parts[0];
        String alias = parts[1];
        if (p2pManager != null) {
            p2pManager.setFrequencyAlias(alias, frequency);
            updateItemInfo();
        }
    }

    private void setP2PAlias(String data) {
        String[] parts = data.split("::");
        String positionData = parts[0];
        String alias = parts[1];
        PartP2PTunnel p2p = parsingP2P(positionData);
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

    private void setPendingBind(PartP2PTunnel p2p) {
        p2pManager.setP2PTunnelPart(p2p);
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
}
