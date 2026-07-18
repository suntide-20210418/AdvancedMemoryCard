package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.GridAccessException;
import appeng.me.cache.P2PCache;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * P2P 网络分析与管理（AE2 rv3 / 1.7.10 适配）。
 * 将 rv6 的 short 频段替换为 rv3 的 long 频段，setOutput 通过访问转换器公开直接调用，
 * newFrequency 在 rv3 中不存在故自行生成，TileEntity 坐标使用 xCoord/yCoord/zCoord。
 */
public class P2PManager {
    private PartP2PTunnel p2pTunnelPartBinding;
    private IGrid grid;
    private final EntityPlayer player;
    private final HashMap<String, String> p2pFrequencyAndAlias;
    private final HashMap<PartP2PTunnel, String> p2pDevicesMap;
    private P2PCache p2pService;

    public P2PManager(PartP2PTunnel p2pTunnelPartBinding, EntityPlayer player) {
        this.p2pTunnelPartBinding = p2pTunnelPartBinding;
        this.p2pDevicesMap = new HashMap<>();
        this.p2pFrequencyAndAlias = new HashMap<>();
        this.player = player;
        analysisP2P();
        try {
            this.p2pService = p2pTunnelPartBinding.getProxy().getP2P();
        } catch (GridAccessException e) {
            this.p2pService = null;
        }
        // 初始化时立即处理频段为0但仍连接了设备的异常P2P
        fixZeroFrequencyAnomalies();
    }

    public void analysisP2P() {
        IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        if (gridNode == null) {
            return;
        }

        grid = gridNode.getGrid();
        if (grid == null) {
            return;
        }

        for (IGridNode node : grid.getNodes()) {
            IGridHost machine = node.getMachine();
            if (machine instanceof PartP2PTunnel) {
                PartP2PTunnel p2pPart = (PartP2PTunnel) machine;
                p2pDevicesMap.put(p2pPart, getP2PType(p2pPart));
                long rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%016X", rawFreq);
                if (!p2pFrequencyAndAlias.containsValue(frequency)) {
                    p2pFrequencyAndAlias.put(frequency, frequency);
                }
            }
        }
    }

