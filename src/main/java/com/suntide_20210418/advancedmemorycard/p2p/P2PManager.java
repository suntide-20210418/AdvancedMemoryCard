package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.suntide_20210418.advancedmemorycard.network.HighlightPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.GridAccessException;
import appeng.me.cache.P2PCache;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;

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

    /**
     * 配对的源设备（“把哪一台绑走”）。
     *
     * <p>
     * 刻意与 {@link #p2pTunnelPartBinding} 分开：后者是 {@link #analysisP2P()} 的扫描锚点，
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

    public P2PManager(PartP2PTunnel p2pTunnelPartBinding, EntityPlayer player) {
        this.p2pTunnelPartBinding = p2pTunnelPartBinding;
        this.pendingBindTarget = p2pTunnelPartBinding;
        this.p2pDevicesMap = new HashMap<>();
        this.p2pFrequencyAndAlias = new HashMap<>();
        this.player = player;
        analysisP2P();
        try {
            this.p2pService = p2pTunnelPartBinding.getProxy()
                .getP2P();
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

    public BindResult bind(String frequencyHex) {
        return bindTo(getBindTarget(), frequencyHex);
    }

    /**
     * 把指定设备绑到指定频段。
     *
     * <p>
     * 批量绑定用它：逐台调用即可，角色仲裁（谁当输入端谁当输出端）在
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

        long freq = parseFrequency(frequencyHex);
        // 频段 0 在 AE2 里代表"未绑定"，而且 P2PCache 会把 freq=0 的设备登记进
        // inputs[0]/outputs[0]，让一堆未绑定设备在界面里互相"连通"。
        // 把它当绑定目标等于静默解绑。
        if (freq == 0) {
            return BindResult.INVALID_FREQUENCY;
        }

        // 角色仲裁对所有隧道类型统一，不豁免 ME：
        // inputs 是"一个频段只存一个输入端"的映射，后登记的会把先前的挤出去；
        // 若让第二台 ME 也当输入端，outputs 永远为空，而 isConnected 对输入端要求
        // "至少一个同频输出端"，整条通道会永久显示未连接，并随 markForSave 写进存档。
        PartP2PTunnel existingInput = getInput(freq);
        boolean shouldBeOutput = existingInput != null && existingInput != target;
        if (shouldBeOutput != target.isOutput()) {
            setP2POutput(target, shouldBeOutput);
        }

        currentP2PService.updateFreq(target, freq);
        // 频段没变时 updateFreq 不落盘，但角色可能变了，显式补一次。
        if (target.getHost() != null) {
            target.getHost()
                .markForSave();
        }

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
        return BindResult.OK;
    }

    /**
     * 把一台输出端设备改成它所在频段的输入端，原来那台输入端降级为输出端。
     *
     * <p>
     * 为什么必须成对做：一个频段只留一个输入端，而 {@code updateFreq} 会直接覆盖旧条目。
     * 若先把当前设备提升为输入端，旧输入端不会被删除，只是从映射里消失：它的
     * {@code isOutput()} 仍是 false，却再也不在输入端表里，也就永远等不到输出端。
     * 所以顺序固定为：先把该频段上所有「非输出端」降级为输出端，再提升目标。
     */
    public RoleResult switchToInput(PartP2PTunnel target) {
        if (target == null) {
            return RoleResult.NO_TARGET;
        }

        long freq = target.freq;
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

        setP2POutput(target, false);
        currentP2PService.updateFreq(target, freq);
        // 频段没变 → updateFreq 既不落盘也不通知邻居，两件事都显式补一次
        if (target.getHost() != null) {
            target.getHost()
                .markForSave();
        }
        target.onTunnelNetworkChange();

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
        return RoleResult.OK;
    }

    /**
     * 把指定频段上所有「非输出端」改成输出端并重登记。
     *
     * <p>
     * 遍历设备表而不是只取 {@code getInput(freq)}：旧版本写坏的存档里可能有多台
     * 非输出端部件，其中只有一台在映射里。只处理映射里那台，剩下的会在目标提升之后变成孤儿。
     */
    private void demoteInputsAt(P2PCache service, long freq, PartP2PTunnel promoteTarget) {
        List<PartP2PTunnel> snapshot = new ArrayList<>(p2pDevicesMap.keySet());
        for (PartP2PTunnel part : snapshot) {
            if (part == null || part == promoteTarget) continue;
            if (part.freq != freq || part.isOutput()) continue;
            setP2POutput(part, true);
            service.updateFreq(part, freq);
            if (part.getHost() != null) {
                part.getHost()
                    .markForSave();
            }
            part.onTunnelNetworkChange();
        }

        // 兜底：映射里那个输入端若不在设备表（例如扫描时它所在节点离线），上面那轮会漏掉它
        PartP2PTunnel stillInput = getInput(freq);
        if (stillInput != null && stillInput != promoteTarget) {
            setP2POutput(stillInput, true);
            service.updateFreq(stillInput, freq);
            if (stillInput.getHost() != null) {
                stillInput.getHost()
                    .markForSave();
            }
            stillInput.onTunnelNetworkChange();
        }
    }

    /** 解析 16 位十六进制频段；非法输入返回 0（即"未绑定"），由调用方拒绝。 */
    private static long parseFrequency(String frequencyHex) {
        if (frequencyHex == null || frequencyHex.isEmpty()) {
            return 0L;
        }
        try {
            // 频段是 long，最高位可能为 1（例如 F000000000000001），必须按无符号解析，
            // 用 Long.parseLong 会抛 NumberFormatException 并被当成"未绑定"。
            return Long.parseUnsignedLong(frequencyHex.trim(), 16);
        } catch (NumberFormatException e) {
            return 0L;
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
            return p2pTunnelPartBinding.getProxy()
                .getP2P();
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
            // TileCreativeEnergyController 继承自 TileController，同样是控制器，需一并判定。
            if (machine != null) {
                String className = machine.getClass()
                    .getName();
                if (className.equals("appeng.tile.networking.TileController")
                    || className.equals("appeng.tile.networking.TileCreativeEnergyController")) {
                    return true;
                }
            }
        }
        return false;
    }

    // setOutput(boolean) 在 AE2 rv3 的 PartP2PTunnel 中为包级私有，跨包无法直接调用，故用反射。
    private static java.lang.reflect.Method setOutputMethod;
    private static java.lang.reflect.Method setCustomNameInternalMethod;

    /**
     * 供 {@code utils.SettingsApplier} 复用：换型后需要恢复原来的输入/输出角色，
     * 而 {@code setOutput} 在 rv3 是包私有的，只能反射。
     */
    public static void setP2POutput(PartP2PTunnel part, boolean output) {
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
                setCustomNameInternalMethod = PartP2PTunnel.class
                    .getDeclaredMethod("setCustomNameInternal", String.class);
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
        // 名字写在部件自己的 ItemStack 上，不标脏的话下次保存会丢
        if (p2pPart != null && p2pPart.getHost() != null) {
            p2pPart.getHost()
                .markForSave();
        }
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
            setP2POutput(target, false);
        }

        long newFreq = generateUniqueFrequency();
        currentP2PService.updateFreq(target, newFreq);

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
        String name = p2pPart.getClass()
            .getSimpleName();
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
        long freq = parseFrequency(frequencyHex);
        if (freq == 0L) {
            return;
        }
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
        World world = te.getWorldObj();
        int dimId = world.provider.dimensionId;
        BlockPos pos = new BlockPos(te.xCoord, te.yCoord, te.zCoord);
        return new P2PPosition(pos, part.getSide(), String.valueOf(dimId));
    }

    /**
     * 按 GUI 里编码的 {@code x|y|z|sideOrdinal|dimId} 在已分析的部件里查找。
     *
     * <p>
     * 用于改名：{@code parsingP2P} 走的是世界侧 {@code getTileEntity}，当目标区块未加载或
     * 朝向索引对不上时会返回 null，改名就会静默失败。这里直接比对内存里已收集的部件。
     */
    public PartP2PTunnel findP2PByPositionString(String data) {
        try {
            String[] parts = data.split("\\|");
            if (parts.length < 4) {
                return null;
            }
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            int sideOrdinal = Integer.parseInt(parts[3]);
            int dimId = parts.length >= 5 ? Integer.parseInt(parts[4]) : player.worldObj.provider.dimensionId;

            for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
                if (p == null) {
                    continue;
                }
                net.minecraft.tileentity.TileEntity te = p.getTile();
                if (te == null || te.getWorldObj() == null) {
                    continue;
                }
                if (te.xCoord != x || te.yCoord != y || te.zCoord != z) {
                    continue;
                }
                if (te.getWorldObj().provider.dimensionId != dimId) {
                    continue;
                }
                if (p.getSide()
                    .ordinal() != sideOrdinal) {
                    continue;
                }
                return p;
            }
        } catch (Exception ignored) {
            // 编码异常按找不到处理
        }
        return null;
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

    /**
     * 当前配对源设备（会被 bind / 分配新频段操作的那一台）。
     *
     * <p>
     * 取值回退顺序：显式选中的 pendingBindTarget → 打开界面时右键的那一台。
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

    public void setFrequencyAlias(String newName, String frequency) {
        analysisP2P();

        long freq = parseFrequency(frequency);
        if (freq == 0L) {
            return;
        }
        PartP2PTunnel input = getInput(freq);
        if (input != null) {
            renameP2P(input, newName);
            return;
        }
        // 没有输入端时（例如输入端所在区块未加载、或该频段当前只有输出端）退化为
        // 重命名该频段的任意一个部件。否则玩家看到的就是「改名完全没反应」。
        for (PartP2PTunnel p : p2pDevicesMap.keySet()) {
            if (p != null && p.getFrequency() == freq) {
                renameP2P(p, newName);
                return;
            }
        }
    }

    public String getFrequencyAlias(String frequency) {
        analysisP2P();

        long freq = parseFrequency(frequency);
        if (freq == 0L) {
            return frequency;
        }
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
