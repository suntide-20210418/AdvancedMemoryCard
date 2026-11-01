# 配置模式 UI 移植文档（1.20.1）

| 项 | 值 |
| --- | --- |
| 基准 | 1.21.1（`AdvancedMemoryCard-1.21.1`，分支 `suntide/1.21.1`） |
| 本版工作副本 | `AdvancedMemoryCard-1.20.1` |
| 分支 | `suntide/1.20.1` |
| 加载器 / Java | Forge 47.4.10 / Java 17 |
| AE2 | 15.4.10 |
| 版本 | `1.2.0-1.20.1` → 目标 `1.3.0-1.20.1` |
| 优先级 | **P1**，三版中最先实施 |

---

## 一、范围与目标

### 1.1 为什么先做这一版

1.20.1 与基准同属 AE2 15 系：`AEBaseScreen`、`WidgetContainer`、`ScreenStyle`、`Menu`、`AEScreen` 这一套同名同签名，`P2PInfo` / `ChannelInfo` / `P2PPosition` / `P2PTypeInfo` 与基准**字节级相同**。

这意味着这一版的移植是「搬代码 + 改少量 API」，而不是「按行为规格重写」。它同时是**语义契约的第一次可运行验证**：状态码、角色仲裁、布局常量、交互规格在这里落成真代码，后面两个版本照它对齐。

### 1.2 能力范围

契约 C5 的 18 项能力（F1–F18）在 1.20.1 **全部可全量实现**，无平台级降级。差异只在于「用什么 API 实现」，不改变行为。

唯一需要注意的实现差异：AE2 15.4.10 存在 `StyleManager` / `WidgetContainer` / `AETextField` / `Scrollbar` / `ConfirmableTextField`，但**没有任何通用下拉或列表选择控件**。基准的下拉本来就是自绘浮层（不依赖 AE2 控件），可直接搬。

### 1.3 开工前置

当前工作区有两处未提交状态，开工前必须先决定归属，不要顺手提交：

- `M build.gradle`（仓库镜像地址由 `bmclapi` 改为 `libraries.minecraft.net` + `neoforged.forgecdn`）—— 若确认是长期需要，单独提交；否则 `git checkout` 掉。
- `?? src/generated/resources/.cache/` —— 该目录被打进了 jar（`build/resources/main` 里能看到）。阶段 0 加 `.gitignore` 一行解决。

---

## 二、现状缺口

| 类别 | 内容 |
| --- | --- |
| 直接搬运（7 个文件） | `PairBarWidget`、`ActionBarWidget`、`P2PTypeNames`、`ActionStatus`、`ClientTickTasks`、`P2PChannelUsage`、`AEBasePartAccessor` |
| 重写（4 个文件） | `P2PTreeWidget`(851→约 1117)、`DetailPanelWidget`(1067→约 711)、`FlatButton`(123→约 186)、`ConfigModeScreen`(526→约 647) |
| 增量改写（3 个文件） | `ConfigModeMenu`（+4 action、状态码、`syncVersion`）、`ConfigModeSyncPacket`（+3 字段、Map 快照）、`P2PManager`（`BindResult`/`RoleResult`/`bindTo`/`switchToInput`/`demoteInputsAt`/双锚点） |
| 已有、无需改 | `P2PInfo`、`ChannelInfo`、`P2PPosition`、`P2PTypeInfo`（与基准一致）；`P2PTunnelPartMixin`（`@Invoker setOutput` 可用，本版**不需要** AT 文件） |
| 缺失能力 | 配对条、状态行、四列列表、多选/框选、批量绑定、传送、`switchToInput`、palette、`syncVersion` 节流 |
| 现状问题 | `containerTick` **每 tick** 重建整棵树；同步包直接持有调用方 Map（Netty 线程写字节时可能 CME）；`ConfigModeMenu` 只有 10 个 action |

`P2PRenderHelper` 已存在，但 `resolvePart` 缺少维度判定，跨维度高亮会点亮同坐标的无关方块 —— 与本轮 UI 无关，但阶段 A 顺手补上。

---

## 三、适配要点

### 3.1 必须遵守的时序

AE2 的控件定位分两段：`widgets.add(id, widget)` 时按 JSON 设宽高，`init()` 里 `populateScreen` 才设坐标。因此：

- 所有控件以 `(0,0,0,0)` 构造；
- **内部有相对布局的控件必须覆写 `setX/setY/setWidth/setHeight` 并重算子元素**（基准的 `DetailPanelWidget`、`ActionBarWidget` 已是这个形状，照搬即可）；
- 评审清单里加一条：凡有子元素，四个 setter 缺一不可。

