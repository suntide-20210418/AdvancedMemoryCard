# 配置模式 UI 移植文档（1.12.2）

| 项 | 值 |
| --- | --- |
| 基准 | 1.21.1（`AdvancedMemoryCard-1.21.1`，分支 `suntide/1.21.1`） |
| 本版工作副本 | `AdvancedMemoryCard-1.12.2` |
| 分支 | `suntide/1.12.2-1.1.0` |
| 加载器 / Java | Forge 14.23.5.2860 / Java 8 |
| AE2 | Unofficial Extended Life（rv6 系） |
| 版本 | `1.1.0-1.12.2` → 目标 `1.2.0-1.12.2`（已实现，见第八节） |
| 优先级 | **P2**，1.20.1 完成后实施；本版同时负责冻结两老版共用文件 |

---

## 一、范围与目标

### 1.1 与 1.20.1 的本质区别

1.12.2 没有 AE2 的 `StyleManager` / `ScreenStyle` / `WidgetContainer`（那是 AE2 1.16+ 才引入的），`assets/ae2/screens/*.json` 在本版**没有意义**。现有界面是 `GuiContainer` 全自绘，与本版保持一致是唯一合理选择。

因此这次移植不是「搬代码」，而是**按基准的行为规格重写**。基准里大量使用 record、switch 表达式、`var`、模式匹配，本版是裸 Java 8，语法层面就不允许拷贝。

### 1.2 能力范围与降级

全部 18 项能力在本版可实现。三条平台级降级：

| 能力 | 降级方式 | 原因 |
| --- | --- | --- |
| F16 语义色板 | 文字色只能 RGB（alpha 恒 255），矩形色才吃 ARGB | `FontRenderer.drawString` 不接受 alpha |
| F1/F9 布局 | 只能常量硬编码，无 JSON 驱动 | 无 style 体系 |
| F14 类型名 | 类型标识来自部件类名映射，不能用资源定位符 | rv6 的 `IPartItem` 没有 `getId()`，部件也没有 `getPartItem()` |

另有两条**不是降级、但要注意**的实现约束：

- `setOutput` 是包私有，现有代码同时留了反射与 AT 两份保险 —— 用一次编译验证后只保留一条路径（见待验证 V-1）。
- `P2PCache.getInputs/getOutputs` 因 `clz == c` 严格比较不可用，通道成员关系只能靠自家设备表算。**不要**为了对齐基准改成 `getInput() != null`，那会在未上线节点上误判连接状态。

### 1.3 现状缺口

| 类别 | 内容 |
| --- | --- |
| 纯新增 | 配对条、操作条、四列列表、多选/框选、批量绑定、传送按钮、`switchToInput`、状态码通道、语义色板、`syncVersion` 节流 |
| 需重写 | `P2PTreeWidget`(542)、`DetailPanelWidget`(638)、`FlatButton`(88)、`ConfigModeScreen`(381) |
| 服务端缺 | `bindTo`、`switchToInput`、`demoteInputsAt`、双锚点、`BindResult`/`RoleResult` |
| 已有可用 | `bind` 的角色仲裁与 freq 0 拒绝、`assignNewFrequency`、`renameP2P`、`isConnected`、`TeleportCommand` |
| 现状问题 | 每 tick 重建整棵树；同步包无状态码；频段改名是死代码（见四） |

---

## 二、自绘结构设计

### 2.1 不引入控件基类

不做一个 `AbstractWidget` 的仿制品，理由都是可核验的：

1. 没有容器机制可用。唯一的 vanilla 子控件机制是 `GuiContainer.buttonList` + `GuiButton`，而 `GuiButton` 固定 20px 高、用 vanilla 贴图，做不了 chips、行、浮层。
2. Screen 本来就拥有完整的绘制与分派所有权，再加一层只是把直白代码绕一圈。
3. 抽象要跨两套互斥绘图 API（基准的 `GuiGraphics` 与本版的 `FontRenderer` + `GL11`），收益为负。
4. 共享代码不能出现 Java 8 以上语法，抽象能省下的重复本来就少。
5. 本版的 `FlatButton` 已被复制模式复用，改基类会牵连 `CopyModeScreen`。

