# 配置模式 UI 移植文档（1.7.10 / GTNH）

| 项 | 值 |
| --- | --- |
| 基准 | 1.21.1（`AdvancedMemoryCard-1.21.1`，分支 `suntide/1.21.1`） |
| 本版工作副本 | `AdvancedMemoryCard-1.7.10` |
| 分支 | `suntide/1.7.10-GTNH` |
| 加载器 / Java | GTNH Forge 10.13.4.1614 / JDK 25 + jabel（编译到 Java 8 字节码） |
| AE2 | GTNH rv3 fork |
| 版本 | `1.1.0-1.7.10-GTNH` → `1.2.0-1.7.10-GTNH`（已实现，见第七节） |
| 优先级 | **P3**，1.12.2 的契约层与树完成后实施（实际执行顺序与文档规划相反，见第七节第 2 条） |

---

## 一、范围与目标

### 1.1 与 1.12.2 的关系

本版与 1.12.2 在配置模式上是**同构代码**（同一套自绘体系、同一份布局意图）。移植策略因此不是「再重写一遍」，而是：

1. 先由 1.12.2 写出并冻结一份共用实现（清单见 1.12.2 文档第三节）；
2. 本版复制过来，**只改四个平台特化点**（见第二节），再跑一次 `spotlessApply`。

这样本版的 UI 阶段从「重写」降级为「复制 + 调参」，但前提是 1.12.2 的那两个阶段确实完成了 —— 顺序不可颠倒。

### 1.2 能力范围与降级

18 项能力全部可实现。四条平台级降级：

| 能力 | 降级方式 | 原因 |
| --- | --- | --- |
| F2「新建频段」 | 生成随机 long，玩家看到的频段是 16 位十六进制 | rv3 的 `P2PCache` **没有** `newFrequency()`，只能用自家 `generateUniqueFrequency()` |
| F7 频段列 | 别名优先；无别名画 `…` + 末 6 位 | 16 位 hex 约 96px，40px 列放不下 |
| F11 框选收尾 | 用 `mouseMovedOrUp(button >= 0)` 判定释放 | rv3 的 `GuiScreen` **没有** `mouseReleased` |
| F16 语义色板 | 文字色只能 RGB | 同 1.12.2 |

另有一条**不是降级但必须保留**的历史包袱：`ConfigModeMenu` 有 36 个离屏背包槽位（放在 -10000）。这是为了规避 1.7.10 零槽位容器在 `NetHandlerPlayServer` 上的空指针，属**禁改区**。

### 1.3 rv3 与 rv6 的差异点

| 项 | 1.12.2（rv6） | 1.7.10（rv3） |
| --- | --- | --- |
| 频段类型 | `short` | **`long`** |
| 频段格式 | `%04X` | `%016X` |
| 通道分组键 | 频段 | 频段 + `"\|"` + 类型（允许同频段跨类型） |
| 新建频段 | `newFrequency()` | 自家 `generateUniqueFrequency()` |
| 唯一输入端 | 自家设备表 | `P2PCache.getInput(long)` 可用 |
| 设输出端 | 反射 + AT | 反射（`output` 字段是 public，见 V-1） |
| 改名 | `setCustomName` + 落盘 | **必须 `setCustomNameInternal`**（rv3 的 `setCustomName` 会广播给同频段所有设备） |
| 释放钩子 | `mouseReleased` | `mouseMovedOrUp`（`button == -1` 表示纯移动） |
| 网络线程 | `addScheduledTask` 调度 | **handler 直接在 netty 线程执行** |
| 打开界面 | `openGui` 可带 hand 序号 | 只能传 3 个 int，靠 `pendingManagers` 侧信道 |
| 构建 | FG3 | GTNHGradle + **spotless 强制格式** |

### 1.4 现状缺口