    public void bind(String frequencyHex) {
        if (p2pTunnelPartBinding == null) {
            return;
        }

        analysisP2P();

        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return;
        }

        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return;
        }

        long freq = Long.parseLong(frequencyHex, 16);

        // 非 ME P2P 设备：智能判断输入/输出端角色
        if (!(p2pTunnelPartBinding instanceof PartP2PTunnelME)) {
            PartP2PTunnel existingInput = getInput(freq);
            if (existingInput != null) {
                if (!p2pTunnelPartBinding.isOutput()) {
                    setP2POutput(p2pTunnelPartBinding, true);
                }
            } else {
                if (p2pTunnelPartBinding.isOutput()) {
                    setP2POutput(p2pTunnelPartBinding, false);
                }
            }
        }

        currentP2PService.updateFreq(p2pTunnelPartBinding, freq);

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
    }

    public void autoConfigP2PIO() {
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null || currentGrid.isEmpty()) {
            return;
        }
        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return;
        }

        if (p2pDevicesMap.isEmpty()) {
            analysisP2P();
        }

        List<PartP2PTunnel> p2pParts = new ArrayList<>(p2pDevicesMap.keySet());
        for (PartP2PTunnel p2pPart : p2pParts) {
            if (p2pPart == null) {
                continue;
            }
            processSingleP2PPart(p2pPart, currentP2PService);
        }
    }

    private IGrid getCurrentGrid() {
        if (p2pTunnelPartBinding != null) {
            IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
            if (gridNode != null) {
                IGrid currentGrid = gridNode.getGrid();
                if (currentGrid != null) {
                    this.grid = currentGrid;
                    return currentGrid;
                }
            }
        }
        return grid;
    }

    private P2PCache getP2PService() {
        try {
            return p2pTunnelPartBinding.getProxy().getP2P();
        } catch (GridAccessException e) {
            return null;
        }
    }

    private void fixZeroFrequencyAnomalies() {
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return;
        }
        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return;
        }

        List<PartP2PTunnel> anomalousP2Ps = new ArrayList<>();
        for (PartP2PTunnel p2pPart : p2pDevicesMap.keySet()) {
            if (p2pPart == null) {
                continue;
            }
            if (p2pPart.getFrequency() == 0 && isConnected(p2pPart)) {
                anomalousP2Ps.add(p2pPart);
            }
        }

        if (anomalousP2Ps.isEmpty()) {
            return;
        }

        for (PartP2PTunnel p2pPart : anomalousP2Ps) {
            currentP2PService.updateFreq(p2pPart, 0L);
            setP2POutput(p2pPart, true);
            currentP2PService.updateFreq(p2pPart, 0L);
        }

        refreshP2PDevicesMap();
    }

    private void refreshP2PDevicesMap() {
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        if (grid == null) {
            return;
        }

        for (IGridNode node : grid.getNodes()) {
            IGridHost machine = node.getMachine();
            if (machine instanceof PartP2PTunnel) {
                PartP2PTunnel p2pPart = (PartP2PTunnel) machine;
                p2pDevicesMap.put(p2pPart, getP2PType(p2pPart));
                long rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%016X", rawFreq);
                if (!p2pFrequencyAndAlias.containsValue(frequency)) {
                    p2pFrequencyAndAlias.put(frequency, frequency);
                }
            }
        }
    }

    private void processSingleP2PPart(PartP2PTunnel p2pPart, P2PCache p2pService) {
        if (!(p2pPart instanceof PartP2PTunnelME)) {
            return;
        }

        if (p2pPart.getFrequency() != 0) {
            return;
        }

        PartP2PTunnelME meP2PTunnelPart = (PartP2PTunnelME) p2pPart;
        IGridNode externalNode = meP2PTunnelPart.getExternalFacingNode();
        if (externalNode == null) {
            setP2POutput(p2pPart, true);
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            return;
        }

        IGrid externalGrid = externalNode.getGrid();
        if (externalGrid == null || externalGrid.isEmpty()) {
            setP2POutput(p2pPart, true);
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            return;
        }

        boolean hasController = checkForController(externalGrid);

        if (hasController) {
            if (p2pPart.getFrequency() == 0) {
                long frequency = generateUniqueFrequency();
                p2pService.updateFreq(p2pPart, frequency);
                analysisP2P();
            }
            if (p2pPart.isOutput()) {
                setP2POutput(p2pPart, false);
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        } else {
            if (!p2pPart.isOutput()) {
                setP2POutput(p2pPart, true);
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        }
    }

    private boolean checkForController(IGrid grid) {
        if (grid == null) {
            return false;
        }
        for (IGridNode node : grid.getNodes()) {
            if (node == null) {
                continue;
            }
            IGridHost machine = node.getMachine();
            // 不直接引用 appeng.tile.networking.TileController，避免其实现的 GregTech 接口
            // (gregtech.api.interfaces.tileentity.IEnergyConnected) 在编译期缺失导致无法加载类。
            if (machine != null && machine.getClass().getName().equals("appeng.tile.networking.TileController")) {
                return true;
            }
        }
        return false;
    }

    // setOutput(boolean) 在 AE2 rv3 的 PartP2PTunnel 中为包级私有，跨包无法直接调用，故用反射。
    private static java.lang.reflect.Method setOutputMethod;
    private static java.lang.reflect.Method setCustomNameInternalMethod;

    private static void setP2POutput(PartP2PTunnel part, boolean output) {
        if (part == null) {
            return;
        }
        try {
            if (setOutputMethod == null) {
                setOutputMethod = PartP2PTunnel.class.getDeclaredMethod("setOutput", boolean.class);
                setOutputMethod.setAccessible(true);
            }
            setOutputMethod.invoke(part, output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke PartP2PTunnel.setOutput", e);
        }
    }

    /**
     * 仅对单个 P2P 部件设置自定义名，不波及同频段其他设备。
     * AE2 rv3 的 PartP2PTunnel.setCustomName 会把名字同步到同频段的输入与所有输出端，
     * 因此这里改用包级私有的 setCustomNameInternal（仅调用 AEBasePart.setCustomName）。
     */
    private static void setP2PCustomNameInternal(PartP2PTunnel part, String name) {
        if (part == null) {
            return;
        }
        try {
            if (setCustomNameInternalMethod == null) {
                setCustomNameInternalMethod = PartP2PTunnel.class.getDeclaredMethod("setCustomNameInternal", String.class);
                setCustomNameInternalMethod.setAccessible(true);
            }
            setCustomNameInternalMethod.invoke(part, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke PartP2PTunnel.setCustomNameInternal", e);
        }
    }

    /**
     * 判断 P2P 是否真正连接。
     * 注意：AE2 rv3 的 P2PCache.getInputs/getOutputs 通过 TunnelCollection.matches 做
     * 严格的 this.clz == c 比较（而非 isAssignableFrom），传入基类 PartP2PTunnel.class 永远
     * 无法匹配具体子类（如 PartP2PTunnelItem），导致始终返回空集合。因此这里直接基于本管理器
     * 已收集的 p2pDevicesMap 判定：同频率 + 同类型 + 一端为输入、另一端为输出。
     */
    public boolean isConnected(PartP2PTunnel p2pPart) {
        if (p2pPart == null) {
            return false;
        }
        long freq = p2pPart.getFrequency();
        boolean output = p2pPart.isOutput();
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p == null || p == p2pPart) {
                continue;
            }
            if (p.getFrequency() != freq) {
                continue;
            }
            if (p.getClass() != p2pPart.getClass()) {
                continue;
            }
            if (output) {
                if (!p.isOutput()) {
                    return true;
                }
            } else {
                if (p.isOutput()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void renameP2P(PartP2PTunnel p2pPart, String name) {
        // 注意：AE2 rv3 的 PartP2PTunnel.setCustomName 会把名字同步到同频段的所有
        // 输入/输出端（getInput + getOutputs），因此这里必须调用只作用于单个部件的
        // setCustomNameInternal，否则改一个 P2P 名字会波及整个频段。
        setP2PCustomNameInternal(p2pPart, name);
    }

    public void assignNewFrequency() {
        if (p2pTunnelPartBinding == null) {
            return;
        }

        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return;
        }
        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return;
        }

        currentP2PService.updateFreq(p2pTunnelPartBinding, 0L);

        if (p2pTunnelPartBinding.isOutput()) {
            setP2POutput(p2pTunnelPartBinding, false);
            currentP2PService.updateFreq(p2pTunnelPartBinding, 0L);
        }

        long newFreq = generateUniqueFrequency();
        currentP2PService.updateFreq(p2pTunnelPartBinding, newFreq);

        analysisP2P();
    }

    /**
     * rv3 的 P2PCache 没有 newFrequency()，这里自行生成一个未被当前网络占用的 long 频段。
     */
    private long generateUniqueFrequency() {
        long freq;
        int attempts = 0;
        do {
            freq = (System.currentTimeMillis() ^ (long) (Math.random() * 0xFFFFFFFFL)) & 0xFFFFFFFFFFFFFFFFL;
            if (freq == 0) {
                freq = 1;
            }
            attempts++;
        } while (isFrequencyUsed(freq) && attempts < 100);
        return freq;
    }

    private boolean isFrequencyUsed(long freq) {
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p != null && p.getFrequency() == freq) {
                return true;
            }
        }
        return false;
    }

    public static String getP2PType(PartP2PTunnel p2pPart) {
        String name = p2pPart.getClass().getSimpleName();
        if (name.startsWith("PartP2PTunnel")) {
            name = name.substring("PartP2PTunnel".length());
        }
        return name.isEmpty() ? "P2P" : name;
    }

    public void renderP2P(PartP2PTunnel p2pPart) {
        if (p2pPart == null || player == null) {
            return;
        }
        analysisP2P();
        ArrayList<P2PPosition> positions = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        positions.add(toPosition(p2pPart));
        colors.add(ModConfigs.getClientConfig().highlightColorSelf);

        PartP2PTunnel input = getInputOf(p2pPart);
        if (input != null) {
            positions.add(toPosition(input));
            colors.add(ModConfigs.getClientConfig().highlightColorInput);
        }
        for (PartP2PTunnel output : getOutputsOf(p2pPart)) {
            if (output == p2pPart || output == null) {
                continue;
            }
            positions.add(toPosition(output));
            colors.add(ModConfigs.getClientConfig().highlightColorOutput);
        }
        sendHighlight(positions, colors);
    }

    public void renderP2P(String frequencyHex) {
        analysisP2P();

        ArrayList<P2PPosition> positions = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        long freq = Long.parseLong(frequencyHex, 16);
        PartP2PTunnel input = getInput(freq);
        if (input == null) {
            return;
        }
        positions.add(toPosition(input));
        colors.add(ModConfigs.getClientConfig().highlightColorInput);
        for (PartP2PTunnel output : getOutputs(freq)) {
            if (output == null) {
                continue;
            }
            positions.add(toPosition(output));
            colors.add(ModConfigs.getClientConfig().highlightColorOutput);
        }
        sendHighlight(positions, colors);
    }

    private void sendHighlight(ArrayList<P2PPosition> positions, ArrayList<Integer> colors) {
        if (player instanceof EntityPlayerMP) {
            NetworkHandler.sendToPlayer(new HighlightPacket(positions, colors), (EntityPlayerMP) player);
        }
    }

    private P2PPosition toPosition(PartP2PTunnel part) {
        TileEntity te = part.getTile();
        World world = te.getWorldObj();
        int dimId = world.provider.dimensionId;
        BlockPos pos = new BlockPos(te.xCoord, te.yCoord, te.zCoord);
        return new P2PPosition(pos, part.getSide(), String.valueOf(dimId));
    }

    private PartP2PTunnel getInput(long freq) {
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p == null) {
                continue;
            }
            if (p.isOutput()) {
                continue;
            }
            if (p.getFrequency() == freq) {
                return p;
            }
        }
        return null;
    }

    private List<PartP2PTunnel> getOutputs(long freq) {
        List<PartP2PTunnel> result = new ArrayList<>();
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p == null) {
                continue;
            }
            if (!p.isOutput()) {
                continue;
            }
            if (p.getFrequency() == freq) {
                result.add(p);
            }
        }
        return result;
    }

    private PartP2PTunnel getInputOf(PartP2PTunnel part) {
        long freq = part.getFrequency();
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p == null || p == part) {
                continue;
            }
            if (p.isOutput()) {
                continue;
            }
            if (p.getFrequency() != freq) {
                continue;
            }
            if (p.getClass() != part.getClass()) {
                continue;
            }
            return p;
        }
        return null;
    }

    private List<PartP2PTunnel> getOutputsOf(PartP2PTunnel part) {
        List<PartP2PTunnel> result = new ArrayList<>();
        long freq = part.getFrequency();
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p == null || p == part) {
                continue;
            }
            if (!p.isOutput()) {
                continue;
            }
            if (p.getFrequency() != freq) {
                continue;
            }
            if (p.getClass() != part.getClass()) {
                continue;
            }
            result.add(p);
        }
        return result;
    }

    public PartP2PTunnel getP2PTunnelPart() {
        return p2pTunnelPartBinding;
    }

    public HashMap<PartP2PTunnel, String> getP2PDevicesMap() {
        return p2pDevicesMap;
    }

    public HashMap<String, String> getP2PFrequencyAndAlias() {
        return p2pFrequencyAndAlias;
    }

    public void setP2PTunnelPart(PartP2PTunnel p2pPart) {
        this.p2pTunnelPartBinding = p2pPart;
    }

    public void setFrequencyAlias(String newName, String frequency) {
        analysisP2P();

        long freq = Long.parseLong(frequency, 16);
        PartP2PTunnel input = getInput(freq);
        if (input != null) {
            renameP2P(input, newName);
        }
    }

    public String getFrequencyAlias(String frequency) {
        analysisP2P();

        long freq = Long.parseLong(frequency, 16);
        PartP2PTunnel input = getInput(freq);
        if (input != null && input.hasCustomName()) {
            String name = input.getCustomName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return frequency;
    }
}