`Scrollbar` 是另一条路径（`setPosition`），且它在 `renderLabels` 阶段绘制、**晚于** children。所以树的 `CONTENT_W = TREE_W - GUTTER(14)` 是必需项，不是可选美化。

### 3.2 需要在 `super.render()` 之后补画的两样

列头（画在 `treeY - HEADER_OFFSET`）与下拉浮层。两者若交给控件自己在 `renderWidget` 里画，会被后续 children 盖住。

### 3.3 右键

AE2 的 `AEBaseScreen.mouseClicked` 在 `button == 1` 且指针落在某个 vanilla child 上时，会把 button 改写成 0 再派发。当前树区域没有 child，正常情况能收到 1；但只要将来在树上方叠任何 vanilla 控件，F13（批量模式右键展开频段）就会静默失效。

绕行：`ConfigModeScreen.mouseClicked` 在调 `super` **之前**拦截树区域右键，直接调 `p2pTree.mouseClicked(mx, my, 1)` 并返回。与基准拦截浮层的写法同形。

验收动作固定为：进入批量模式 → 右键一条频段行 → 应折叠/展开，且左侧选中与检查器不变。

### 3.4 其它 API 差异

| 项 | 1.20.1 写法 | 处理 |
| --- | --- | --- |
| `mouseScrolled` | 3 参（无 `scrollX`） | 删一个形参，逻辑不变 |
| 客户端 tick 事件 | `TickEvent.ClientTickEvent` + `phase == END` | `ClientTickTasks` 搬过来时改事件类型 |
| 配置 spec | `ForgeConfigSpec` / `ModLoadingContext` | `ClientConfig` / `ServerConfig` / `ModConfigs` 按本版写法 |
| 菜单类型 | `IForgeMenuType.create` | 基准的 `IMenuTypeExtension` 改名 |
| `ResourceLocation` | `new ResourceLocation(str)` | 不能用 `ResourceLocation.parse` |
| 语言数据 | `net.minecraftforge.common.data.LanguageProvider` | 两个 datagen provider 改 import |
| 音效 | `AbstractWidget.playDownSound` 是否存在未确认 | 编译一次；不存在则改 `AbstractButton` 或手动播 `SoundEvents.UI_BUTTON_CLICK` |
| 输入框 | 现有 `DetailPanelWidget` 有 `nameField` + `freqField` 两个 | 重写时合并为一个；`getEditFields()` 返回单元素数组 |

### 3.5 同步包

现有实现直接把调用方传入的 5 个 Map 存进字段，Netty 线程写字节与服务端刷新并发时可能抛 CME。基准已在构造时做 `new HashMap<>(...)` 快照，搬运时一并带上。

### 3.6 协议版本

`NetworkHandler` 里 `PROTOCOL_VERSION = "1"`，双向比对。加了 3 个字段后必须升到 `"2"` —— 好处是旧客户端在握手期就被拒绝，而不是进游戏后在读包时崩。

代价是 `1.3.0` 与 `1.2.0` 不互通，需要在 CHANGELOG 与 README 写明。

<!-- contract:begin -->
## 跨版本契约（语义基线）

> 本段在四个仓库中**逐字一致**，任何单仓修改都视为漂移。校验方法见各文档末尾的「契约校验」。
> 凡与本节不一致的实现，视为缺陷，而不是「版本差异」。版本特有能力裁剪写在各自的「适配要点」章节，不写在这里。

### C1 服务端语义（六条，不可改）

1. **双锚点分离。** `p2pTunnelPartBinding`（扫描锚点）与 `pendingBindTarget`（配对源）是两个独立字段。`getBindTarget()` 优先返回 pending，且要求它仍在本次扫描结果内，否则回退到扫描锚点。UI 选中设备只改 pending，不动扫描锚点。

2. **`bindTo(target, freq)` 的固定顺序。** 拒绝 `freq == 0` → 取 grid 与 P2P 服务 → 角色仲裁（`existingInput != null && existingInput != target` 则本机为输出端）→ 设 output → `updateFreq` → `markForSave`。顺序不可交换。

3. **`switchToInput(target)` 必须「先降级全部旧输入端、再提升目标」。** 原因：服务端的 inputs 是以频段为键的映射，先提升会把旧输入端挤出映射，形成「`output == false` 却不在 inputs 里」的孤儿，并被写进存档。降级遍历设备表而非只查 `getInput(freq)`，末尾再用 `getInput(freq)` 兜底一次。

