# 配置模式 UI 跨版本移植文档（1.21.1 / 基准）

| 项 | 值 |
| --- | --- |
| 本版角色 | 语义基线来源，不参与移植 |
| 工作副本 | `AdvancedMemoryCard-1.21.1` |
| 分支 | `suntide/1.21.1` |
| 加载器 / Java | NeoForge 21.1.219 / Java 21 |
| AE2 | 1.21.1 系（curse 7027323） |
| 当前版本 | `1.2.1-1.21.1` |
| 下游目标 | 1.20.1、1.12.2、1.7.10 |

本文只描述**基准发生了什么、下游该拿什么**。目标版本的执行细节写在各自的 `docs/config-ui-porting.md`；四份文档的「跨版本契约」章节逐字一致。

---

## 一、本版定位

1.21.1 是这次移植的**唯一真相来源**：新配置模式界面、服务端角色仲裁、状态码通道、布局常量都以本版实现为准。

它同时承担一个额外职责：**在开始移植之前，先把本版自身的三处缺陷修掉**。否则同一个缺陷会被复制三遍，且在下游表现为「新 UI 的行为就是不对」，排查成本远高于现在修。

因此本版有一个独立的阶段 0（见第三节），必须先于 1.20.1 的工作完成。

---

## 二、能力清单

以下 18 项是下游必须对齐的能力面。1.21.1 已全部实现；各目标版本的可达性差异写在各自文档。

| # | 能力 | 本版实现位置（参考实现） |
| --- | --- | --- |
| F1 | 顶部「源 → 目标」配对条（双槽 + 箭头 + 清除叉） | `client/gui/widgets/PairBarWidget.java` |
| F2 | 频段下拉浮层（7 行 + 滚动）+「新建频段」行 | 同上（`renderOverlay` / `handleOverlayClick` / `handleOverlayScroll`） |
| F3 | 状态行回执（16 码 + 4 级语义色） | `utils/ActionStatus.java` + `PairBarWidget.setStatus` |
| F4 | 检查器状态 chip + 角色 chip | `client/gui/widgets/DetailPanelWidget.java`（`renderChips`） |
| F5 | 就地改名（P2P 与频段两类） | 同上（`enterEditing` / `onNameConfirm` / `cancelEditing`） |
| F6 | 跟随选中（`revealP2P`，不强制居中） | `P2PTreeWidget.revealP2P` + `ConfigModeScreen.refreshFromMenu` |
| F7 | 四列列表（状态/名称/频段/汇总）+ 列头 | `P2PTreeWidget.renderWidget` / `renderColumnHeader` |
| F8 | 右下角主操作「绑定」+ 批量开关 | `client/gui/widgets/ActionBarWidget.java` |
| F9 | 检查器 2×N 次级按钮按可见项重排（奇数末格拉通） | `DetailPanelWidget.repositionButtons` |
| F10 | 传送按钮（服务端 `allowTeleport` 二次校验） | `ConfigModeScreen.doTeleport` + `menu/ConfigModeMenu` 的 `teleport_p2p` |
| F11 | Ctrl 拖动框选 | `P2PTreeWidget.beginDragSelection` / `applyDragSelection` |
| F12 | Ctrl 多选 + 批量绑定 | `P2PTreeWidget.multiSelection` + `ConfigModeScreen.doBind` |
| F13 | 批量模式下右键展开频段 | `P2PTreeWidget.mouseClicked`（`button == 1` 分支） |
| F14 | 设备行 5 格（高亮/改名/新频段/设为输入端/传送） | `DetailPanelWidget` 的回调表 + `P2PManager.switchToInput` |
| F15 | 「设为输入端」（先降级旧输入端再提升目标） | `P2PManager.switchToInput` / `demoteInputsAt` |
| F16 | 语义色板 palette | `config/ClientConfig.java` 的 `client.palette.*` |
| F17 | 面板 440×312 + 新底板贴图 | `assets/ae2/screens/config_mode_menu.json` + `guis/config_mode_menu.png` |
| F18 | 同步节流（`syncVersion`） | `ConfigModeMenu.getSyncVersion` + `ConfigModeScreen.containerTick` |

关键文件规模：`P2PTreeWidget` 1117 行、`DetailPanelWidget` 711 行、`ConfigModeScreen` 647 行、`PairBarWidget` 550 行、`ConfigModeMenu` 844 行、`P2PManager` 741 行。

---

## 三、待修缺陷（阶段 0，先于移植）