### 2.2 只加一层极薄的输入接口

```
client/gui/input/InputHandler.java     4 个方法：鼠标按下 / 拖动 / 抬起 / 滚轮
client/gui/input/PointerState.java     指针坐标、按下键、ctrl/shift 采样、手势枚举
client/gui/GuiDraw.java                5 个静态方法：rect / text / textWidth / pushClip / popClip
client/gui/layout/ConfigUiLayout.java  契约 C5 的全部常量
```

有了 `GuiDraw`，两个老版的 widget 方法体可以逐字相同，差异只剩 Screen 层。

**Ctrl 采样统一交给 Screen**：控件不自己判断修饰键，一律读 `PointerState.ctrl`，由 Screen 在派发前刷新（本版用 `GuiScreen.isCtrlKeyDown()`）。这样多选判定逻辑三版同构，也消除了基准里「从控件反查 `Minecraft.screen`」在 `screen == null` 时的隐患。

### 2.3 分派顺序（固定）

```
1. 下拉浮层（若打开且命中）
2. 树
3. 配对条本体
4. 检查器
5. 操作条
6. 滚动条拖动
7. super
```

滚轮同理：浮层 → 树 → 滚动条 → 不处理。任一层消费后必须终止本次事件，否则会出现「浮层滚一格、树也滚一格」。

本版滚轮入口是 `handleMouseInput` + `Mouse.getEventDWheel()`，把鼠标坐标换算后交给统一入口，命中浮层或树则提前 `return`（不再调 `super.handleMouseInput()`）。

已知副作用：提前 return 会跳过一次 `Mouse.next()` 循环，同一帧后续鼠标事件延后到下一 tick。现有代码已有此行为，影响远小于「树与浮层同时滚动」，记录在案不修。

### 2.4 鼠标模型映射

| 宿主回调 | 映射到 |
| --- | --- |
| `mouseClicked(x,y,button)` | `handleMouseDown` |
| `mouseClickMove(x,y,button,time)` | `handleMouseDrag`（无增量参数，但框选本来只用坐标算行下标，改造成本为零） |
| `mouseReleased(x,y,button)` | `handleMouseUp` |
| `handleMouseInput` + `getEventDWheel` | `handleMouseWheel`（符号化为 ±1） |

---

## 三、两老版共用文件（冻结清单）

以下文件与本版 1.7.10 版**逐字相同**，以本版为权威副本（1.7.10 从本版复制，然后跑 spotless）：

`InputHandler`、`PointerState`、`ConfigUiLayout`、`GuiDraw`、`PairBarWidget`、`ActionBarWidget`、`FlatButton`、`P2PChannelUsage`、`TreeNode`、`ActionStatus`、`NodeType`。

允许不同（但类名、方法签名、常量字段名必须一致）：`ConfigModeScreen`、`P2PTreeWidget`（频段列文本与通道键）、`DetailPanelWidget`（输入框字段名）、`P2PTypes`、`TranslateHelper`、`ClientConfig`、`P2PManager`、`ConfigModeMenu`、`ConfigModeSyncPacket`。

跨仓校验：

```
for f in <清单>; do diff -u "$f" "../AdvancedMemoryCard-1.7.10/$f" || echo "DRIFT: $f"; done
```

---

## 四、阶段 0：开工前必须先修的模型缺陷

这三处不是移植引入的，是既有缺陷，但会让新 UI 直接表现错误。必须**独立提交、先发补丁版**。

### 4.1 `P2PPosition` 的相等性忽略维度

`P2PPosition` 有 `dimension` 字段，但 `equals`/`hashCode` 只比较坐标与朝向。