4. **频段未变化时，隧道网络重算必须显式触发。** 各版 `updateFreq` 是否已内部触发不同，由各仓「适配要点」记录实测结论；凡未内部触发者，必须在角色变更后手动调用一次隧道网络变更回调。

5. **`renameP2P` 只写自定义名，不改角色。** 不得走 settings 导入那条路（会顺带执行升级、优先级、配置槽导入，并在带频段数据时把设备设成输出端）。写完必须同时落盘标记与方块更新标记。

6. **`freq == 0` 一律拒绝绑定。** `0000` 表示未绑定，不是可绑定目标。绑定 `0000` 不得改变 `getFrequency()`。

### C2 状态码

16 个码，编号与语义跨版本一致，不得重排。

| 码 | 常量 | 参数 | severity |
| --- | --- | --- | --- |
| 0 | `NONE` | — | NEUTRAL |
| 1 | `BIND_OK` | 频段 | OK |
| 2 | `BIND_NO_TARGET` | — | WARN |
| 3 | `BIND_INVALID_FREQ` | 频段 | WARN |
| 4 | `BIND_NO_GRID` | — | ERROR |
| 5 | `SOURCE_SELECTED` | 设备名 | OK |
| 6 | `ASSIGNED` | 频段 | OK |
| 7 | `RENAMED` | 新名 | OK |
| 8 | `REFRESHED` | — | OK |
| 9 | `RENAME_FAILED` | — | WARN |
| 10 | `BIND_BATCH_OK` | `ok\|freq` | NEUTRAL |
| 11 | `BIND_BATCH_PARTIAL` | `ok\|fail\|freq` | WARN |
| 12 | `SWITCHED_TO_INPUT` | 频段 | OK |
| 13 | `SWITCH_NO_CHANNEL` | 频段 | WARN |
| 14 | `SWITCH_ALREADY_INPUT` | — | WARN |
| 15 | `SWITCH_FAILED` | — | ERROR |

severity 只有四个取值：`NEUTRAL / OK / WARN / ERROR`，用于状态点与状态行取色。**不得把 10 与 11 漏在映射之外**（否则批量成功显示为灰）。

### C3 action 名称与参数格式

名称与参数拼接方式跨版本一致；底层传输层的结构（类型化注册或字符串分发）由各版决定。

| action | 参数 |
| --- | --- |
| `refresh_p2p` | — |
| `update_item_info` | — |
| `bind_frequency` | 频段 hex |
| `bind_frequency_batch` | `"freq;pos;pos…"` |
| `set_channel_alias` | `"freq\|alias"` |
| `set_p2p_alias` | `"pos::alias"` |
| `set_pending_bind` | pos |
| `clear_pending_bind` | — |
| `auto_config_io` | — |
| `assign_freq` | — |
| `switch_to_input` | pos |
| `highlight_p2p` | pos |
| `highlight_p2p_tunnel` | 频段 hex |
| `teleport_p2p` | pos |

位置编码：`x|y|z|dirOrdinal|dimension`。同步包必须承载 `pendingBindPosition`、`statusCode`、`statusParam` 三个字段，参数一律字符串。

### C4 交互语义

1. **滚轮优先级**：下拉浮层打开且指针落在浮层内 → 浮层消费；否则命中树 → 树滚动；否则滚动条；否则不处理。任何一层消费后必须终止本次滚轮事件，不得让第二层再滚一格。
2. **框选**：锚点是**行下标**而非设备身份（同步包会重建行列表）；每次拖动从按下时刻的选择快照重算，因此往回拖可以还原；允许指针越界，但钳位在可见行内。
3. **批量模式退出不清空多选。** 未选中任何设备时，Ctrl 点击非设备行进入批量模式。
4. **高亮流程固定「发包 → 关界面 → 转头」**；界面先关会导致服务端不再接受该容器的 action。
5. **传送必须延后到落地条件成立**（同维度且落点距离在阈值内），超时则放弃后续动作。
6. **就地改名**：Esc 取消、Enter 提交；编辑期间列表刷新不得强制退出编辑态。

### C5 布局常量

三版同值，集中在一份布局常量类中，禁止散落到各控件。

