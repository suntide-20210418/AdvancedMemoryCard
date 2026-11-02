# 更新日志 / Changelog

面向玩家与整合包作者的版本日志。内部重构、CI 调整等不影响游戏行为的改动不在此列出。

---

## 未发布（待批准）：界面译名、底图、树点击与操作失效

**Minecraft** 1.7.10 · **Forge** 10.13.4.1614 · **AE2** GTNH Applied-Energistics-2-Unofficial rv3

> 版本号与发布暂缓，等实机确认后再定版。

修掉界面里显示原始翻译键（`gui.advanced_memory_card...` 这种）的问题：新做的操作条、配对条、状态行、树头与树汇总用的键，代码拼出来的名字和语言文件里的名字对不上。

### 修复

- **树整块点不动**：基准里「鼠标命中控件 → 调 `onClick`」是 AE2 `AbstractWidget#mouseClicked` 干的活，旧版 `Widget` 基类的 `mouseClicked` 只返回 `false`，于是 `P2PTreeWidget.onClick` 从头到尾没有调用者 —— 点设备行不选中、点频段行不展开、Ctrl 多选与框选起手也都没了。现在树的 `mouseClicked` 自己派发：左键落在树矩形内就调 `onClick` 并返回 `true`。
- **滚动条被树吃掉点击**：滚动条的矩形压在树右缘的 14px 留白上，而事件的派发顺序是「树 → 滚动条」，树开始消费点击之后滑块就再也拖不动了。现在滚动条先于树判定（与基准里它是独立控件、后加的在上层一致）。

- **界面显示原始翻译键**：`TranslateHelper.Keys` 里的键都是 `GUI_PREFIX + "advanced_memory_card.<组>."`，拼出来是双份命名空间的 `gui.advanced_memory_card.advanced_memory_card.<组>.*`。历史组（复制模式、检查器、按钮、状态、树、聊天、tooltip）的语言文件一直是双份，能对上；但 1.2.0 新加的 `action_bar` / `pair` / `status_line` / `p2p_type` 写成了单份，`tree` 与 `tree_col` 干脆没有写，于是这 39 个键在游戏里按原样显示。现在两语文件统一为双份命名空间，并补齐 `tree_col.{name,frequency,summary}` 与 `tree.{summary_channel,summary_channel_plain,summary_type,role_input,role_output,prefix_input,prefix_output}` 共 10 条新键。
- **P2P 类型名的键前缀**：`P2PTypeNames` 的 `LANG_PREFIX` 同步改成双份命名空间，与其余各组保持一致。

### 修复（操作按钮与高亮）

- **配置界面所有操作按钮失效（绑定 / 改名 / 传送 / 高亮 / 刷新）**：`ServerTaskQueue.register()` 把 `ServerTaskQueue.class` 传给了 FML 的 `EventBus.register(Object)`。1.7.10 的 `EventBus` 只有 `register(Object)`，没有 `register(Class<?>)` 重载，它扫描的是 `target.getClass().getMethods()` —— 传 `Class` 对象时扫描的是 `java.lang.Class` 自己的方法，`onServerTick` 一个都没注册。于是动作包全部入队、队列从未被消费，所有服务端 action 静默失效。现在注册单例实例，并把 `onServerTick` 改成实例方法。
- **客户端延迟任务同样不执行**：`ClientTickTasks.register()` 有同样的 `register(Class)` 误用。传送后的到达等待、延迟高亮都挂在这个队列上，队列不跑，这些收尾动作也就不会发生。同样改为注册实例。
- **高亮不关界面、不渲染、不刷聊天栏**：界面从未在 `onHighlightP2P` / `onHighlightChannel` 里关闭；现在按基准 1.21.1 的顺序执行「发包 → 关界面 → 转头」，并补上频段高亮时转向第一台输入端的逻辑。
- **高亮发包后立刻关界面会丢动作**：动作包被延迟到下一个服务端 tick 执行，而关容器的包先到，执行时 `player.openContainer` 已回到玩家背包，`instanceof IActionMenu` 失败、动作被丢弃。现在动作包处理器在收到包时就抓住菜单实例，菜单关掉后仍持有 `p2pManager` 与玩家背包，动作照常执行。
- **远程 / 未加载区块的传送与高亮静默失败**：`parsingP2P` 只走世界侧 `getTileEntity`，区块未加载或朝向索引对不上时返回 `null`。改名早已用 `findP2PByPositionString` 兜底，传送 / 高亮 / 切换输入端 / 设配对源现在统一走 `resolveP2P`，失败时同样退回已分析部件表。
- **同维度传送白跑一次维度转移**：1.7.10 的 `transferPlayerToDimension` 不判断 `j == dimension`，会把玩家 danger-remove 再传回。现在只在维度不同时调用。
- **传送后界面不关**：服务端已经完成位移，客户端还对着 GUI，玩家会以为按钮没反应。现在发包后立即关界面，与基准 1.21.1 的 `doTeleport` 一致。
- **高亮始终不渲染（客户端找不到部件）**：`ClientHighlightHandler.findPart` 用 `DimensionManager.getWorld(dimId)`。1.7.10 的 `DimensionManager` 是服务端注册表，`setWorld` 只在 `WorldServer` / `MinecraftServer` 里调用，客户端 `loadWorld` 不会注册 `WorldClient`：专用服务器客户端上 `getWorld` 恒为 `null`，单机拿到的是服务端 `WorldServer` 及其部件，都不是渲染用的那一份。现在改用 `Minecraft.theWorld`，并且只处理当前维度（跨维度的部件本来就看不见）。