| 类别 | 内容 |
| --- | --- |
| 复用 1.12.2 | 输入接口、指针状态、`GuiDraw`、布局常量、配对条、操作条、`FlatButton`、`ActionStatus`、`P2PChannelUsage`、`TreeNode` |
| 本版特化 | `P2PTreeWidget`（通道键 + 频段列文本 + 排序）、`DetailPanelWidget`（输入框字段名）、`P2PManager`、`ConfigModeMenu`、`ConfigModeSyncPacket` |
| 已有可用 | 频段就地改名（本版已正确，1.12.2 的反而是坏的）、三列按钮网格、搜索、跟随选中 |
| 现状缺陷 | `bind` 对 ME 跳过仲裁、不拒绝 freq 0；释放钩子误触发按钮（见第四节） |

---

## 二、频段显示规则（已定）

频段是 64 位，`%016X` 后 16 个字符。列表列宽保持 40px（与另两版一致），显示按以下优先级：

1. **有别名** → 画别名，超出按像素截断并补 `…`；
2. **无别名** → 画 `"…" + 末 6 位`，例如 `…A3F019`；
3. **完整值**只出现在检查器的「频段」行与 tooltip；
4. **搜索始终匹配完整 hex**，不匹配显示后的短串。

这样处理的代价是：列表里两个都未命名、末 6 位又恰好相同的频段无法区分。概率约为 1/16^6，可接受；若真的撞上，检查器与 tooltip 仍能给出完整值。

排序必须在 `long` 域内进行（`Long.parseLong(hex, 16)`），不能用 `int`，否则高位溢出导致排序错乱 —— 本版现有代码已有此处理，不要改回。

---

## 三、阶段 0：开工前必须先修

四处缺陷，独立提交、先发补丁版。

### 3.1 `bind` 的三处语义缺失

本版 `bind` 与基准相比缺三件事：

1. **对 ME 隧道跳过角色仲裁。** 基准对所有类型一视同仁（契约 C6 第 3 条）。跳过仲裁的后果是：把第二台 ME P2P 绑到已有通道时，只会再登记一个输入端，输出端映射永远为空，整条通道永久显示未连接，且这个坏状态会被写进存档。
2. **不拒绝 `freq == 0`。** `0000` 表示未绑定，把它当绑定目标等于静默解绑。
3. **缺少显式落盘。** 需要确认 `updateFreq` 内部是否已落盘（V-2）；若没有，必须补。

### 3.2 `P2PPosition` 的相等性忽略维度

同 1.12.2 文档 4.1：新 UI 的多选集合与设备反查会跨维度错并。本版坐标用的是自建 `BlockPos`，修法相同 —— `equals`/`hashCode` 加入 `dimensionId`。

### 3.3 `TreeNode.getNodeId()` 用 `hashCode()`

同 1.12.2 文档 4.2。本版没有 `BlockPos.asLong()`，用字符串拼接即可：`"p2p:" + dimensionId + "@" + x + "," + y + "," + z + ":" + ordinal`。

### 3.4 释放钩子误触发按钮

`ConfigModeScreen.mouseMovedOrUp` 无条件调 `detailPanel.mouseReleased(mouseX, mouseY, mouseButton)`，而 `mouseButton` 为 -1 时表示「纯移动」。`FlatButton` 的 `mouseReleased` 又不看 button 参数。

后果：按住左键从按钮上移开再移动一下，就触发该按钮的动作。这在 1.21.1 / 1.12.2 上不存在（它们有独立的 `mouseReleased`），是本版特有的。

修法：只在 `button >= 0` 时派发 Up；`button == -1` 只刷新指针位置。`FlatButton.mouseReleased` 再补一道 `if (button != 0) return false;`。

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