| 区域 | 字段 = 值 |
| --- | --- |
| 面板 | `PANEL_W=440` `PANEL_H=312` |
| 标题 | `TITLE_X=7` `TITLE_Y=7` |
| 配对条 | `PAIR_X=10` `PAIR_Y=20` `PAIR_W=420` `PAIR_H=52` |
| 配对条内部 | `SLOT_Y=2` `SLOT_H=29` `SRC_X=2` `SLOT_W=192` `ARROW_X=196` `ARROW_W=28` `TGT_X=226` `STATUS_Y=36` `DD_MAX_ROWS=7` `DD_ROW_H=14` |
| 搜索 | `SEARCH_X=18` `SEARCH_Y=76` `SEARCH_W=120` `SEARCH_H=13` |
| 树 | `TREE_X=18` `TREE_Y=102` `TREE_W=231` `TREE_H=175` |
| 树列 | `COL_STATUS_W=12` `COL_FREQ_W=40` `COL_SUMMARY_W=52` `GUTTER=14` `CHEVRON_W=9` `RIGHT_PAD=4` `HEADER_OFFSET=10` |
| 滚动条 | `SB_X=236` `SB_Y=102` `SB_H=175` |
| 检查器 | `DETAIL_X=250` `DETAIL_Y=75` `DETAIL_W=179` `DETAIL_H=207` |
| 检查器内部 | `PAD=4` `CHIP_H=10` `ROW_H=14` `BTN_H=18` `BTN_GAP=6` `LABEL_W=44` `CHIPS_Y=+3` `NAME_ROW_Y=+15` `DIVIDER_Y=+29` `ROWS_TOP=+32` `BTN_TOP_GAP=4` `CHANNEL_BAR_EXTRA=18` |
| 检查器行数 | `ROWS_P2P=6` `ROWS_CHANNEL=5` `ROWS_TYPE=3` |
| 操作条 | `ACTION_X=10` `ACTION_Y=288` `ACTION_W=420` `ACTION_H=17` `ABTN_W=76` `ABTN_H=14` `ABTN_GAP=6` `ARIGHT_PAD=4` |
| 树栏位 | `CONTENT_W = TREE_W - GUTTER`；频段列右缘 `= 内容宽 - COL_SUMMARY_W - RIGHT_PAD`；汇总列右缘 `= 内容宽 - RIGHT_PAD` |

参数化项（允许各版不同）：`rowHeight=14`、`indentWidth=10`、`nameMaxLength=14`、`freqColumnWidth=40`。

### C6 数据与格式

1. **频段展示格式**：`%04X`（1.21.1 / 1.20.1 / 1.12.2），`%016X`（1.7.10）。频段宽度为 64 位的版本，列表列内按「别名优先；无别名则画 `…` + 末 6 位」显示，完整值只出现在检查器与 tooltip；搜索始终匹配完整 hex。
2. **通道分组键**：`freq`（1.21.1 / 1.20.1 / 1.12.2），`freq + "|" + p2pType`（1.7.10，允许同频段跨类型）。
3. **角色仲裁对所有权类型一视同仁**，不得对 ME 隧道开例外。
4. **`freq == 0` 不参与任何角色判定、服务映射对齐或通道修复。**
5. **禁止对频段做窄化强转**（`(short)` 一类），比较一律用字符串或与版本对应的整型宽度。

<!-- contract:end -->

---

## 四、阶段与验收

规则：**additive-first**（先加重载/新方法，后续阶段再切调用方）、每阶段结束必须能编译 + 进游戏 + 看得见变化、每阶段写清「若在此停止产品是什么状态」。

