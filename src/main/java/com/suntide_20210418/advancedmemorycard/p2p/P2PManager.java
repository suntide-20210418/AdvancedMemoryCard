package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.GridAccessException;
import appeng.me.cache.P2PCache;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;
import appeng.tile.networking.TileController;
import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * P2P 网络分析与管理（AE2 rv6 / 1.12.2 适配）。
 * 对应 1.20.1 的 P2PManager，将 P2PTunnelPart / P2PService / MEP2PTunnelPart 等 rv7 API
 * 替换为 rv6 的 PartP2PTunnel / P2PCache / PartP2PTunnelME，并通过访问转换器暴露 setOutput。
 */
public class P2PManager {
    private PartP2PTunnel p2pTunnelPartBinding;
    private IGrid grid;
    private final EntityPlayer player;
    private final HashMap<String, String> p2pFrequencyAndAlias;
    private final HashMap<PartP2PTunnel, String> p2pDevicesMap;
    private P2PCache p2pService;

    private static final java.lang.reflect.Method SET_OUTPUT_METHOD;
    static {
        java.lang.reflect.Method m = null;
        try {
            m = PartP2PTunnel.class.getDeclaredMethod("setOutput", boolean.class);
            m.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            // 反射获取 AE2 PartP2PTunnel 的包级私有 setOutput
        }
        SET_OUTPUT_METHOD = m;
    }

    private static void setOutput(PartP2PTunnel part, boolean output) {
        if (SET_OUTPUT_METHOD == null || part == null) {
            return;
        }
        try {
            SET_OUTPUT_METHOD.invoke(part, output);
        } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            // ignore
        }
    }

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
                short rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%04X", rawFreq & 0xFFFF);
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

        short freq = (short) Integer.parseInt(frequencyHex, 16);

        // 非 ME P2P 设备：智能判断输入/输出端角色
        if (!(p2pTunnelPartBinding instanceof PartP2PTunnelME)) {
            PartP2PTunnel existingInput = getInput(freq);
            if (existingInput != null) {
                if (!p2pTunnelPartBinding.isOutput()) {
                    setOutput(p2pTunnelPartBinding, true);
                }
            } else {
                if (p2pTunnelPartBinding.isOutput()) {
                    setOutput(p2pTunnelPartBinding, false);
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
            currentP2PService.updateFreq(p2pPart, (short) 0);
            setOutput(p2pPart, true);
            currentP2PService.updateFreq(p2pPart, (short) 0);
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
                short rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%04X", rawFreq & 0xFFFF);
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
            setOutput(p2pPart, true);
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            return;
        }

        IGrid externalGrid = externalNode.getGrid();
        if (externalGrid == null || externalGrid.isEmpty()) {
            setOutput(p2pPart, true);
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            return;
        }

        boolean hasController = checkForController(externalGrid);

        if (hasController) {
            if (p2pPart.getFrequency() == 0) {
                short frequency = p2pService.newFrequency();
                p2pService.updateFreq(p2pPart, frequency);
                analysisP2P();
            }
            if (p2pPart.isOutput()) {
                setOutput(p2pPart, false);
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        } else {
            if (!p2pPart.isOutput()) {
                setOutput(p2pPart, true);
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
            if (node.getMachine() instanceof TileController) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 P2P 是否真正连接。
     * 注意：AE2 rv6 的 P2PCache.getInputs/getOutputs 通过 TunnelCollection.matches 做
     * 严格的 this.clz == c 比较（而非 isAssignableFrom），传入基类 PartP2PTunnel.class 永远
     * 无法匹配具体子类（如 PartP2PTunnelItem），导致始终返回空集合。因此这里直接基于本管理器
     * 已收集的 p2pDevicesMap 判定：同频率 + 同类型 + 一端为输入、另一端为输出。
     */
    public boolean isConnected(PartP2PTunnel p2pPart) {
        if (p2pPart == null) {
            return false;
        }
        short freq = p2pPart.getFrequency();
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
        p2pPart.setCustomName(name);
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

        currentP2PService.updateFreq(p2pTunnelPartBinding, (short) 0);

        if (p2pTunnelPartBinding.isOutput()) {
            setOutput(p2pTunnelPartBinding, false);
            currentP2PService.updateFreq(p2pTunnelPartBinding, (short) 0);
        }

        short newFreq = currentP2PService.newFrequency();
        currentP2PService.updateFreq(p2pTunnelPartBinding, newFreq);

        analysisP2P();
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
        ArrayList<Integer> types = new ArrayList<>();
        positions.add(toPosition(p2pPart));
        types.add(HighlightPacket.TYPE_SELF);

        PartP2PTunnel input = getInputOf(p2pPart);
        if (input != null) {
            positions.add(toPosition(input));
            types.add(HighlightPacket.TYPE_INPUT);
        }
        for (PartP2PTunnel output : getOutputsOf(p2pPart)) {
            if (output == p2pPart || output == null) {
                continue;
            }
            positions.add(toPosition(output));
            types.add(HighlightPacket.TYPE_OUTPUT);
        }
        sendHighlight(positions, types);
    }

    public void renderP2P(String frequencyHex) {
        analysisP2P();

        ArrayList<P2PPosition> positions = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        short freq = (short) Integer.parseInt(frequencyHex, 16);
        PartP2PTunnel input = getInput(freq);
        if (input == null) {
            return;
        }
        positions.add(toPosition(input));
        types.add(HighlightPacket.TYPE_INPUT);
        for (PartP2PTunnel output : getOutputs(freq)) {
            if (output == null) {
                continue;
            }
            positions.add(toPosition(output));
            types.add(HighlightPacket.TYPE_OUTPUT);
        }
        sendHighlight(positions, types);
    }

    private void sendHighlight(ArrayList<P2PPosition> positions, ArrayList<Integer> types) {
        if (player instanceof EntityPlayerMP) {
            NetworkHandler.sendToPlayer(new HighlightPacket(positions, types), (EntityPlayerMP) player);
        }
    }

    private P2PPosition toPosition(PartP2PTunnel part) {
        TileEntity te = part.getTile();
        int dimId = te.getWorld().provider.getDimension();
        return new P2PPosition(te.getPos(), part.getSide(), String.valueOf(dimId));
    }

    private PartP2PTunnel getInput(short freq) {
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

    private List<PartP2PTunnel> getOutputs(short freq) {
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
        short freq = part.getFrequency();
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
        short freq = part.getFrequency();
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

        short freq = (short) Integer.parseInt(frequency, 16);
        PartP2PTunnel input = getInput(freq);
        if (input != null) {
            renameP2P(input, newName);
        }
    }

    public String getFrequencyAlias(String frequency) {
        analysisP2P();

        short freq = (short) Integer.parseInt(frequency, 16);
        PartP2PTunnel input = getInput(freq);
        if (input != null && input.hasCustomInventoryName()) {
            String name = input.getCustomInventoryName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return frequency;
    }
}