后果：新 UI 用 `Set<P2PPosition>` 做多选集合，并用 `p2pInfoMap.get(key)` 反查设备。主世界与下界同坐标的两台设备会被当成同一台 —— Ctrl 多选永远只选中一台，批量绑定会漏掉另一台。

修法：`equals`/`hashCode` 加入 `dimension`。

验收：主世界与下界同坐标各放一台 P2P，Ctrl 多选应为 2 台。

### 4.2 `TreeNode.getNodeId()` 用 `hashCode()` 做标识

P2P 节点的 id 拼的是 `p2pInfo.hashCode()`，而它是 14 个字段的全量哈希 —— 连接状态一变，id 就变。

后果：每次同步包重建树后，之前选中的节点「找不到」，表现为改名后详情面板自己跳回未选中、跟随选中反复滚动。

修法：改成「维度 + 坐标 + 朝向下标」构成的稳定字符串。

验收：连接状态变化（插拔输出端）后，选中项不丢。

### 4.3 频段改名是死代码

`DetailPanelWidget.onNameConfirm()` 只判 `selectedP2PInfo != null`；`onChannelAliasConfirm()` 虽然写好了，但 `keyTyped` 的 Enter 分支只调前者。

后果：选中频段节点改名 → 回车 → 输入框关闭，什么也没发生。

修法：照 1.7.10 同名文件的分派写法（先判节点类型是 CHANNEL 则走频段别名路径）。

验收：选中频段节点改名，别名生效且重进存档仍在。

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

## 五、阶段与验收

| 阶段 | 内容 | 涉及文件 | 验收 | 停止态 | 人日 |
| --- | --- | --- | --- | --- | --- |
| **0** | 模型缺陷修复（见第四节），单独提交、先发补丁版 | `p2p/P2PPosition`、`p2p/TreeNode`、`client/gui/widgets/DetailPanelWidget` | 频段别名能改；插拔输出端后选中不丢；跨维度同坐标可多选为 2 台 | 修 bug 的补丁版 | 0.5 |
| **A** | 契约层脚手架（纯新增，无调用方） | `client/gui/input/InputHandler`、`PointerState`、`client/gui/GuiDraw`、`client/gui/layout/ConfigUiLayout`、`client/gui/P2PTypes`、`p2p/P2PChannelUsage`、`utils/ActionStatus` | 编译通过，界面不变 | 基础设施就位（含 1.7.10 可复用的冻结文件） | 1.0–1.5 |
| **B** | 服务端语义 + 协议 | `p2p/P2PManager`（双锚点、`bindTo`、`switchToInput`、`demoteInputsAt`、`BindResult`/`RoleResult`、通道修复）、`client/gui/menu/ConfigModeMenu`（4 个新 action + 状态码）、`network/ConfigModeSyncPacket`（+3 字段 + 格式版本前置 int + Map 快照）、`ConfigModeScreen`（读 syncVersion） | 旧界面照常；日志有状态码；旧客户端连新服务端被提示而非崩包 | 旧 UI + 新服务端能力 | 3.0–4.0 |
| **C** | 换皮肤（440×312 + 新贴图） | `ConfigModeScreen` 常量改用 `ConfigUiLayout`、`assets/advanced_memory_card/textures/gui/config_mode_menu.png` | 新底板可见；旧控件不越界 | 新皮肤 + 旧功能 | 0.5–1.0 |
| **D** | 树重写（实现 `InputHandler`） | 重写 `client/gui/widgets/P2PTreeWidget`；改 `ConfigModeScreen`（三入口、浮层优先的滚轮、删每 tick 重建、滚动条绘制） | 四列、列头、框选、多选、批量模式、搜索、滚动条拖动 | 新树 + 旧检查器 | 3.0–4.0 |
| **E** | 配对条 | 新增 `client/gui/widgets/PairBarWidget`；改 `ConfigModeScreen` | 双槽、下拉 7 行、状态行配色、叉号清源 | 配对主路径可用 | 2.0–3.0 |
| **F** | 检查器 + 操作条 | 重写 `DetailPanelWidget`（chips + 属性表 + 就地改名 + 5 格按钮）、`FlatButton`（三态，保留旧构造签名）；新增 `ActionBarWidget`；改 Screen 回调 | 同 1.20.1 阶段 E；另加「设为输入端后退出重进角色保留」 | 功能完整 | 2.5–3.5 |
| **G** | 收尾 | `config/ClientConfig`（palette）、三份 `.lang`（+约 45 键）、README/CHANGELOG、`gradle.properties` | 三语键集合一致；旧配置升级颜色不崩 | 可发版 | 2.0–2.5 |