| 阶段 | 内容 | 涉及文件 | 验收 | 停止态 | 人日 |
| --- | --- | --- | --- | --- | --- |
| **0** | 模型与交互缺陷修复（见第三节），单独提交、先发补丁版 | `p2p/P2PPosition`、`p2p/TreeNode`、`p2p/P2PManager`、`client/gui/screen/ConfigModeScreen`、`client/gui/widgets/FlatButton` | 按住左键从按钮滑出不再触发；插拔输出端后选中不丢；跨维度同坐标可多选为 2 台 | 补丁版 | 0.5–1.0 |
| **A** | 引入 1.12.2 冻结文件 + `spotlessApply`（**单独一个 `style:` 提交**） | 复制清单见 1.12.2 文档第三节；本版 `GuiDraw` 用 `Gui.func_146110_a` 版本 | 编译通过，界面不变 | 基础设施就位 | 1.0–1.5 |
| **B** | 服务端语义 + 协议（rv3 特化） | `p2p/P2PManager`（双锚点、`bindTo`、`switchToInput`、`demoteInputsAt`、`BindResult`/`RoleResult`、通道修复、`assignNewFrequency` 去掉「先写 0」、保留 `setCustomNameInternal`）、`menu/ConfigModeMenu`、`network/ConfigModeSyncPacket`、`network/ConfigModeActionPacket`（**整段包进 `MinecraftServer.getServer().addScheduledTask`**） | 旧界面照常；批量绑定 6 台不崩；角色改动重进保留；生成的新频段在树里显示正常 | 旧 UI + 新服务端能力 | 3.0–4.0 |
| **C** | 换皮肤（440×312） | `ConfigModeScreen` 常量改用 `ConfigUiLayout`（保留画布尺寸 512）、`assets/advanced_memory_card/textures/gui/config_mode_menu.png` | 新底板可见；旧控件不越界 | 新皮肤 + 旧功能 | 0.5–1.0 |
| **D** | 树（复制 1.12.2 版，改三处） | `client/gui/widgets/P2PTreeWidget`：① 通道键改 `freq\|type`；② 频段列按第二节规则；③ 排序保持 `Long.parseLong`；改 Screen 三个入口 | 16 位频段显示 `…A3F019`，完整值在检查器；跨类型同频段分组正确；框选/滚轮/搜索正常 | 新树 + 旧检查器 | 3.0–4.0 |
| **E** | 配对条 | 复制 `PairBarWidget`，改下拉项标签与槽内文本预算 | 同 1.20.1 阶段 D；下拉项显示短频段 | 配对主路径可用 | 2.5–3.0 |
| **F** | 检查器 + 操作条 | 复制 `ActionBarWidget`；重写 `DetailPanelWidget`（**保留本版已正确的频段改名分派**）、`FlatButton`；改 Screen 回调 | 功能完整；**打开界面不崩**（证明离屏槽位未被删） | 功能完整 | 2.5–3.5 |
| **G** | 收尾 | `config/ClientConfig`（palette）、`en_US.lang` / `zh_CN.lang`、README、`gradle.properties`、`docs/`、`spotlessApply` 单独提交 | 两语键集合一致；`spotlessCheck` 通过 | 可发版 | 2.0–3.0 |

合计 **15.0–21.0 人日**。

### 4.1 spotless 提交策略

GTNH 约定会强制重排从 1.12.2 复制来的文件（import 顺序为 `java/javax/net/org/com`，与 1.12.2 的 `com/net/...` 不同）。策略：

1. 复制文件后立刻 `spotlessApply`，**单独提交一次**，提交前缀 `style:`；
2. 业务改动另起提交；
3. 审查时只看逻辑提交（`git show -w` 跳过格式噪声）。

不这样做的话，一个「复制 10 个文件」的提交会变成几百行格式差异，把真正的改动淹掉。

### 4.2 语言文件约束

本版语言文件**没有** `#PARSE_ESCAPES`，多行文案必须写成 `\n` 并由 `TranslateHelper` 手工替换。新增键（约 45 个）落在 `en_US.lang` 与 `zh_CN.lang` 两份，键集合必须一致。

---

## 五、风险

