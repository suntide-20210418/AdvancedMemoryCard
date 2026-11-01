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

    /**
     * 配对的源设备（“把哪一台绑走”）。
     *
     * <p>刻意与 {@link #p2pTunnelPartBinding} 分开：后者是 {@link #analysisP2P()} 的扫描锚点，
     * 决定界面看到的是哪一个 grid。以前两者是同一个字段，于是“在树里选一台设备”会顺带把
     * 整个界面切到另一张网络的扫描结果。现在 UI 改配对源只动这里，扫描范围保持不变。
     */
    private PartP2PTunnel pendingBindTarget;

    /** bind() 的结果码：UI 需要知道“为什么没成功”，而不是只看一个静默的 return。 */
    public enum BindResult {
        OK,
        NO_TARGET,
        INVALID_FREQUENCY,
        NO_GRID,
        NO_SERVICE
    }

    /** {@link #switchToInput(PartP2PTunnel)} 的结果码。 */
    public enum RoleResult {
        OK,
        NO_TARGET,
        /** 设备没绑定频段（0000）：没有通道可以接管。 */
        NO_CHANNEL,
        /** 已经是输入端。界面上不该出现这个按钮，这里只做防御。 */
        ALREADY_INPUT,
        NO_GRID,
        NO_SERVICE
    }

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

    /** 供其它包（粘贴路径）复用同一个反射入口，避免再复制一份私有方法查找。 */
    public static void setP2POutput(PartP2PTunnel part, boolean output) {
        setOutput(part, output);
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
        this.pendingBindTarget = p2pTunnelPartBinding;
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

    public BindResult bind(String frequencyHex) {
        return bindTo(getBindTarget(), frequencyHex);
    }

    /**
     * 把指定设备绑到指定频段。
     *
     * <p>批量绑定用它：逐台调用即可，角色仲裁（谁当输入端谁当输出端）在
     * {@code P2PCache} 层面自然完成 —— 第一台注册成输入端后，后面的都会被判成输出端。
     *
     * @param target 要绑定的设备；为 null 时返回 {@link BindResult#NO_TARGET}
     */
    public BindResult bindTo(PartP2PTunnel target, String frequencyHex) {
        if (target == null) {
            return BindResult.NO_TARGET;
        }

        analysisP2P();

        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return BindResult.NO_GRID;
        }

        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return BindResult.NO_SERVICE;
        }

        short freq = parseFrequency(frequencyHex);

        // 频段 0 在 AE2 里是「未绑定」。1.12.2 的 P2PCache 用的是 Multimap，
        // 把所有 freq=0 的设备登记进同一条通道，于是未绑定设备在界面里会互相「连通」。
        // 绑定到 0000 等于静默解绑，而且这个状态会随 updateFreq 写进存档，直接拒绝。
        if (freq == 0) {
            return BindResult.INVALID_FREQUENCY;
        }

        // 角色仲裁：目标频段已有【其他】设备的输入端时，当前设备作为输出端。
        // 这里对所有类型统一处理（含 ME P2P）：让玩家把第二个设备绑到已有通道时
        // 得到「输入 → 输出」的正常结构，而不是两个输入端挂在同一频段上互相不通。
        PartP2PTunnel existingInput = getInput(freq);
        boolean shouldBeOutput = existingInput != null && existingInput != target;
        if (shouldBeOutput != target.isOutput()) {
            setOutput(target, shouldBeOutput);
        }

        currentP2PService.updateFreq(target, freq);

        // updateFreq 只在频段变化时才会走到 setFrequency；角色变化而频段不变时
        // 上面那次 setOutput 是纯内存写，需要显式让宿主落盘，否则重进存档角色丢失。
        target.getHost().markForSave();

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
        return BindResult.OK;
    }

    /**
     * 把一台输出端设备改成它所在频段的输入端，原来那台输入端降级为输出端。
     *
     * <p>为什么必须成对做：一个频段只留一个输入端，而 {@code updateFreq} 会直接覆盖旧条目。
     * 若先把当前设备提升为输入端，旧输入端不会被删除，只是从映射里消失：它的
     * {@code isOutput()} 仍是 false，却再也不在输入端表里，也就永远等不到输出端。
     * 所以顺序固定为：先把该频段上所有「非输出端」降级为输出端，再提升目标。
     */
    public RoleResult switchToInput(PartP2PTunnel target) {
        if (target == null) {
            return RoleResult.NO_TARGET;
        }

        short freq = target.getFrequency();
        if (freq == 0) {
            return RoleResult.NO_CHANNEL;
        }
        if (!target.isOutput()) {
            return RoleResult.ALREADY_INPUT;
        }

        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return RoleResult.NO_GRID;
        }
        P2PCache currentP2PService = getP2PService();
        if (currentP2PService == null) {
            return RoleResult.NO_SERVICE;
        }

        demoteInputsAt(currentP2PService, freq, target);

        setOutput(target, false);
        currentP2PService.updateFreq(target, freq);
        // 频段没变 → updateFreq 既不落盘也不通知邻居。角色变了却不落盘，
        // 重进存档又会变回输出端，所以两件事都显式补一次。
        target.getHost().markForSave();
        target.onTunnelNetworkChange();

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
        return RoleResult.OK;
    }

    /**
     * 把指定频段上所有「非输出端」改成输出端并重登记。
     *
     * <p>遍历设备表而不是只取 {@code getInput(freq)}：旧版本写坏的存档里可能有多台
     * 非输出端部件，其中只有一台在映射里。只处理映射里那台，剩下的会在目标提升之后变成孤儿。
     */
    private void demoteInputsAt(P2PCache service, short freq, PartP2PTunnel promoteTarget) {
        List<PartP2PTunnel> snapshot = new ArrayList<>(p2pDevicesMap.keySet());
        for (PartP2PTunnel part : snapshot) {
            if (part == null || part == promoteTarget) continue;
            if (part.getFrequency() != freq || part.isOutput()) continue;
            setOutput(part, true);
            service.updateFreq(part, freq);
            part.getHost().markForSave();
            part.onTunnelNetworkChange();
        }

        // 兜底：映射里那个输入端若不在设备表（例如扫描时它所在节点离线），上面那轮会漏掉它
        PartP2PTunnel stillInput = getInput(freq);
        if (stillInput != null && stillInput != promoteTarget) {
            setOutput(stillInput, true);
            service.updateFreq(stillInput, freq);
            stillInput.getHost().markForSave();
            stillInput.onTunnelNetworkChange();
        }
    }

    /** 解析 4 位十六进制频段。非法输入返回 0（即「未绑定」），由调用方拒绝。 */
    private static short parseFrequency(String frequencyHex) {
        if (frequencyHex == null || frequencyHex.isEmpty()) {
            return 0;
        }
        try {
            return (short) Integer.parseInt(frequencyHex, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        // rv6 的 setCustomName 只改部件持有的 ItemStack 显示名，不会自己落盘；
        // 不显式 markForSave 的话，重进存档名字就没了（1.20.1 线上踩过同一个坑）。
        p2pPart.setCustomName(name);
        p2pPart.getHost().markForSave();
    }

    public void assignNewFrequency() {
        PartP2PTunnel target = getBindTarget();
        if (target == null) {
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

        // 不再先把频段写成 0 再分配：两次 updateFreq 之间如果被区块卸载/退出存档打断，
        // 存档里会留下「已绑定设备变成未绑定」。直接一次性换到新频段。
        if (target.isOutput()) {
            setOutput(target, false);
        }

        short newFreq = currentP2PService.newFrequency();
        currentP2PService.updateFreq(target, newFreq);
        target.getHost().markForSave();

        analysisP2P();
    }

    /**
     * 当前配对源设备（会被 bind / 分配新频段操作的那一台）。
     *
     * <p>取值回退顺序：显式选中的 pendingBindTarget → 打开界面时右键的那一台。
     * 若显式目标已经不在本次扫描结果里（被拆掉 / 换了网络），同样回退。
     */
    public PartP2PTunnel getBindTarget() {
        if (pendingBindTarget != null && p2pDevicesMap.containsKey(pendingBindTarget)) {
            return pendingBindTarget;
        }
        return p2pTunnelPartBinding;
    }

    /** 设置配对源设备。只影响绑定目标，不影响扫描锚点。 */
    public void setPendingBindTarget(PartP2PTunnel p2pPart) {
        if (p2pPart != null) {
            this.pendingBindTarget = p2pPart;
        }
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