合计 **14.5–20.0 人日**。

### 5.1 不可拆的原子单元

| 单元 | 原因 | 缓解 |
| --- | --- | --- |
| 同步包字段 + 编解码 | 加字段必然改读写顺序 | 保留旧构造重载；格式版本号前置，读到不一致就丢弃并提示，不抛异常（抛会断连接） |
| 检查器回调接口 | 接口一换，Screen 匿名实现立即编译失败 | 一次性改 2 个文件 |
| Screen 的控件装配 | 新控件必须在 Screen 里 `new` | 每阶段只加一段 |

### 5.2 `FlatButton` 三态改造的兼容约束

复制模式界面复用 `FlatButton`。加 `Variant` 时必须保留现有构造签名与 `visible` / `active` 字段语义，`CopyModeScreen` 不得因这次改造而需要改动。

---

## 六、风险

| # | 风险 | 依据 | 止损 |
| --- | --- | --- | --- |
| R1 | `switchToInput` 的降级逻辑改写老存档里的「双输入端」通道 | rv6 的 inputs 以频段为键，后登记覆盖前者 | ① 仅在「目标当前是输出端且频段非 0」时降级；② 降级后回读 `getInput(freq) == target` 校验，不等则返回 `SWITCH_FAILED` 且不继续；③ 变更前打一条 INFO 日志记录每台角色；④ **打开界面时不跑通道修复**，只跑「空缺时才补登」的分支 |
| R2 | 多选集合跨维度错并 | `P2PPosition` 相等性缺维度 | 阶段 0 修 + 专项验收 |
| R3 | AT 与反射并存，不清楚哪条在生效 | `build.gradle` 配了 AT，`P2PManager` 又走反射 | 先用一次编译验证（V-1），确认后**只保留一条路径**，不要长期双保险 |
| R4 | 协议无格式版本，新旧端连上后读包错乱 | 同步包按字段顺序硬编码读写 | 格式版本前置 int；不一致丢弃 + 聊天栏提示，不 throw |
| R5 | 每 tick 重建树造成 CPU 浪费与选中抖动 | `ConfigModeScreen` 的 `updateScreen` 无条件调重建 | 阶段 D 换 `syncVersion`；验收：静止 10 秒，计数只随服务端 2 次/秒增长 |
| R6 | 把状态点判定改成 `getInput() != null` | 想「对齐基准」时最容易犯的错 | 保持本版自扫设备表的 `isConnected`，写进代码注释 |
| R7 | 频段改名的包体积或副作用 | `set_channel_alias` 走 `"freq|alias"` 拼串，别名里若含 `|` 会解析错 | 服务端按**第一个** `|` 分割；客户端提交前过滤 `|` |
| R8 | 旧 AT 文件在字段已 public 后变成噪音 | 冗余配置长期存在会误导维护者 | V-1 确认后一并删除 AT 与反射 |

---

## 七、测试与验证

### 7.1 每阶段必跑

1. `gradlew build` 通过。
2. 单机 `runClient`：界面能开、能关、能重开。
3. 专服 `runServer`：启动无客户端类加载错误。
4. 重进存档：角色、频段、别名不漂移。

### 7.2 功能矩阵