### 修复（选中态与检查器底色）

- **设备行第一次点击后选中态被清掉**：`TreeNode.p2pNodeId()` 用的是 `dimension()` 且漏掉 `:frequency`，与 `getNodeId()` 的 P2P 分支拼法不一致。点击设备会触发 `set_pending_bind`，服务端回包后自动导航用 `p2pNodeId` 恢复选中，匹配失败就把选中态当成节点消失清掉；第二次点击时 pending 已相同、不再导航，所以能选上。现在两者逐字一致（维度 id + 坐标 + 朝向 + 频段）。
- **右侧检查器多出一块白色矩形**：`DetailPanelWidget.render()` 先用 `paletteSurface` 铺满整块面板，盖住了基准底图。现在移除这次填充，面板底色交给 `config_mode_menu.png`，与 1.21.1 用 `ScreenStyle` 面板底、不二次填充的做法一致。

### 修复（批量绑定）

- **批量模式多选只绑定一台**：多选集合的键 `positionKey` 用 `info.dimension()`（维度名，如 `Overworld`）拼 `P2PPosition`，而 `p2pInfoMap` 的键用的是 `String.valueOf(dimensionId)`（如 `0`）；`P2PPosition.equals` 又把 dimension 纳入比较，于是 `p2pInfoMap.get(key)` 全部落空，`getSourceCandidates()` 退回单选的 `selectedP2PInfo`，`doBind()` 只发 `bind_frequency` 而不是 `bind_frequency_batch`。现在 `positionKey` 统一用维度 id 字符串，多选反查能拿全所有设备。

### 新增

- **传送落地后点亮目标 P2P**：传送发包后界面立即关闭，但此时坐标还是旧的，跨维度时新维度的区块也还没加载，直接点亮会拿不到部件。现在用 `ClientTickTasks` 每 tick 探一次「落点 + 目标部件可查」（客户端世界维度一致、距方块中心 2 格内、`findPart` 能找到部件），真正到达并加载后再点亮目标，并在 `rotateHeadOnHighlight` 开启时转头；10 秒超时。只判坐标会在"坐标先到、区块后到"的窗口里把高亮静默丢掉，这是上一次修复后传送仍不显示的原因。

### 改动

- **配置模式界面换上基准底图**：1.2.0 交付时面板是纯色自绘的（`GuiDraw.fillRect` 刷一块 `0xFFC7CCDA`），基准 1.21.1 的那张 `guis/config_mode_menu.png` 从没被打包进来。现在把基准图集（512×512，取左上 440×312）原样搬过来，界面直接铺这张图；标题对齐基准的 (7,7)，搜索框去掉原版黑底、改成面板上的纯文字输入并补占位符「搜索…」。控件矩形与基准 JSON 本来就逐项相同，位置没有改动。

## 1.2.2-1.7.10-GTNH — 2026-11-01

**Minecraft** 1.7.10 · **Forge** 10.13.4.1614 · **AE2** GTNH Applied-Energistics-2-Unofficial rv3

修复 1.2.1 里同一张界面剩下的第二个崩溃点：类型名解析走了 1.7.10 用不上的物品注册表；顺带把 P2P 类型名与 ME 类型判断改对。

### 修复

- **配置模式界面崩溃（`ClassCastException`）**：渲染 P2P 树的类型行时，`P2PTypeNames` 用 `ResourceLocation` 去查 1.7.10 的物品注册表，而这张表的键是 `String`，`RegistryNamespaced.getObject(Object)` 会先做强转，于是抛 `ResourceLocation cannot be cast to String`，客户端在 `Rendering screen` 阶段崩溃。现在类型名只查本模组自己的语言键，不再碰注册表。
- **类型名显示成类名**：`P2PManager.getP2PType` 只剥掉了 `PartP2PTunnel` 前缀，`PartP2PRedstone` / `PartP2PItems` 这类部件会把 `PartP2PRedstone` 原样画到树上。现在两个前缀都剥掉，配合新增的 14 条 `gui.advanced_memory_card.p2p_type.*` 语言键（中英各一份），树上显示「红石 P2P 隧道」「物品 P2P 隧道」等名称；第三方模组的 P2P 部件查不到译名时退回原始 token。
- **ME 频道的占用数字不显示**：`P2PChannelUsage.ME_TYPE` 沿用基准的 `me_p2p_tunnel`，而 1.7.10 的类型 token 是 `ME`，两者永远不相等，导致 ME 频道的占用统计与检查器里的 ME 判定一直失效。现在统一为 `ME`，输出端设备行与频道行重新显示占用数字。

