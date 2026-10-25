package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartItem;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.service.P2PService;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import com.suntide_20210418.advancedmemorycard.mixin.AEBasePartAccessor;
import com.suntide_20210418.advancedmemorycard.mixin.P2PTunnelPartMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class P2PManager {
    private P2PTunnelPart<?> p2pTunnelPartBinding;
    private IGrid grid;
    private final Player player;
    private final HashMap<String, String> p2pFrequencyAndAlias;
    private final HashMap<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap;
    private P2PService p2pService;

    public P2PManager(P2PTunnelPart<?> p2pTunnelPartBinding, Player player) {
        this.p2pTunnelPartBinding = p2pTunnelPartBinding;
        this.p2pDevicesMap = new HashMap<>();
        this.p2pFrequencyAndAlias = new HashMap<>();
        this.player = player;
        analysisP2P();
        // 部件的 freq/output 持久化在方块 NBT 里，而 P2PService 的 inputs/outputs
        // 是纯运行时映射，只在节点被并入网格时由 addNode 重建。二者不是同一份状态：
        // 重进存档 / 网络重建期间运行时映射尚未建全，isConnected() 的结果不可信。
        // 这里只补登缺失的条目，不动已经正确的条目，让后续判定有意义。
        synchronizeServiceMappings();
        // 修复"同一频段登记了多个输入端、却一个输出端都没有"的坏通道（历史版本写坏的存档）
        repairChannelInputs();
        // 初始化时立即处理频段为0但仍连接了设备的异常P2P
        fixZeroFrequencyAnomalies();
    }

    /**
     * 补登 P2PService 中缺失的 inputs/outputs 条目。
     *
     * <p>为什么不能像以前那样无脑对每个部件调用 {@code updateFreq(part, part.getFrequency())}：
     *
     * <ul>
     *   <li>{@code P2PService.inputs} 是 {@code HashMap<Short, P2PTunnelPart>} —— 一个频段只
     *       存一个输入端。{@code updateFreq} 在入口处是
     *       {@code if (inputs.containsValue(part)) inputs.remove(part.getFrequency())}，
     *       即按频段 key 整条删，然后重新 put。</li>
     *   <li>因此当同一频段存在两个输入端时，谁最后被迭代到谁就占据 {@code inputs[F]}。
     *       而部件集合是 HashMap 的 keySet，迭代顺序不稳定 —— 表现为"同一条通道，
     *       重进存档前后显示的状态/别名会变"。</li>
     * </ul>
     *
     * <p>所以这里只在"该频段当前确实没有任何登记"时补一次；已经有人占位就不去抢。
     */
    private void synchronizeServiceMappings() {
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;

        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        for (P2PTunnelPart<?> p2pPart : new ArrayList<>(p2pDevicesMap.keySet())) {
            if (p2pPart == null) continue;
            IGridNode node = p2pPart.getGridNode();
            if (node == null || node.getGrid() != currentGrid) continue;

            short freq = p2pPart.getFrequency();
            // 频段 0 在 AE2 里是"未绑定"，不属于任何通道，不参与对齐。
            if (freq == 0) continue;

            if (p2pPart.isOutput()) {
                P2PTunnelPart<?> input = currentP2PService.getInput(freq);
                if (input != null && input.getClass() == p2pPart.getClass()
                        && input.getOutputs().contains(p2pPart)) {
                    continue; // 已在 outputs 映射中
                }
                // outputs 是 LinkedHashMultimap（SetMultimap），重复登记是幂等的
                currentP2PService.updateFreq(p2pPart, freq);
            } else {
                // 输入端只在频段空缺时补登记，避免把同频段真正的输入端挤出映射
                if (currentP2PService.getInput(freq) != null) continue;
                currentP2PService.updateFreq(p2pPart, freq);
            }
        }

        this.grid = currentGrid;
        this.p2pService = currentP2PService;
    }

    /**
     * 修复"同一频段有多个输入端、但没有输出端"的通道。
     *
     * <p>这种状态是旧版 {@link #bind(String)} 留下的：它对 ME P2P 跳过了输入/输出端仲裁，
     * 于是把第二个设备绑到已有通道时只是又登记了一个输入端 ——
     * {@code P2PService.inputs} 是单槽 HashMap，后来的输入把先前的挤出映射，
     * {@code outputs} 永远为空。而 {@link #isConnected} 对输入端要求"至少有一个同频输出端"，
     * 所以整条通道（包括被挤出去的那个）在界面上永久显示"未连接"，且这个坏状态
     * 已经由 {@code setOutput}/{@code updateFreq} 写进了存档，重进存档照旧。
     *
     * <p>修复策略：只在"该频段没有任何输出端"时动手 —— 这种通道当前不传递任何东西，
     * 把多余的那个输入端降级为输出端，只会让玩家当初的绑定意图（连起来）成立。
     * 保留哪个作为输入端：优先取有自定义名的那个（频段别名就是输入端名称，玩家改名
     * 的那一端才是他认定的输入端），否则取 P2PService 当前登记的那个。
     */
    private void repairChannelInputs() {
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;

        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 按频段归类输入端
        HashMap<Short, List<P2PTunnelPart<?>>> inputsByFreq = new HashMap<>();
        for (P2PTunnelPart<?> p2pPart : new ArrayList<>(p2pDevicesMap.keySet())) {
            if (p2pPart == null || p2pPart.isOutput()) continue;
            short freq = p2pPart.getFrequency();
            if (freq == 0) continue;
            inputsByFreq.computeIfAbsent(freq, k -> new ArrayList<>()).add(p2pPart);
        }

        for (var entry : inputsByFreq.entrySet()) {
            List<P2PTunnelPart<?>> candidates = entry.getValue();
            if (candidates.size() < 2) continue;

            P2PTunnelPart<?> registered = currentP2PService.getInput(entry.getKey());
            if (registered == null) continue;

            // 节点未上线时 getOutputs() 恒为空，无法判断该频段是否已有输出端 —— 保持原状
            IGridNode registeredNode = registered.getGridNode();
            if (registeredNode == null || !registeredNode.isOnline()) continue;

            if (!registered.getOutputs().isEmpty()) continue; // 通道已有输出端，不动

            P2PTunnelPart<?> keeper = pickChannelInput(candidates, registered);
            for (P2PTunnelPart<?> p2pPart : candidates) {
                if (p2pPart == keeper) continue;
                // 不同隧道类型之间的输入/输出概念不通用，跨类型不做改写
                if (p2pPart.getClass() != keeper.getClass()) continue;
                // 先改角色再重登记：updateFreq 内部按 isOutput() 决定放进 inputs 还是 outputs。
                // 被降级的部件本来就不在 inputs 里（它被后来的输入挤掉了），
                // 所以 inputs.containsValue(part) 为 false，不会误删保留者的条目。
                ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
                currentP2PService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        }
    }

    /**
     * 从同一频段的多个输入端中挑出"作为通道输入端"的那一个。
     * 有且仅有一个带自定义名时取它，否则回退到 P2PService 当前登记的输入端。
     */
    private static P2PTunnelPart<?> pickChannelInput(List<P2PTunnelPart<?>> candidates,
                                                     P2PTunnelPart<?> registered) {
        P2PTunnelPart<?> named = null;
        int namedCount = 0;
        for (P2PTunnelPart<?> p2pPart : candidates) {
            if (p2pPart.getClass() != registered.getClass()) continue;
            Component customName = p2pPart.getCustomName();
            if (customName == null || customName.getString().isEmpty()) continue;
            named = p2pPart;
            namedCount++;
        }
        return namedCount == 1 ? named : registered;
    }

    public void analysisP2P() {
        // 直接通过 P2PTunnelPart 获取其所在的 GridNode
        IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        if (gridNode == null) {
            return;
        }

        // 通过 GridNode 获取整个网络
        grid = gridNode.getGrid();
        if (grid == null) {
            return;
        }

        // 遍历网络中的所有节点，找出所有 P2P 设备
        for (IGridNode node : grid.getNodes()) {
            Object machine = node.getOwner();
            if (machine instanceof P2PTunnelPart<?> p2pPart) {
                p2pDevicesMap.put(p2pPart, getP2PType(p2pPart));
                short rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%04X", rawFreq & 0xFFFF);
                if (!p2pFrequencyAndAlias.containsValue(frequency)) {
                    p2pFrequencyAndAlias.put(frequency, frequency);
                }
            }
        }
    }

    public void bind(String frequencyHex){
        if (p2pTunnelPartBinding == null) {
            return;
        }

        // 频段 0 在 AE2 里代表"未绑定"，且 P2PService 会把 freq=0 的设备登记进
        // inputs[0] / outputs[0]，让一堆未绑定设备在 GUI 里互相"连通"。
        // 因此本方法只接受 1..0xFFFF 的真实频段：GUI 上的"未连接(0000)"频道
        // 不是可绑定目标，绑定到它等于静默解绑，而 setFrequency 会把这个状态写进存档。
        short freq = parseFrequency(frequencyHex);
        if (freq == 0) {
            return;
        }

        // 运行时重新获取 grid 和 p2pService，避免因构造时网络未就绪导致绑定失败
        IGrid currentGrid = null;
        IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
        if (gridNode != null) {
            currentGrid = gridNode.getGrid();
        }
        if (currentGrid == null) {
            return;
        }

        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) {
            return;
        }

        // 角色仲裁（对所有 P2P 类型统一，不再跳过 ME P2P）：
        //   - 目标频段已有【其他】设备的输入端 → 当前设备作为输出端（setOutput(true)）
        //   - 没有输入端（或输入端就是自己）   → 当前设备作为输入端（setOutput(false)）
        //
        // 为什么 ME P2P 也必须走这段：P2PService.inputs 是 HashMap<Short, P2PTunnelPart>，
        // 一个频段只存一个输入端，后登记的输入会把先前的挤出映射；而 outputs 是独立的
        // Multimap。旧实现对 ME P2P 跳过仲裁，于是"把第二个 ME P2P 绑到已有通道"
        // 只会再登记一个输入端 —— outputs 永远为空，isConnected() 对输入端的判定
        // （要求至少一个同频输出端）恒为 false，界面永久显示"未连接"；
        // 而且这个坏状态会随 setOutput/updateFreq 的 markForSave 一起写进存档，
        // 重进存档后照样是未连接。
        P2PTunnelPart<?> existingInput = currentP2PService.getInput(freq);
        boolean shouldBeOutput = existingInput != null && existingInput != p2pTunnelPartBinding;
        if (shouldBeOutput != p2pTunnelPartBinding.isOutput()) {
            ((P2PTunnelPartMixin) p2pTunnelPartBinding).invokeSetOutput(shouldBeOutput);
        }

        currentP2PService.updateFreq(p2pTunnelPartBinding, freq);
        // updateFreq → setFrequency 在频段变化时会 markForSave；这里再显式标记一次，
        // 保证"角色变了但频段没变"这类只改 output 的场景同样落盘。
        p2pTunnelPartBinding.getHost().markForSave();

        // 更新内部缓存引用
        this.grid = currentGrid;
        this.p2pService = currentP2PService;
    }

    /**
     * 解析 4 位十六进制频段。非法输入返回 0（即"未绑定"），由调用方拒绝。
     */
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
        // 运行时重新获取 grid 和 p2pService，避免因构造时网络未就绪导致失败
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null || currentGrid.isEmpty()) {
            return;
        }
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 确保 p2pDevicesMap 是最新的
        if (p2pDevicesMap.isEmpty()) {
            analysisP2P();
        }

        // 复制一份 key 列表后再遍历，避免 processSingleP2PPart 中调用 analysisP2P()
        // 导致 p2pDevicesMap.clear() 引发的 ConcurrentModificationException，
        // 进而导致只处理第一个 P2P 就中断循环的问题。
        List<P2PTunnelPart<?>> p2pParts = new ArrayList<>(p2pDevicesMap.keySet());
        for (P2PTunnelPart<?> p2pPart : p2pParts) {
            if (p2pPart == null) continue;
            processSingleP2PPart(p2pPart, currentP2PService);
        }
    }

    /**
     * 尝试从 p2pTunnelPartBinding 重新获取当前 grid。
     * 如果 p2pTunnelPartBinding 为 null，则回退到内部缓存的 grid。
     */
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

    private void fixZeroFrequencyAnomalies() {
        // 运行时重新获取 grid 和 p2pService
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 收集所有频段为0但仍处于连接状态的异常P2P设备
        List<P2PTunnelPart<?>> anomalousP2Ps = new ArrayList<>();
        for (P2PTunnelPart<?> p2pPart : p2pDevicesMap.keySet()) {
            if (p2pPart == null) continue;
            if (p2pPart.getFrequency() == 0 && isConnected(p2pPart)) {
                anomalousP2Ps.add(p2pPart);
            }
        }

        if (anomalousP2Ps.isEmpty()) return;

        // 处理每个异常P2P设备
        for (P2PTunnelPart<?> p2pPart : anomalousP2Ps) {
            // 强制断开连接：通过P2PService将频段更新为0，
            // 这会从旧频段的inputs/outputs映射中移除该设备
            currentP2PService.updateFreq(p2pPart, (short) 0);

            // 角色修正交给 processSingleP2PPart：它遵循"有控制器方为输入端"的规则
            // 来决定输入/输出端，而不是无条件把所有异常 P2P 重置为输出端。
            // 原来这里的 setOutput(true) 会 markForSave，把打开界面时的误判固化进存档，
            // 重进后正常输入端变成输出端，通道随之失效（表现为"绑定丢失"）。
            // 非 ME P2P 会被 processSingleP2PPart 内部忽略，保留玩家配置的角色。
            processSingleP2PPart(p2pPart, currentP2PService);
        }

        // 验证：重新扫描网格中的P2P设备，刷新映射，确保异常状态已被彻底清除
        refreshP2PDevicesMap();
    }

    /**
     * 重新扫描网格中的P2P设备并刷新 p2pDevicesMap 和 p2pFrequencyAndAlias。
     * 与 analysisP2P() 不同，此方法不会调用 fixZeroFrequencyAnomalies()，避免递归。
     */
    private void refreshP2PDevicesMap() {
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
        if (grid == null) return;

        for (IGridNode node : grid.getNodes()) {
            Object machine = node.getOwner();
            if (machine instanceof P2PTunnelPart<?> p2pPart) {
                p2pDevicesMap.put(p2pPart, getP2PType(p2pPart));
                short rawFreq = p2pPart.getFrequency();
                String frequency = String.format("%04X", rawFreq & 0xFFFF);
                if (!p2pFrequencyAndAlias.containsValue(frequency)) {
                    p2pFrequencyAndAlias.put(frequency, frequency);
                }
            }
        }
    }

    private void processSingleP2PPart(P2PTunnelPart<?> p2pPart, P2PService p2pService) {
        if (!(p2pPart instanceof MEP2PTunnelPart meP2PTunnelPart)) {
            return;
        }

        if (p2pPart.getFrequency() != 0) {
            return;
        }

        IGridNode externalNode = meP2PTunnelPart.getExternalFacingNode();
        if (externalNode == null) {
            // 外部节点尚未就绪（刚放置或网络重建中），此时无法判断是否有控制器。
            // 保留玩家/频道管理设定的角色，不做任何写入：setOutput 会 markForSave，
            // 在重建时序里把它当成最终答案写进存档，重进后角色就再也回不去了。
            return;
        }

        IGrid externalGrid = externalNode.getGrid();
        if (externalGrid == null || externalGrid.isEmpty()) {
            // 外部网络尚未就绪，同上：只读判断，不改角色。
            return;
        }

        boolean hasController = checkForController(externalGrid);

        if (hasController) {
            // 外部网络有控制器：应该设为输入端
            if (p2pPart.getFrequency() == 0) {
                // 尚未分配频率，分配一个新频率
                short frequency = p2pService.newFrequency();
                p2pService.updateFreq(p2pPart, frequency);
                analysisP2P();
            }
            // 确保设为输入端（如果当前不是输入端）
            if (p2pPart.isOutput()) {
                ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(false);
                // 角色变了才重登记：P2PService.updateFreq 内部是按频段 key 去移除
                // inputs 条目的，无条件调用会把同频段真正的输入端从映射里挤掉。
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        } else {
            // 外部网络没有控制器：应该设为输出端
            if (!p2pPart.isOutput()) {
                ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        }

    }

    private boolean checkForController(IGrid grid) {
        if (grid == null) return false;
        Iterable<IGridNode> nodes = grid.getNodes();
        if (nodes == null) return false;
        for (IGridNode node : nodes) {
            if (node == null) continue;
            if (node.getOwner() instanceof ControllerBlockEntity) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnected(P2PTunnelPart<?> p2pPart) {
        if (p2pPart == null) return false;
        if (p2pPart.isOutput()) {
            // 输出设备：检查是否有输入
            return p2pPart.getInput() != null;
        } else {
            // 输入设备：检查是否有输出
            List<?> outputs = p2pPart.getOutputs();
            return outputs != null && !outputs.isEmpty();
        }
    }

    /**
     * 设置 P2P 部件的自定义名（空字符串表示清除名称）。
     *
     * <p>不走 {@code importSettings(SettingsFrom.MEMORY_CARD, ...)}：那条路会额外执行
     * {@code MemoryCardItem.importGenericSettings}（升级卡 / 优先级 / 配置项），而且
     * {@code P2PTunnelPart#importSettings} 在数据里带 EXPORTED_P2P_FREQUENCY 时还会
     * {@code setOutput(true)} + {@code updateFreq} —— 改名不该有这些副作用。
     * AE2 没有公开 setter，所以直接写 {@code AEBasePart.customName}。
     *
     * <p>写完必须显式落盘：{@code AEBasePart.writeToNBT} 只在 customName 非 null 时写这个键，
     * 而改名字段本身不经过任何 setFrequency/setOutput，没有任何地方会顺带 markForSave，
     * 于是"界面里改了、重进存档又没了"。
     */
    public void renameP2P(P2PTunnelPart<?> p2pPart, String name) {
        if (p2pPart == null) {
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        ((AEBasePartAccessor) p2pPart)
                .setCustomName(trimmed.isEmpty() ? null : Component.literal(trimmed));

        // 落盘 + 让客户端重新同步该部件（customName 不在 writeToStream 里，只能靠方块更新包）
        p2pPart.getHost().markForSave();
        p2pPart.getHost().markForUpdate();
    }

    /**
     * 为当前待绑定的非 ME P2P 设备分配一个新频段并设置为输入端。
     * 操作的是 p2pTunnelPartBinding（即 Memory Card 当前关联的 P2P 设备），而非外部传入的任意 P2P。
     * 流程：
     * 1. 确保该 P2P 为输入端（setOutput(false)）
     * 2. 通过 p2pService.newFrequency() 获取新频段
     * 3. 一次性绑定到新频段
     *
     * <p>注意：这里刻意<b>不</b>先写 freq=0 再分配。updateFreq 本身就会把部件从旧频段的
     * inputs/outputs 中摘除再登记到新频段；先写 0 只会多产生一次 markForSave，并且一旦在
     * 两次调用之间被打断（区块卸载、玩家退出存档），存档里留下的就是 freq=0 —— 已绑定设备
     * 被静默改成未绑定。
     */
    public void assignNewFrequency() {
        if (p2pTunnelPartBinding == null) return;

        // 运行时重新获取 grid 和 p2pService
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 确保设为输入端（非 ME P2P 需要手动管理输入/输出端）
        if (p2pTunnelPartBinding.isOutput()) {
            ((P2PTunnelPartMixin) p2pTunnelPartBinding).invokeSetOutput(false);
        }

        // 直接换到新频段：updateFreq 会把部件从旧频段映射中移除并登记到新频段
        short newFreq = currentP2PService.newFrequency();
        currentP2PService.updateFreq(p2pTunnelPartBinding, newFreq);
        p2pTunnelPartBinding.getHost().markForSave();

        // 刷新内部映射
        this.grid = currentGrid;
        this.p2pService = currentP2PService;
        analysisP2P();
    }

    public static ResourceLocation getP2PType(P2PTunnelPart<?> p2pPart){
        IPartItem<?> partItem = p2pPart.getPartItem();
        return IPartItem.getId(partItem);
    }

    /**
     * 获取当前绑定的 P2P 设备（即 Memory Card 当前关联的目标 P2P）。
     * 可能返回 null（尚未绑定或绑定已清除时）。
     */
    public P2PTunnelPart<?> getP2PTunnelPart(){
        return p2pTunnelPartBinding;
    }

    public HashMap<P2PTunnelPart<?>, ResourceLocation> getP2PDevicesMap() {
        return p2pDevicesMap;
    }

    /**
     * 设置当前绑定的 P2P 设备。
     * 注意：调用后需要由上层（如 ConfigModeMenu）手动调用 analysisP2P() 刷新网络数据。
     */
    public void setP2PTunnelPart(P2PTunnelPart<?> p2pPart){
        this.p2pTunnelPartBinding = p2pPart;
    }

    public HashMap<String, String> getP2PFrequencyAndAlias() {
        return p2pFrequencyAndAlias;
    }

    /**
     * 设置频段别名：将频段中第一个输入端 P2P 的名称设置为指定名称。
     * 频段别名现在指向该频段中输入端 P2P 设备的名称。
     *
     * @param newName   新的输入端名称
     * @param frequency 目标频段 hex
     */
    public void setFrequencyAlias(String newName, String frequency) {
        // 运行时重新获取 p2pService
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        short freq = parseFrequency(frequency);
        if (freq == 0) return;
        P2PTunnelPart<?> input = currentP2PService.getInput(freq);
        if (input != null) {
            renameP2P(input, newName);
        }
    }

    /**
     * 获取频段别名（即该频段中第一个输入端 P2P 的名称）。
     * 如果没有输入端或输入端没有自定义名称，返回频段 hex。
     *
     * @param frequency 目标频段 hex
     * @return 输入端名称或频段 hex
     */
    public String getFrequencyAlias(String frequency) {
        // 运行时重新获取 p2pService
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return frequency;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return frequency;

        short freq = parseFrequency(frequency);
        if (freq == 0) return frequency;
        P2PTunnelPart<?> input = currentP2PService.getInput(freq);
        if (input != null && input.getCustomName() != null) {
            String name = input.getCustomName().getString();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return frequency;
    }
}