| 阶段 | 内容 | 涉及文件 | 验收 | 停止态 | 人日 |
| --- | --- | --- | --- | --- | --- |
| **0** | 协议版本 `"1"`→`"2"`；`.gitignore` 加 `src/generated/resources/.cache/` | `network/NetworkHandler.java`、`.gitignore` | 进服正常；`git status` 不再出现 `.cache` | 与现状等价，旧客户端在握手期被拒 | 0.2 |
| **A** | 服务端语义 + 协议：双锚点、`bindTo`、`switchToInput`、`demoteInputsAt`、`BindResult`/`RoleResult`；同步包 8 字段 + Map 快照；4 个新 action + 状态码 + `syncVersion`；顺手补 `P2PRenderHelper` 的维度判定 | 新增 `p2p/P2PChannelUsage`、`utils/ActionStatus`、`client/gui/P2PTypeNames`、`mixin/AEBasePartAccessor`、`client/ClientTickTasks`；改 `p2p/P2PManager`、`network/ConfigModeSyncPacket`、`menu/ConfigModeMenu`、`client/P2PRenderHelper`、`advanced_memory_card.mixins.json` | 旧界面全功能回归；日志能看到每次操作后的状态码；绑定后重进存档角色不变 | 旧 UI + 完整服务端能力，可单独发补丁 | 2.0–2.5 |
| **B** | 换皮肤与布局：440×312 JSON、新贴图、布局常量类 | `assets/ae2/screens/config_mode_menu.json`、`assets/ae2/textures/guis/config_mode_menu.png`、新增 `client/gui/layout/ConfigUiLayout.java`、改 `ConfigModeScreen.initWidgets` 与滚动条范围 | 面板 440×312；树 231×175、滚动条 x=236、检查器 x=250；**滚动条不压字** | 新皮肤 + 旧功能 | 1.0–1.5 |
| **C** | 树重写（四列/列头/多选/框选/批量/同步节流） | 重写 `client/gui/widgets/P2PTreeWidget.java`；改 `ConfigModeScreen`（`syncVersion` 节流、列头绘制、`mouseDragged` 抢先转发、右键拦截、滚动条偏移同步） | 四列可读；Ctrl 拖动框选；静止 10 秒后 `syncVersion` 只随服务端 2 次/秒增长；搜索过滤正常 | 新树 + 旧检查器，绑定仍走检查器按钮 | 2.5–3.5 |
| **D** | 配对条（含下拉浮层与状态行） | 新增 `client/gui/widgets/PairBarWidget.java`；改 `ConfigModeScreen`（装配、浮层绘制、点击/滚轮分发、多选联动） | 顶部双槽；下拉 7 行可滚；状态行按 severity 变色；叉号清源 | 配对主路径可用（选设备 → 选频段 → 仍需点检查器按钮） | 2.0–2.5 |
| **E** | 检查器 + 操作条 | 新增 `ActionBarWidget`；重写 `DetailPanelWidget`（chips + 6 行属性表 + 就地改名 + 5 格按钮）；`FlatButton` 加三态；改 `ConfigModeScreen`（新回调接口、`doBind`/`doTeleport`/`toggleBatchMode`/`updateActionBar`） | chips 两枚；改名 Esc/Enter 正常；输出端出现「设为输入端」且互换后原输入端变输出；绑定键仅在配对齐全时点亮；回车=绑定；传送跨维度并转头 | **功能完整** | 2.5–3.0 |
| **F** | 配置迁移 + i18n + 文档 + 发版 | `config/ClientConfig`（palette 取代旧分区色）、`config/ServerConfig`（`allowTeleport`）、`config/ModConfigs`（`isTeleportAllowed`）、两个 datagen lang provider、`ru_ru.json`、README/CHANGELOG、`gradle.properties` | 中/英/俄三语完整；旧配置文件升级后颜色不崩 | 可发版 | 1.5–2.0 |

合计 **11.7–15.2 人日**。

### 4.1 三个不可拆的原子单元

| 单元 | 为什么不可拆 | 缓解 |
| --- | --- | --- |
| 同步包字段 + 编解码 | 加字段必然同时改 `encode`/`decode` 与构造 | 保留旧构造（委托成 `null, 0, ""`），编解码一次改完，调用点下阶段再改 |
| 检查器回调接口替换 | 接口一换，`ConfigModeScreen` 的匿名实现立即编译失败 | 一次性改 2 个文件，约 120 行，属 UI 层不碰协议 |
| `initWidgets` 装配 | 新控件必须在 Screen 里 `new` + `add` | 每个新控件单独一个阶段，每次只加一段 |

### 4.2 旧色板兼容

`ConfigModeScreen` / `DetailPanelWidget` / `FlatButton` 现在读 `treeColors`、`panelColors`、`buttonColors`、`panelLayout` 四组。阶段 F 换成 palette 时：

- 保留旧键一个版本周期（标记 deprecated），避免玩家配置升级后整个界面变黑；
- 首次读取时把旧分区色**映射**成新的语义键，而不是丢掉。

---

## 五、风险