| # | 风险 | 依据 | 止损 |
| --- | --- | --- | --- |
| R1 | netty 线程直接改世界（批量绑定一次动多台） | `ConfigModeActionPacket` 的 handler 无调度 | 整段包进 `addScheduledTask`；task 内部重新取容器并校验仍是本模组容器（防止执行时界面已关） |
| R2 | 误用 `setCustomName` 导致整个频段一起改名 | rv3 的 `setCustomName` 会传播 | 保留 `setCustomNameInternal` 反射；改完回读校验，不一致报 `RENAME_FAILED` |
| R3 | 频段被窄化强转 | 64 位频段 | 比较一律字符串或 `long`；格式化集中在通道键工具里；代码评审专门看 `(short)` |
| R4 | 离屏背包槽位被误删 → 打开界面崩 | 零槽位容器的历史问题 | `ConfigModeMenu` 的槽位段列为禁改区；阶段 F 验收含「打开界面不崩」 |
| R5 | spotless 一次性重排淹没 review | GTNH 约定 | 见 4.1 |
| R6 | 框选在无 `mouseReleased` 下漏收尾 | 只有 `mouseMovedOrUp` | 只在 `button >= 0` 派发 Up；阶段 0 先修误触发，阶段 D 再实现框选 |
| R7 | 下拉浮层滚动与树滚动双重消费 | `handleMouseInput` 是全局回调 | 命中浮层即 `return`，不调 `super` |
| R8 | 批量绑定数量过大导致单 tick 卡顿 | 一次任务里改多台设备的网格状态 | 客户端限制单次批量上限；服务端按顺序逐台处理，不做嵌套跨网格操作 |

---

## 六、测试与验证

### 6.1 每阶段必跑

1. `gradlew build` 通过；`spotlessCheck` 通过。
2. 单机 `runClient`：界面能开、能关、能重开。
3. 专服 `runServer`：启动正常，proxy 分流正确。
4. 重进存档：角色、频段、别名不漂移。

### 6.2 功能矩阵

按契约 C4 与基准文档「五、基准验收」逐条对照。补充本版专属项：

| 用例 | 期望 |
| --- | --- |
| 生成一个新频段 | 树里显示 `…` + 末 6 位；检查器显示完整 16 位 |
| 同频段、不同类型的两组通道 | 分组正确（通道键含类型） |
| 按住左键从按钮上滑出再移动 | 不触发该按钮（验证阶段 0） |
| 批量模式右键频段行 | 折叠/展开，选中不变 |
| 批量绑定 6 台 | 不崩、不丢台；状态行 `BIND_BATCH_OK` |
| 设为输入端 → 退出 → 重进 | 角色保留 |
| 同频段其它设备的名字 | 改名一台时不被连带修改 |
| 打开配置界面 | 不显示背包槽位、不崩（离屏槽位仍在） |
| 两语 `.lang` 键集合 | 完全一致 |
| `spotlessCheck` | 通过 |

### 6.3 待验证

| # | 待验证 | 为什么重要 | 方法 |
| --- | --- | --- | --- |
| V-5 | 零槽位容器是否真的触发 `NetHandlerPlayServer` 空指针 | 离屏槽位这个 hack 的依据只有代码注释，没有实测记录 | 临时注释掉槽位段，单机开界面一次、专服开一次，观察是否复现；确认后把结论写回本文档 |
| V-6 | `MinecraftServer` 是否有可用的任务调度入口 | 决定批量绑定的线程安全写法（R1） | 在反编译产物里检索；编译一次；用一个 `LOGGER` 打印当前线程名确认是否已切主线程 |

补充：`output`/`freq` 在 rv3 是 **public 字段**，理论上可以直接赋值而不用反射。但为保持与 1.12.2 同构，建议保留一个包装方法（内部可改为直接赋值），不要在两版写出两套不同的访问方式。

---

## 七、变更记录