按契约 C4 与基准文档「五、基准验收」逐条对照。补充本版专属项：

| 用例 | 期望 |
| --- | --- |
| 主世界与下界同坐标各放一台，Ctrl 多选 | 选中 2 台（验证 `P2PPosition` 修复） |
| 插拔输出端后观察详情面板 | 仍停留在原设备（验证 nodeId 修复） |
| 选中频段节点改名 → 回车 | 别名生效（验证 4.3） |
| 设为输入端 → 退出界面 → 重进存档 | 角色保留（验证落盘） |
| 频段里带 `|` 的别名 | 服务端按第一个 `|` 分割，不会串位 |
| 快速连续滚轮（浮层打开） | 只有浮层滚动，树不动 |
| 拖动框选越出树矩形 | 仍然延伸，抬手后一次性提交 |
| 关闭复制模式界面 | 不受 `FlatButton` 改造影响 |

### 7.3 待验证

| # | 待验证 | 为什么重要 | 方法 |
| --- | --- | --- | --- |
| V-1 | `setOutput` 是否纯字段写、`output` 字段能否直接赋值 | 决定能否删掉 AT + 反射双保险（R3/R8） | `javap -p -c` 读字节码；写一行 `part.setOutput(true)` 编译；再进游戏改角色并重进存档确认落盘 |
| V-2 | `P2PCache.updateFreq` 是否内部已落盘与触发网络重算 | 决定 `switchToInput` 是否需要手动补这两步（契约 C1 第 4 条） | `javap -p -c` 读 `updateFreq`；再做一次「设为输入端 → 不上线 → 重进存档」 |

---

## 八、变更记录

| 日期 | 内容 |
| --- | --- |
| 初版 | 建立本版移植文档；确定 P2 优先级、阶段 0（三处模型缺陷）/A–G、两老版冻结文件清单 |
| 2026-11-01 | 阶段 0（`427464f`）、服务端 B 上半（`43d423c`）/下半（`3552cca`）、A（`83f1fc7`）、译名回显修正（`00c8d7a`）完成 |
| 2026-11-01 | 阶段 C 皮肤 / D 树 / E 配对条 / F 检查器与操作条完成，并发布 `1.2.0-1.12.2`（tag 已推送，触发 release 工作流）。实施记录与偏离见下方「实施备注」 |
| 2026-11-01 | 全程没有实机视觉验证：进游戏项（批量绑定状态行、框选、滚轮、跨维度传送）只在代码层核对过，CI 通过不等于界面观感通过 |
| 2026-11-01 | 未发版（待批准）：树整块不可点击。`P2PTreeWidget.onClick` 没有调用者（基准靠 AE2 `AbstractWidget#mouseClicked` 派发，旧版 `Widget` 基类直接返回 false），现由树的 `mouseClicked` 自己派发；滚动条判定同时挪到树之前，避免滑块拖动被树的命中吃掉（见实施备注 8） |
| 2026-11-02 | 高亮流程补齐关界面：`onHighlightP2P` / `onHighlightChannel` 现在按 C4 执行「发包 → 关界面 → 转头」，频段高亮转向该频段第一台输入端；此前只发包，GUI 不会关闭 |
| 2026-11-02 | UI 贴图从 1.21.1 原样复制（`config_mode_menu.png` 7538 字节，md5 `0a4cb6e7bcc09d2fdcb4dfcc94c1f639`），面板与检查器不再用 `fillRect` 画矩形；搜索框关闭原版黑底并补占位符 |
| 2026-11-02 | 翻译键补齐：39 条双前缀 / 树键修复；`ru_ru.lang` 从 78 键补到 123 键，三语键集合一致、占位符逐条对齐 |
| 2026-11-02 | 实机确认：高亮描边、传送位移、传送后点亮、批量多选等主路径；据此修复了高亮不关 GUI、首次点击取消选中、批量只绑一台等交互问题 |

### 实施备注