### 改进

- **配置模式树**：类型行不再有 `PartP2P` 前缀噪声，同一类型下 ME / 普通隧道分组名与其他界面（检查器「类型」一行）保持一致。

## 1.2.1-1.7.10-GTNH — 2026-11-01

**Minecraft** 1.7.10 · **Forge** 10.13.4.1614 · **AE2** GTNH Applied-Energistics-2-Unofficial rv3

修复 1.2.0 里打开配置模式界面、自动跳到待绑定设备时客户端直接崩溃的问题；并把 64 位频段的比较统一到无符号语义。

### 修复

- **配置模式界面崩溃（`NumberFormatException`）**：1.7.10 的频段是 64 位，服务端下发的是 16 位十六进制（如 `000001A13FF44AA4`）。树在排序频道时用 `Integer.parseInt(frequency, 16)` 解析，必然溢出并抛出异常，而这个排序发生在界面 tick 的 `updateScreen` → `tryAutoNavigateToPendingBind` → `navigateToP2P` → `rebuildTree` 调用链里，于是直接 `Ticking screen` 崩溃。现在统一改用 64 位无符号比较（`ChannelInfo.compareFrequency`），遇到脏数据退化为长度 + 字典序，不再有异常逃逸到渲染循环。
- **批量搜索结果排序**：搜索模式下频道结果的频段排序有同样的问题，一并改掉。
- **配对条频段下拉顺序错乱**：下拉列表的排序把解析失败的值全部塞成 `Integer.MAX_VALUE`，所有 64 位频段排序值相同，顺序实际上是随机的；现在按真实数值从小到大。
- **最高位为 1 的频段被当成「未绑定」**：服务端解析频段用的是有符号 `Long.parseLong`，`F000000000000001` 这类频段会解析失败并被当作 0，导致渲染、高亮与频段改名静默失效；现在改用 `Long.parseUnsignedLong`。

## 1.2.0-1.7.10-GTNH — 2026-11-01

**Minecraft** 1.7.10 · **Forge** 10.13.4.1614 · **AE2** GTNH Applied-Energistics-2-Unofficial rv3

配置模式界面按 1.21.1 的基准重做，P2P 的绑定、改名与频段显示一并适配 64 位频段；复制模式与七项粘贴选项保持原有行为。

### 新增

- **配置模式界面重做**：改为 440×312 的自绘面板，包含左侧四列 P2P 树（状态点 / 名称 / 频段 / 汇总）、顶部配对条（源 → 目标 + 频段下拉 + 批量开关）、右侧检查器（频段 / 角色 / 连接 / 别名 / 按钮组）、底部操作条与状态行。
- **状态行**：所有操作统一在底部状态行反馈，覆盖绑定、批量绑定、切换输入端、改名、刷新等 16 种结果，并按成功 / 警告 / 失败着色。
- **批量绑定**：Ctrl 点击非设备行进入批量模式，框选多台设备后一次性绑定到同一频段；状态行会区分「批量成功」与「批量部分成功」。
- **检查器增强**：支持就地改名（`Enter` 提交 / `Esc` 取消）、设置频道别名、把设备切为输入端、传送与高亮定位。
- **客户端配色板**：新增 `client.palette.*` 语义色板，配置模式界面的文字、强调色、行状态、面板底色、状态色与浮层颜色都可在 `config/advanced_memory_card/client.cfg` 中调整。

### 改进

- **频段显示适配 64 位**：树中无别名时只显示 `…` + 末 6 位（如 `…A3F019`），完整频段值在右侧检查器里看；频段一律以 16 位十六进制传递（如 `A3F019B7C2D40815`），搜索仍然匹配完整十六进制。
- **通道分组**：通道键改为「频段 + P2P 类型」，同一频段下不同类型的隧道各自成组，不再互相覆盖；树与检查器按「频段 + 类型」查找频道，设备行的频道占用数字与频道别名都能取到。
- **绑定结果分类**：绑定失败会按原因区分（无目标 / 频段非法 / 无网络），状态行直接给出具体原因，而不是笼统的一句失败。

### 修复

- 按住左键从按钮上滑出、再移动鼠标时会误触发该按钮；现在 `mouseMovedOrUp` 只在 `button >= 0` 时派发 Up 事件，滑出即取消。
- P2P 绑定的角色仲裁对 ME 隧道也一视同仁：一个频段只保留一个输入端，其余自动作为输出端，避免整条 ME 通道永久显示未连接。
- 拒绝绑定全零频段（`0000…0000`）：这类频段在 AE2 里代表「未连接」，不能作为绑定目标。
- 切换输入端时先降级旧输入端、再提升目标设备，避免中间态出现两个输入端。

## 更早的版本

`1.1.0-1.7.10-GTNH` 及更早的改动早于本文件建立，未在此追述。
