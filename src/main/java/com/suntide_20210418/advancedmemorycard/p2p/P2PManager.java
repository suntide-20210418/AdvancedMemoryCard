package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartItem;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.service.P2PService;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.CustomNameUtil;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.mixin.P2PTunnelPartMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

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
        this.p2pService = P2PService.get(grid);
        // 初始化时立即处理频段为0但仍连接了设备的异常P2P
        fixZeroFrequencyAnomalies();
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

        short freq = (short) Integer.parseInt(frequencyHex, 16);

        // 非 ME P2P 设备：智能判断输入/输出端角色
        // 检查目标频段中是否已有输入端：
        //   - 如果有输入端：当前设备作为输出端（setOutput(true)）
        //   - 如果没有输入端：当前设备作为输入端（setOutput(false)）
        if (!(p2pTunnelPartBinding instanceof MEP2PTunnelPart)) {
            P2PTunnelPart<?> existingInput = currentP2PService.getInput(freq);
            if (existingInput != null) {
                // 频段中已有输入端，当前设备作为输出端
                if (!p2pTunnelPartBinding.isOutput()) {
                    ((P2PTunnelPartMixin) p2pTunnelPartBinding).invokeSetOutput(true);
                }
            } else {
                // 频段中没有输入端，当前设备作为输入端
                if (p2pTunnelPartBinding.isOutput()) {
                    ((P2PTunnelPartMixin) p2pTunnelPartBinding).invokeSetOutput(false);
                }
            }
        }

        currentP2PService.updateFreq(p2pTunnelPartBinding, freq);

        // 更新内部缓存引用
        this.grid = currentGrid;
        this.p2pService = currentP2PService;
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
            // 这样可以避免把正常配置好的输入端误改成输出端并持久化到存档。
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
            // 外部节点尚未就绪（刚放置或网络重建中），此时无法判断是否有控制器，
            // 不强制改写输入/输出端角色，保留玩家/频道管理设定的角色，仅同步连接。
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            return;
        }

        IGrid externalGrid = externalNode.getGrid();
        if (externalGrid == null || externalGrid.isEmpty()) {
            // 外部网络尚未就绪，同样无法判断控制器，保留当前角色，仅同步连接，
            // 避免在重进存档的网络重建时序中把正常输入端误判为输出端。
            p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
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
                // 同步 P2PService 的 inputs/outputs map
                p2pService.updateFreq(p2pPart, p2pPart.getFrequency());
            }
        } else {
            // 外部网络没有控制器：应该设为输出端
            if (!p2pPart.isOutput()) {
                ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
                // 同步 P2PService 的 inputs/outputs map
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

    public void renameP2P(P2PTunnelPart<?> p2pPart, String name){
        CompoundTag tag = new CompoundTag();
        CustomNameUtil.setCustomName(tag, name);
        p2pPart.importSettings(SettingsFrom.MEMORY_CARD, tag, player);
    }

    /**
     * 为当前待绑定的非 ME P2P 设备分配一个新频段并设置为输入端。
     * 操作的是 p2pTunnelPartBinding（即 Memory Card 当前关联的 P2P 设备），而非外部传入的任意 P2P。
     * 流程：
     * 1. 断开当前频段连接（设置为频段 0）
     * 2. 确保该 P2P 为输入端（setOutput(false)）
     * 3. 通过 p2pService.newFrequency() 获取新频段
     * 4. 绑定到新频段
     */
    public void assignNewFrequency() {
        if (p2pTunnelPartBinding == null) return;

        // 运行时重新获取 grid 和 p2pService
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 先断开当前频段连接（设置为频段 0）
        currentP2PService.updateFreq(p2pTunnelPartBinding, (short) 0);

        // 确保设为输入端（非 ME P2P 需要手动管理输入/输出端）
        if (p2pTunnelPartBinding.isOutput()) {
            ((P2PTunnelPartMixin) p2pTunnelPartBinding).invokeSetOutput(false);
            // 用当前频段(0)同步 P2PService 的 inputs/outputs map，
            // 确保从 outputs map 移到 inputs map
            currentP2PService.updateFreq(p2pTunnelPartBinding, (short) 0);
        }

        // 获取一个新频段并绑定
        short newFreq = currentP2PService.newFrequency();
        currentP2PService.updateFreq(p2pTunnelPartBinding, newFreq);

        // 刷新内部映射
        analysisP2P();
    }

    public static ResourceLocation getP2PType(P2PTunnelPart<?> p2pPart){
        IPartItem<?> partItem = p2pPart.getPartItem();
        return IPartItem.getId(partItem);
    }

    public void renderP2P(P2PTunnelPart<?> p2pPart) {
        if (p2pPart == null) return;
        // 渲染属于客户端行为，专用服务器上无客户端类，必须仅在物理客户端执行
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            P2PRenderer p2pRenderer = P2PRenderer.getInstance();
            p2pRenderer.clearAllRenders();
            p2pRenderer.triggerRender(p2pPart, ModConfigs.getClientConfig().highlightColorSelf.get());
            if (p2pPart.getInput() != null) {
                p2pRenderer.triggerRender(p2pPart.getInput(), ModConfigs.getClientConfig().highlightColorInput.get());
            }
            for (P2PTunnelPart<?> output : p2pPart.getOutputs()) {
                if (output == p2pPart || output == null) continue;
                p2pRenderer.triggerRender(output, ModConfigs.getClientConfig().highlightColorOutput.get());
            }
        });
    }

    public void renderP2P(String frequencyHex){
        // 运行时重新获取 p2pService，避免因构造时网络未就绪导致渲染失败
        IGrid currentGrid = getCurrentGrid();
        if (currentGrid == null) return;
        P2PService currentP2PService = P2PService.get(currentGrid);
        if (currentP2PService == null) return;

        // 渲染属于客户端行为，专用服务器上无客户端类，必须仅在物理客户端执行
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            P2PRenderer p2pRenderer = P2PRenderer.getInstance();
            p2pRenderer.clearAllRenders();
            short freq = (short) Integer.parseInt(frequencyHex, 16);
            P2PTunnelPart<?> input = currentP2PService.getInput(freq);
            if (input == null) return;
            p2pRenderer.triggerRender(input, ModConfigs.getClientConfig().highlightColorInput.get());
            input.getOutputs().forEach(output -> p2pRenderer.triggerRender(output, ModConfigs.getClientConfig().highlightColorOutput.get()));
        });
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

        short freq = (short) Integer.parseInt(frequency, 16);
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

        short freq = (short) Integer.parseInt(frequency, 16);
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