1. **实际执行顺序与规划相反**：1.7.10 线先跑到阶段 G，本版的 C/D/E/F 是在 1.7.10 之后按它的实现回补的。控件（`PairBarWidget`、`ActionBarWidget`、重写的 `DetailPanelWidget`）先写好、Screen 后接线，中途工作树一度编译不过（旧 `ConfigModeScreen` 仍在调 `detailPanel.draw(FontRenderer,int,int)` 与 `setPendingBindP2P(...)`，新控件已换成 `render(double,double,float)` 与 `setSelectedNode(...)`）。
2. **皮肤改用基准贴图（2026-11-02 更新）**：面板不再走 `GuiDraw.fillRect(...0xFFC7CCDA)`，改为 `Gui.drawModalRectWithCustomSizedTexture` 铺 `textures/gui/config_mode_menu.png`（512×512，取左上 440×312）；检查器的 `paletteSurface` 填充移除；搜索框关闭原版黑底并补占位符，标题对齐 (7,7)。原记录中的「保持旧图、不读贴图」已作废。440×312 的槽位数值仍来自 `ConfigUiLayout`，与基准 JSON 对齐。
3. **平台差异**：频段用 `%04X` 四位十六进制直出，不做 1.7.10 的「`…` + 末 6 位」降级（4 位放得下 40px 列宽）；通道 map 在本版以**纯频段**为键（`client/gui/menu/ConfigModeMenu.java:420` `channelInfoMap.put(frequency, channelInfo)`），因此不存在 1.7.10 那条 `ChannelInfo.find(freq, type)` 的键不匹配问题；`mouseReleased(int,int,int)` 取代 `mouseMovedOrUp`；Java 8 语法（无 var / record / 箭头 switch）。
4. **主动偏离**：树新增 `mouseScrolled` 覆写（滚轮一格三行）——1.7.10 那份实现里滚轮在树上是失效的，只能拖滚动条；本版把两条路径都留了。
5. **语言文件是小写** `en_us.lang` / `zh_cn.lang`（不是 1.7.10 的 `en_US` / `zh_CN`），各 113 键、键集合与 1.7.10 对拍零缺失；`ru_ru.lang` 未补（本就缺这些键且编码异常），按「只保证 en/zh」处理。
6. **本仓没有 spotless**：`build.gradle` 只有 `net.minecraftforge.gradle` / `eclipse` / `idea` / `maven-publish` 四个插件，格式门禁实际只有 javac 一道；验收以 `clean compileJava` 通过为准。
7. **无实机视觉验证**：全程只做编译级验证，未启动客户端；`build`（reobf / 资源打包）与 `runClient` 未跑，§7.3 的 V-1/V-2 仍未实测。
8. **控件的输入要靠 Screen 自己派发（树点不动的根因）**：基准的控件继承 AE2 `AbstractWidget`，`mouseClicked` 里先判命中、再调 `onClick`，由 `WidgetContainer` 统一分发；本版 `Widget.mouseClicked` 只 `return false`，`P2PTreeWidget.onClick(double,double)` 因此没有任何调用者，整棵树（设备行、频段行、Ctrl 多选、框选起手）全是死区。现在 `P2PTreeWidget.mouseClicked` 自己派发（左键命中矩形就调 `onClick` 并返回 `true`）。另外滚动条的矩形与树右缘的 14px 留白重叠，派发顺序必须是「滚动条 → 树」。**以后给旧版控件加交互，先确认这条路径上到底有没有调用者。**

---

## 契约校验

在四个仓的根目录分别执行（把 `<版本>` 换成 1.21.1 / 1.20.1 / 1.12.2 / 1.7.10）：

```sh
sed -n '/contract:begin/,/contract:end/p' docs/config-ui-porting.md > /tmp/c-<版本>.txt
```

然后 `diff` 任意两份，必须为空。不为空说明有人只改了一个仓的契约段：先判断改动本身是否成立（成立则四仓同步并各自提交），再合并。