| # | 风险 | 依据 | 止损 |
| --- | --- | --- | --- |
| R1 | 协议版本升级导致与 1.2.0 服务端不互通 | `NetworkHandler.PROTOCOL_VERSION` 双向比对 | 阶段 0 先升；文档写明不互通；服务端侧由握手层拒绝而非游戏中崩包 |
| R2 | 右键被 AE2 改写成左键，F13 静默失效 | `AEBaseScreen.mouseClicked` 的 button 改写分支 | Screen 抢在 `super` 前拦截；验收动作固定为「批量模式下右键频段行应折叠」 |
| R3 | 滚动条压住树右边缘 | 滚动条在 `renderLabels` 绘制，晚于 children | 树内容宽扣 14；阶段 B 验收单列该项 |
| R4 | 新控件漏覆写 setter，子元素错位 | 注册定宽高、init 定坐标的两段时序 | 评审清单：凡有子元素必须覆写四个 setter |
| R5 | 只搬 Screen 不搬 Menu，或反之 | Screen 依赖 `menu.getSyncVersion()` 节流 | 阶段 A 先落 Menu 侧，阶段 C 才切 Screen 的节流；两个阶段间允许「Menu 有 syncVersion 但 Screen 仍每 tick 查询」的过渡态 |
| R6 | 打包混入 `.cache/` | `src/generated/resources/.cache/` 未忽略 | 阶段 0 加 `.gitignore` |
| R7 | `allowTeleport` 默认值语义错误 | 基准注释与代码矛盾（见基准文档 B2） | 等基准定真值后再搬；本版不要自行决定 |

---

## 六、测试与验证

### 6.1 每阶段必跑

1. `gradlew build` 通过且无新增 warning。
2. 单机 `runClient`：界面能开、能关、能重开；无异常日志。
3. 专服 `runServer`：启动无客户端类加载错误（重点看 `ClientTickTasks`、`P2PRenderer`）。
4. 重进存档：角色、频段、别名不漂移。

### 6.2 功能矩阵

按契约 C4 与基准文档「五、基准验收」逐条对照。补充本版专属项：

| 用例 | 期望 |
| --- | --- |
| 临时在树控件里打印收到的 button | 右键频段行时打印 `1`（验证 R2 的绕行是否生效） |
| 构建产物中的 refmap | `advanced_memory_card.refmap.json` 与 `mixins.json` 声明一致（当前实际不生成，只会打一条 ERROR 日志，不致命但要确认没有新增错误） |
| 播放按钮音效 | 若 `playDownSound` 不存在，点击不应抛 `NoSuchMethodError` |
| 打开配置界面后按 Esc 再开 | 下拉浮层状态与编辑态不残留 |
| 旧配置文件（含 treeColors 等旧键）升级 | 界面颜色正常，不是全黑 |

### 6.3 待验证

| # | 待验证 | 为什么重要 | 方法 |
| --- | --- | --- | --- |
| V-3 | `AbstractWidget` 是否有 `playDownSound` | 决定 `FlatButton` 能否原样搬 | 一次 `gradlew compileJava` |
| V-4 | `AEBaseScreen` 的右键改写是否真的影响树区域 | 决定 R2 的绕行是否必需 | 控件内临时打印 button 值，右键频段行观察 |

---

## 七、变更记录

| 日期 | 内容 |
| --- | --- |
| 初版 | 建立本版移植文档；确定 P1 优先级、阶段 0/A–F、7 个搬运文件与 4 个重写文件清单 |
| 2026-11-01 | 阶段 0/A/B/C/D/E/F 全部完成，1.3.0-1.20.1 待发布。实施记录与三处偏离见下方「实施备注」 |

### 实施备注

1. **协议版本随格式一起升**，没有按原计划放在阶段 0 独立提交：提前升级会让两个 mod 版本号相同的构建互不兼容，开发期排查成本高。
2. **`ConfigUiLayout` 未单独引入**：1.20.1 的外层槽位仍由 JSON 驱动，此刻新增一个没人读的常量类是死代码。等两老版重写时再作为共用的内部度量来源引入。
3. **`TreeNode.getNodeId` 的缺陷 1.20.1 原本也有**（原计划只记在两老版），阶段 C 一并修复：P2P 节点改为「维度@坐标:朝向」，否则连接状态一变选中就丢。
4. 阶段 B 时 `info` 文本节点被保留（旧 Screen 仍在 `setTextContent("info")`，删了会抛异常），到阶段 E 由操作条取代后一并移除。
5. `client.treeColors` / `panelColors` / `buttonColors` / `panelLayout` 四组旧配置已删除 —— 全部改读 `client.palette.*`，删除前已确认无代码引用。

---

## 契约校验

在四个仓的根目录分别执行（把 `<版本>` 换成 1.21.1 / 1.20.1 / 1.12.2 / 1.7.10）：

```sh
sed -n '/contract:begin/,/contract:end/p' docs/config-ui-porting.md > /tmp/c-<版本>.txt
```

然后 `diff` 任意两份，必须为空。不为空说明有人只改了一个仓的契约段：先判断改动本身是否成立（成立则四仓同步并各自提交），再合并。