| 日期 | 内容 |
| --- | --- |
| 初版 | 建立本版移植文档；确定 P3 优先级、频段显示规则、阶段 0（四处缺陷）/A–G、spotless 提交策略 |
| 2026-11-01 | 阶段 0（`8619b75`）、自绘脚手架（`38b1d60`）、服务端语义与协议（`8ca5770`）、四列树（`a061228`）、界面总装（`2bd55ab`）全部完成。`2bd55ab` 把业务改动与 `spotlessApply` 放在同一提交，没有按 §4.1 拆成 `style:` + 业务两个提交 |
| 2026-11-01 | 阶段 G 收尾：`README.md` / `README_en.md` 重写为 1.7.10 真实内容（原来是从 1.20.1 复制的错误版本）、新建 `CHANGELOG.md`、补 `shortFrequency()` 与频道键查找修复（见实施备注 3、4）、两语键集合一致（各 109 键）、`spotlessCheck` 通过、发版 `1.2.0-1.7.10-GTNH` |

### 实施备注

1. **皮肤是自绘的，没有换贴图**：`assets/advanced_memory_card/textures/gui/config_mode_menu.png` 仍是旧图（7399 字节）。440×312 面板由 `ConfigModeScreen.drawScreen` 里的 `GuiDraw.fillRect(guiLeft, guiTop, PANEL_W, PANEL_H, 0xFFC7CCDA)` 加标题自绘，不读贴图资源。阶段 C 写的是「新底板可见」，按自绘实现了，美术资产替换没有做；将来要换图，改这一处绘制即可。
2. **实际执行顺序与规划相反**：§1.1 要求先由 1.12.2 冻结共用实现、本版再复制过来；实际本版先做完了，1.12.2 至今仍卡在屏幕接线。所以本版不是「复制 1.12.2」，而是与 1.21.1 基准同构重写。1.12.2 后续应以本版（尤其 `P2PTreeWidget` 的四列/框选/批量与 `ConfigModeScreen` 的回调装配）为参照回补。
3. **频段列此前会溢出**：树里直接画完整 16 位十六进制，而频段列只有 40px（`COL_FREQ_W`），文字会压到名称与汇总列。已补 `P2PTreeWidget.shortFrequency()`：宽度超一屏就只留 `…` + 末 6 位，完整值在检查器看，符合 §1.2 的 F7 降级。
4. **频道键查找缺失（本次修复）**：`ConfigModeMenu` 的 `channelInfoMap` 以 `frequency|p2pType` 为键（`menu/ConfigModeMenu.java:474`），但树与检查器原先按纯频段查（`channelInfoMap.get(freq)`），永远命中不了 —— 设备行的频道占用数字、搜索树里的频道节点、检查器的频道别名全部取不到数据。新增 `ChannelInfo.find(map, frequency, p2pType)`（先查 `freq|type`、再退回纯频段），替换 `P2PTreeWidget`（搜索树频道节点、设备行汇总）与 `DetailPanelWidget`（`channelAlias`、就地改名回显）的四处查找。
5. 36 个离屏背包槽位未动（禁改区）。
6. **全程没有实机视觉验证**：`compileJava` 与 `spotlessCheck` 通过、CI 绿，但批量绑定后的状态点颜色、右键折叠（只在批量模式 + 频道行生效）、Ctrl 框选、跨维度传送、静止时 `syncVersion` 节流都只在代码层核对过。§6.3 的 V-5（零槽位容器是否真的触发空指针）与 V-6（`MinecraftServer` 任务调度入口）仍未实测。

---

## 契约校验

在四个仓的根目录分别执行（把 `<版本>` 换成 1.21.1 / 1.20.1 / 1.12.2 / 1.7.10）：

```sh
sed -n '/contract:begin/,/contract:end/p' docs/config-ui-porting.md > /tmp/c-<版本>.txt
```

然后 `diff` 任意两份，必须为空。不为空说明有人只改了一个仓的契约段：先判断改动本身是否成立（成立则四仓同步并各自提交），再合并。