| # | 缺陷 | 证据 | 后果 | 处理 |
| --- | --- | --- | --- | --- |
| B1 | `ActionStatus.severity()` 漏判码 10/11 | `utils/ActionStatus.java` 的 severity 分支只覆盖 0–9 与 12–15，10/11 落到默认分支 | 批量绑定成功、部分成功时状态点是中性灰而非绿/黄；三份移植会各复制一遍 | 补两个分支：10 → OK，11 → WARN |
| B2 | `ServerConfig.allowTeleport` 注释与默认值矛盾 | `config/ServerConfig.java` 注释写「Default: false.」，代码默认 `true` | 文档与行为不一致；玩家以为默认关闭实际默认开启 | **已定：保留 `true`**（与 1.2.1 行为一致，避免发版即移除既有能力），把注释改成 `Default: true.`。若后续要收紧权限，改这一行的默认值即可，同时更新本条 |
| B3 | `client/gui/ModMenu.java` 是死代码 | 该文件只被自己引用；真正注册入口是 `menu/ModMenu.java`；两者定义同一批 menu type | 整目录拷贝到目标版本会 double register 并崩溃 | 删除该文件，并把路径写进下游的「禁止复制清单」 |

阶段 0 的验收：`gradlew build` 通过；进游戏打开配置界面，批量绑定两台设备后状态点应为绿色；`runServer` 正常启动。

阶段 0 的版本处理：作为 `1.2.2-1.21.1` 补丁发布（若 B2 的默认值改动被判定为行为变更，则并入下一个次版本并写进 CHANGELOG）。

---

## 四、对下游的导出物

### 4.1 按参考实现搬运（1.20.1 适用）

`PairBarWidget`、`ActionBarWidget`、`P2PTypeNames`、`ActionStatus`、`P2PChannelUsage`、`ClientTickTasks`、`AEBasePartAccessor`、`ConfigUiLayout`。

`P2PTreeWidget`(1117)、`DetailPanelWidget`(711)、`FlatButton`(186)、`ConfigModeScreen`(647) 属「按行为规格搬 + 改 API」，不是逐行拷贝。

### 4.2 禁止复制清单

以下内容直接拷贝到目标版本会出问题：

- `client/gui/ModMenu.java` —— 死代码，重复注册 menu type（见 B3）。
- `datagen/*`、`ModDataGenerator.java` —— 1.12.2 / 1.7.10 无 datagen。
- `network/*` 的 `StreamCodec` / `CustomPacketPayload` 写法 —— 三个目标版本的网络栈都不是这一套。
- `ConfigModeScreen` 内部的 `SearchFieldWidget`（`AETextField` 子类）—— 1.12.2 / 1.7.10 无 AE2 文本框体系。
- `assets/ae2/screens/*.json` —— 只有 1.20.1 读这套；两老版 AE2 根本没有 StyleManager。
- `mixin/*` —— 1.12.2 / 1.7.10 走反射或 AT。
- 任何用到 record / switch 表达式 / `var` / 模式匹配的代码片段 —— 两个老版编译目标是 Java 8。

### 4.3 导出物变更纪律

下游开工后，如果本版因为修 bug 改动了以下任一文件，必须在三个目标仓的文档里记一条变更记录并评估是否回灌：

- `p2p/P2PManager.java` 的角色仲裁与改名路径
- `utils/ActionStatus.java` 的码表与 severity
- `ConfigUiLayout` 的常量值
- `ConfigModeSyncPacket` 的字段顺序

---

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

## 五、基准验收（作为下游对照）

下游每完成一个阶段，回到本版对照一次以下行为；凡不一致，先确认是本版的缺陷还是下游的偏差。

| 用例 | 期望 |
| --- | --- |
| 空频段绑定 | 本机成为输入端，状态行 OK |
| 已有输入端再绑 | 本机成为输出端 |
| 同频双输入端（异常存档） | 通道修复降级多余输入端，界面恢复「已连接」 |
| 绑定 `0000` | 频段不变，状态行 `BIND_INVALID_FREQ` |
| 输出端点「设为输入端」 | 角色互换，重进存档后保持 |
| 已是输入端再点 | `SWITCH_ALREADY_INPUT`，无写入 |
| 未绑频段点「设为输入端」 | `SWITCH_NO_CHANNEL` |
| 批量绑定 5 台 | `BIND_BATCH_OK`，状态点绿色（依赖 B1 已修复） |
| 批量模式右键频段行 | 折叠/展开，选中不变 |
| Ctrl 拖动框选 | 越界仍延伸，往回拖可还原 |
| 关闭界面后世界高亮 | 正确维度、正确方块；跨维度不误点亮 |
| 静止 10 秒 | `syncVersion` 只随服务端 2 次/秒增长 |

---

## 六、变更记录

| 日期 | 内容 |
| --- | --- |
| 初版 | 建立四仓移植文档，固化跨版本契约；列出 B1–B3 三处待修缺陷与禁止复制清单 |

---

## 契约校验

在四个仓的根目录分别执行（把 `<版本>` 换成 1.21.1 / 1.20.1 / 1.12.2 / 1.7.10）：

```sh
sed -n '/contract:begin/,/contract:end/p' docs/config-ui-porting.md > /tmp/c-<版本>.txt
```

然后 `diff` 任意两份，必须为空。不为空说明有人只改了一个仓的契约段：先判断改动本身是否成立（成立则四仓同步并各自提交），再合并。
