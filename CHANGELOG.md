# 更新日志 / Changelog

面向玩家与整合包作者的版本日志。内部重构、CI 调整等不影响游戏行为的改动不在此列出。

---

## 1.2.1-1.21.1 — 2026-10-31

**Minecraft** 1.21.1 · **NeoForge** 21.1.219 或更高 · **AE2** 1.21.1 版本线（开发基于 19.2.17）

**输入端可以在界面上换人**：一条通道只有一个输入端。以前要让另一台设备接管，只能去世界里手动改，现在检查器里点一下就行。

### 新增

- **设为输入端**：设备当前是输出端时，检查器第 4 格出现「设为输入端」—— 这台设备接管所在频段的输入端，原来那台输入端自动降级为输出。顺序在服务端固定为「先降级、再提升」：`P2PService.inputs` 是 `HashMap<Short, Part>`，`updateFreq` 末尾是裸的 `inputs.put`，先提升会把旧输入端直接挤出映射 —— 它的 `output` 字段仍是 false，却再也不在 `inputs` 里，永远等不到输出端，而且这个坏状态会随 `markForSave` 写进存档。降级一侧遍历本频段上所有「非输出端」而不只是映射里那一台：旧版本写坏的存档可能残留孤儿输入端，只处理映射里的只会把孤儿留在原地。输入端与未绑定频段（0000）的设备不显示此按钮：前者那个方向会让通道失去唯一的输入端而彻底失效，后者没有通道可接管。
- **切换的回执**：状态行分别报「0A1B 已设为输入端，原输入端转为输出」/「0000 未绑定频段，无法设为输入端」/「该设备已经是输入端」/「切换失败：网络未就绪或设备已不在当前网络」。切换成功后服务端立刻重扫一次网络，树里的输入/输出标记与频段汇总不用等下一个同步包。

### 改变

- **传送挪到第 5 格**：设备行的动作顺序现在是「高亮 / 改名 / 分配新频段 / 设为输入端 / 传送」，其中后两格按条件占位 —— 已是输入端或未绑定频段时没有「设为输入端」，`allowTeleport = false` 时没有「传送」。频段行仍是三格（频段本身没有位置，不提供传送），类型行仍是两格。
- **落单按钮拉通整行**：次级按钮排两列，数量为奇数时最后那个占满整行 —— 多了「设为输入端」之后设备行变成 5 个，右下角空一格会看起来像少画了一个；频段行原本 3 个按钮的同款空槽一并修掉。

---

## 1.2.0-1.21.1 — 2026-10-28

**Minecraft** 1.21.1 · **NeoForge** 21.1.219 或更高 · **AE2** 1.21.1 版本线（开发基于 19.2.17）

**配置模式界面重做**：把「选设备 → 点选择 → 找频段 → 点绑定」这条跨视图、只存在于服务端字段里的链路，改成屏幕上看得见的配对流。

### 新增

- **配对条**：顶部常驻「源 → 目标」两槽。点选列表里的设备即成为源，点选频段行（或下拉）即成为目标，箭头在两者齐备时点亮。源槽的 × 把配对源退回打开界面时右键的那一台。
- **频段下拉**：目标槽可点开频段列表（按频段号排序），底部是「新建频段…」。列表超长时可用滚轮滚动。
- **状态行**：每次操作都有回执。「已绑定到 0A1B」/「没有源设备」/「0000 不是可绑定频段」/「网络未就绪」——以前这些都只有一个静默的 `return`，玩家只能靠重读界面猜。
- **检查器状态 chip**：连接状态、输入/输出、频道占用放在面板最上面；属性表降到 6 行。
- **就地改名**：改名时其余属性继续显示（以前一进编辑态整块信息消失），Esc 取消。
- **跟随选中**：点选设备后列表自动展开到它并选中；已在可视区则不滚动。绑定后设备换组，也会跟到新位置。
- **传送按钮**：检查器第 4 格新增「传送」，由服务端直接把玩家送到该 P2P 部件所在位置（支持跨维度，实心宿主块会落到顶面）。落位之后客户端会等坐标真正同步过来（跨维度还要等新维度加载完），再点亮这台设备并把视角转向它 —— 直接紧跟动作包发送时用的是旧坐标，跨维度时新维度的方块实体也还不存在；这里的转头刻意不受 `rotateHeadOnHighlight` 约束（那项只管高亮；传送是玩家主动发起的位移，落点贴着设备，不转头等于落地即迷路）。这是一项可选的越权能力，默认关闭：服务端配置 `allowTeleport = false` 时按钮整格隐藏，服务端也会拒绝伪造的传送包。频段节点没有自己的位置，因此不提供传送按钮。

### 改变

- **列表改为四列**：状态点 / 名称 / 频段 / 汇总。缩进从 2px/级 改为 10px/级，频段与汇总右对齐，超宽按像素截断并补省略号（以前被 scissor 硬裁且无省略号，剩余频道经常被切掉）。
- **按钮不再漂移**：取消 3×2 六宫格，主操作固定为底部右下角的「绑定」，其余动作收进检查器的 2×2 次级按钮。以前同一个像素位在不同节点类型下含义完全不同（第 1 槽在设备上是「改名」，在类型上是「刷新」）。
- **「选择」的副作用被拆开**：以前它改的是 `p2pTunnelPartBinding`，而那个字段同时是网络扫描的锚点 —— 换配对目标会顺带把整个界面切到另一张网络。现在配对目标独立存放，扫描锚点只在打开界面时确定。
- **角色按钮语义修正**：「待绑定」实际执行的是「定位到待绑定设备」，已随旧按钮一并移除。
- **「高亮」与「定位」合并**：点一次「高亮」现在依次做三件事 —— 在世界里点亮、关掉界面、把视角转向目标。顺序是刻意的：位置信息的动作包要在关界面之前发出（关界面会立刻发出容器关闭包，之后服务端不再受理这个 menu 的动作）；界面要在下一帧渲染前撤掉，否则会看到「界面还在、世界已经亮起」；转头放最后，因为它改的是玩家朝向，界面还开着时会被 GUI 的鼠标捕获抵消。转头仍受 `[client.highlight] rotateHeadOnHighlight` 控制，关掉它的玩家不会被强行掰头。
- **按钮换位**：检查器第 2 格从「定位」改为「改名」（改名原先在第 4 格），第 4 格改为「传送」。频段行同样换位（第 2 格改名），但不提供传送 —— 频段本身没有位置，只剩三格内容；类型行只保留「初始化 P2P」（仅 ME P2P）与「刷新」两格。
- **聊天栏不再附带传送链接**：高亮只发位置信息。旧链接本质是 `/execute in <dim> run tp @p …` 命令，需要 OP 或开启作弊；现在传送由服务端 API 执行，是否允许完全由 `allowTeleport` 决定，与权限等级解耦。
- **频段高亮列出两端**：以前点频段节点的高亮只把输入端写进聊天栏，玩家看不到频道最终流向哪几台设备。现在输入端与输出端分两段全部列出，每行带「输入」/「输出」标记。
- **列表只在新数据到达时重建**：以前每 tick 重建一次（20 次/秒），现在跟随同步包（2 次/秒）。
- **频道占用显示在设备行**：ME P2P 设备的汇总列显示频道占用，并带「入 / 出」前缀区分角色。输入端显示自己外部网络的已用/总数；输出端显示「本输出已用 / (输入端剩余 + 本输出已用)」——分母是这条频段还能分给它的份额度。非 ME 设备没有这个概念，仍显示输入/输出角色。频段行保持「台数 + 剩余频道」，非 ME 频段没有频道概念，只报台数（以前显示「余 0」等于把「没有这个概念」画成了「剩余为零」）。
- **频道占用不再出现三元组**：详情面板删掉「已用/剩余/总数: a/b/c」那一行（频段节点与类型节点都有），统一成头部一枚 `已用/总数` chip + 底部一根进度条，且只对输出设备 / ME 频段显示。
- **类型名改为部件译名**：树上与详情里的 `me_p2p_tunnel` / `item_p2p_tunnel` 这类资源路径，改为读对应部件的物品名（「ME P2P 隧道」「物品 P2P 隧道」…），搜索也支持按译名匹配。
- **绑定后保留目标频段**：绑完一台设备目标槽不再被清空，可以接着选下一台绑到同一个频段。
- **换配对源保留目标频段**：点选另一台设备时不再清空已选目标；只有目标频段与新设备的 **P2P 类型不同**时才清空。判断依据从「上一台源设备」改为「目标频段自身的类型」——源设备要等服务端回包才更新，连点时读到的是旧值，正是此前同类型切换也会被清空的原因。
- **频段下拉按类型过滤**：目标槽的下拉列表只列「当前配对源所在类型」下的频段（判定看该频段里有没有同类型设备，兼容 AE2 允许的混装频段）。
- **目标槽可开可合**：再点一次目标槽收起频段列表，点别处同样收起（此前浮层与槽口各管一半状态，点第二下会「先关再开」，等于永远展着）。
- **改名后即时刷新**：设备改名与频段别名改名之后立刻重扫一次网络，树与检查器马上反映新名字。
- **Ctrl 多选批量绑定**：按住 Ctrl 点选多台设备，源槽显示「已选 N 台」，回车或「绑定」一次绑完（服务端逐台绑定，第一台成为该频段输入端，其余成为输出端）。多选**只允许同类型** —— 点到不同类型的设备时，之前选中的全部取消；多选行的底色比单选浅一档并带绿色左边条，避免和单选混淆。
- **Ctrl 拖动框选**：按住 Ctrl 从某一行按下左键往上或往下拖，覆盖到的设备会**改变选中状态**而不是一律加上：从一台未选中的设备起手是「扫过即选中」，从一台已选中的设备起手是「扫过即取消选中」，整段拖动只朝一个方向走。**从频段行起手时整条频段的两端一起算**（频段折叠着也能选，不必先展开）。每次拖动都从按下瞬间的快照重算，所以往回拖会把多出来或少掉的行还原；划过类型标题不会把整个类型选上；跨类型时只收同类型的设备，与 Ctrl 点选同一条规则。指针划出列表上下边界时会在最上 / 最下一行停住，不会顺手把滚出视野的设备也选进来（想多选就边拖边滚轮）。拖动过程中配对条的「已选 N 台」实时跟随，不用等下一次同步包。
- **批量操作模式**：底部操作条在「绑定」左边新增「批量操作」按钮，点一下进入批量模式，按钮文字变为「退出批量操作」并点亮，再点一次才退出。模式里不用一直按着 Ctrl：单击设备就是加选 / 取消加选，按住左键拖动照样框选；**点或拖到频段行都会把该频段下所有设备收进多选**（整条已全选时再点一次整条退掉）。批量模式下频段行的展开 / 折叠改用**右键** —— 左键已经被「选中设备」占用，不能再兼职折叠。按住 Ctrl 点频段行或类型行也能直接进这个模式，类型行仍然只负责展开折叠、不选设备。退出模式不会清空已选设备 —— 批量选完正好接着挑目标频段去绑定。
- **类型行不再提供高亮与传送**：P2P 类型行只是分组标题，高亮或传送都需要先挑一台具体设备，用「该类型第一台」代替等于替玩家猜，所以这两格直接去掉，类型行只保留「初始化 P2P」与「刷新」。

### 修复

- **配置模式同步包偶发编码崩溃**：服务端日志出现 `Failed to encode packet 'clientbound/minecraft:custom_payload'`，根因是 `ConcurrentModificationException`，客户端随即掉线。同步包只保存了界面数据表的引用，而这张表每 10 tick 会被 `clear()` 后重建，Netty 线程真正写字节时正好撞上重置。现在发包时先在服务端线程把数据拷成快照再交给网络层（该缺陷自同步包引入时就存在，只是触发需要刷新与编码撞在同一瞬间）。

### 配置变更（需要留意）

- **移除** `[client.treeColors]`、`[client.panelColors]`、`[client.buttonColors]`、`[client.panelLayout]`。
- **新增** `[client.palette]`：语义色板（text / accent / surface / divider / status / chip / overlay）。默认值按对比度挑选，在列表底色 `#ADB0C4` 上全部 ≥ 4.5:1；旧默认的「金色类型行」「灰色计数」实测只有 1.08–1.12:1。
- `[client.treeLayout]` 保留，默认值调整为 `rowHeight=14`、`indentWidth=10`；移除 `iconTextSpacing` / `expandIconWidth`（折叠箭头改为代码内固定绘制）。
- **新增** `[server] allowTeleport`（默认 `false`）：是否允许界面上的「传送」按钮把玩家直接送到 P2P 设备位置。默认关闭是因为它绕过了原版传送权限（旧的可点击链接要求 OP / 开启作弊）。服务端配置由 NeoForge 同步到客户端，因此开服时改这一项即可，客户端无需改配置。

> 旧的这四段配置会被直接忽略（不会报错）。想保留自定义颜色的话，对照 `palette` 重新填一遍即可。

---

## 1.2.1 (English)

**Minecraft** 1.21.1 · **NeoForge** 21.1.219+ · **AE2** 1.21.1 line (developed against 19.2.17)

**The input side can now be handed over from the GUI**: a channel has exactly one input. Changing which device holds it used to mean going into the world and rewiring by hand; now it is one click in the inspector.

### Added

- **Make input**: when the selected device is an output, the 4th slot of the inspector reads *Make Input* — that device takes over the channel's input role and the previous input is demoted to an output. The server always demotes first and promotes second: `P2PService.inputs` is a `HashMap<Short, Part>` whose `updateFreq` ends in a bare `inputs.put`, so promoting first silently evicts the old input — its `output` flag still says "input" while the map no longer knows it, it never sees an output again, and `markForSave` persists that state into the save file. The demotion pass walks every non-output part on that frequency rather than just the one in the map, because saves written by older builds can still contain orphaned inputs; handling only the mapped one would leave the orphans behind. Devices that are already inputs, or that have no channel bound (0000), get no button: the first direction would strip the channel of its only input and kill it, the second has nothing to take over.
- **Result reporting**: the status line distinguishes "0A1B is now the input; the previous input became an output" / "0000 has no channel bound" / "already an input" / "switch failed: network not ready or device left this network". On success the server rescans the network immediately, so the in/out tags in the tree and the channel summary do not wait for the next sync packet.

### Changed

- **Teleport moved to the 5th slot**: device rows now read *Highlight / Rename / Assign channel / Make Input / Teleport*, with the last two conditional — no *Make Input* when the device is already an input or has no channel, no *Teleport* when `allowTeleport = false`. Frequency rows keep three slots (a frequency has no position, so no teleport); type rows keep two.
- **A lone trailing button spans the full row**: secondary actions are laid out in two columns, and an odd count makes the last one take the whole row — with *Make Input* the device row now carries five buttons, and an empty bottom-right slot reads as a missing one. The same hole on three-button frequency rows is fixed too.

---

## 1.2.0 (English)

**Minecraft** 1.21.1 · **NeoForge** 21.1.219+ · **AE2** 1.21.1 line (developed against 19.2.17)

**Config Mode UI rewrite**: the old flow — pick a device, hit *Select*, hunt for a frequency, hit *Bind* — was a cross-view chain that only existed in server-side fields. It is now a source → target pair you can see on screen.

### Added

- **Pair bar** at the top: click a device to make it the source, click a frequency row (or use the dropdown) to make it the target; the arrow lights up once both are filled, and the × on the source slot resets it to the tunnel you right-clicked on open.
- **Frequency dropdown** on the target slot, sorted by frequency with a *New frequency…* entry at the bottom.
- **Status line**: every action now reports back (bound / no source / invalid frequency / grid not ready) instead of silently returning.
- **Inspector status chips**: connection state, in/out role and channel usage moved to the top of the panel; the property table shrank to six rows.
- **Inline rename** that keeps the rest of the panel visible; Esc cancels.
- **Follow selection**: the tree expands to the clicked device; after a bind it follows the device to its new group.
- **Teleport button** (4th slot): the server moves you to the P2P part, cross-dimension included, landing on top of a solid host block. The client waits for the coordinates to actually arrive (and for the new dimension on a cross-dimension trip) before highlighting the device and turning the view towards it. This is an opt-in privilege escalation — `[server] allowTeleport` defaults to `false`, hides the whole slot, and the server rejects forged teleport packets. Frequency rows have no position, so they get no teleport button.

### Changed

- **Four-column list** (status dot / name / frequency / summary), 10 px indent per level, right-aligned frequency and summary, pixel-accurate truncation with an ellipsis.
- **Buttons no longer drift**: the 3×2 grid is gone; the primary action is a single *Bind* button in the bottom-right corner, everything else lives in the inspector's 2×2 secondary grid.
- **"Select" side effects split apart**: the binding target is now stored separately from the tree anchor, so switching targets no longer switches the whole view to another network.
- **Highlight and locate merged**: one *Highlight* click lights the target in the world, closes the GUI and turns your view towards it, in that order. The view turn still respects `[client.highlight] rotateHeadOnHighlight`.
- **Button positions swapped**: slot 2 is now *Rename* (was *Locate*), slot 4 is *Teleport*. Frequency rows get the same swap but no teleport; type rows keep only *Init P2P* (ME only) and *Refresh*.
- **No more teleport links in chat**: highlight only sends position info. Teleporting is done by the server API and is gated by `allowTeleport` instead of OP level.
- **Channel highlight lists both ends**: inputs and outputs are both printed, each line tagged 输入/输出 with dimension and coordinates.
- **List rebuilds only when new data arrives** (2 Hz instead of 20 Hz).
- **Channel usage on the device row**: ME P2P rows show used/total with an 入/出 prefix; frequency rows keep "count + remaining channels", and non-ME channels just report the count.
- **No more a/b/c triple**: the inspector shows a `used/total` chip plus a usage bar, only for output devices and ME channels.
- **Type names use the part's item name** instead of the raw resource path; search matches them too.
- **Target frequency survives a bind**, and switching the source only clears it when the target channel's P2P type differs.
- **Dropdown filtered by the source's type**; the target slot toggles open/closed on repeated clicks.
- **Immediate refresh after rename** — the tree and inspector update without waiting for the next poll.
- **Ctrl multi-select** to bind several devices at once (same type only; the first one becomes the input).
- **Ctrl-drag box selection**: dragging over rows *toggles the selection direction* — starting on an unselected device selects everything swept over, starting on a selected one deselects. Starting from a frequency row covers the whole channel even when collapsed; every update recomputes from the snapshot taken on mouse-down, so dragging back restores rows. Rows outside the viewport are never included.
- **Batch mode**: a *Batch select* button left of *Bind* enters a mode where plain clicks add/remove devices, dragging still box-selects, and clicking or dragging over a frequency row takes the whole channel. The button flips to *Exit batch*; right-click now expands/collapses frequency rows (left-click is taken by selecting), and leaving the mode keeps the selection.
- **Type rows no longer offer highlight/teleport**: they are group headers; both actions need a concrete device.

### Fixed

- **Intermittent sync-packet encoding crash**: `Failed to encode packet 'clientbound/minecraft:custom_payload'` caused by a `ConcurrentModificationException` — the packet kept references to the menu's data tables, which are cleared and rebuilt every 10 ticks, so the Netty thread could iterate one mid-reset. The packet now snapshots the tables on the server thread.

### Config changes

- **Removed** `[client.treeColors]`, `[client.panelColors]`, `[client.buttonColors]`, `[client.panelLayout]`; **added** `[client.palette]` (semantic colours, all ≥ 4.5:1 on the `#ADB0C4` row background).
- `[client.treeLayout]` defaults changed to `rowHeight=14`, `indentWidth=10`; `iconTextSpacing` / `expandIconWidth` removed.
- **Added** `[server] allowTeleport` (default `false`): whether the GUI's *Teleport* button may move you to a P2P device. Server configs are synced to clients, so setting it on the server is enough.

---

## 1.1.0-1.21.1 — 2026-10-25

**Minecraft** 1.21.1 · **NeoForge** 21.1.219 或更高 · **AE2** 1.21.1 版本线（开发基于 19.2.17）

### 新增

- **复制模式：粘贴选项面板（8 项开关）**：复制到卡片之后、批量粘贴之前，可以逐项决定这一次要应用哪些配置。

| 选项 | 作用 |
| --- | --- |
| 升级 | 升级卡 |
| 设置 | 输出面、模糊模式等通用配置 |
| 优先级 | 存储总线 / 接口等的优先级 |
| 配置物品 | 过滤、标记等配置槽内容 |
| 样板 | 样板供应器的样板 |
| P2P 类型 | P2P 隧道类型 |
| P2P 频率 | P2P 频段 |
| 名称 | 设备/部件的自定义名称 |

- **`G` 键**：手持卡片且处于复制模式时打开该面板，键位可在「选项 → 控制 → 按键绑定 → 高级内存卡」中修改。
- **复制后自动弹出**：默认在每次复制（Shift + 右键把设置复制进卡片）时自动弹出面板；目标不是 AE 方块或线缆部件时不会弹。
- **齿轮开关**：面板左侧（挂在面板外）的按钮切换自动弹出 —— 齿轮 = 弹出，灰化齿轮 = 不弹，设置同样跟着卡片走。
- **选项按卡保存**：勾选结果与齿轮开关都存在卡片上，切换模式、重新复制、退出存档都不会丢。
- **新界面同样自带中文**：面板标题、8 个开关名称与齿轮按钮的悬停提示都有简体中文（语言支持本身在 1.0.2.1 就已加入，这里是补齐新内容）。

### 修复

- **P2P 重进存档后绑定丢失**：用配置界面打开 P2P 管理时，会把网络重建期间的误判当成真实状态写进存档，导致退出重进后 P2P 全部失去绑定或角色被改成输出端。现在运行时映射会先按部件自身持久化的频段重建，角色修正也只在外部网络就绪时进行。
- **P2P 通道一直显示"未连接"**：把第二个 ME P2P 绑到同一频段后，通道两端都是输入端、没有输出端，整条通道永久不通。现在所有 P2P 类型在绑定时会统一仲裁输入/输出角色；旧存档里已经写坏的"多输入、零输出"通道会自动自愈一次。
- **P2P 改名不生效**：重命名后名称没有落盘、界面也无提示。现在改名只改名称本身（不再顺带改动升级卡/优先级/频段），并会立即存盘与刷新；同时修掉了改名后左侧树里选中项被清空的问题。
- **副手持卡时对 P2P 部件粘贴无效**：批量粘贴此前固定按主手处理，副手持卡时部件会被静默跳过。

### 注意（行为变化）

- **未配置过的卡片默认"全部不选"**。粘贴时不会应用任何可选项，需要在面板里显式勾选；勾选一次后即按卡片记住。升级到本版本后第一次复制时请留意面板。
- **取消勾选 = 保持目标原值**。"样板 / P2P 类型 / 名称"这三项在 AE2 里没有"保留"语义，本模组会在粘贴时把目标自身的当前值填回去，效果等同于不改动；其余各项则直接跳过。

### 文档

- README 中英两版重写，按 1.21.1 / NeoForge 实际环境更新依赖、按键与配置说明。
- 游戏内指南（AE2 Guide）补齐粘贴选项面板，并修正了选区颜色指示（起点方块红、终点方块黄、准星方块绿、待粘贴时整选区变绿、超上限转红）。

---

## 1.1.0 (English)

**Minecraft** 1.21.1 · **NeoForge** 21.1.219+ · **AE2** 1.21.1 line (developed against 19.2.17)

### Added

- **Copy Mode: paste options panel (8 toggles)**: after copying a device into the card and before pasting in bulk, choose exactly what gets applied.

| Option | Applies |
| --- | --- |
| Upgrades | Upgrade cards |
| Settings | Generic settings such as output side and fuzzy mode |
| Priority | Storage bus / interface priority |
| Config inventory | Config slots (filters, marker items, …) |
| Patterns | Pattern provider patterns |
| P2P type | P2P tunnel type |
| P2P frequency | P2P frequency |
| Name | Custom name of the block/part |

- **`G` key** opens the panel while holding the card in Copy Mode (rebindable under Options → Controls → Key Binds → Advanced Memory Card).
- **Auto-open after copying**: by default the panel pops up every time you copy (Shift + Right-click to copy a device's settings into the card). It only opens when the target is an AE block or a cable part.
- **Cog toggle**: the button attached to the left of the panel switches the auto-open behavior — cog = open every time, greyed-out cog = never. Saved per card.
- **Options are stored per card**: the toggles and the auto-open switch persist across mode switches, re-copies, and world reloads.
- **The new UI is localized too**: the panel title, all 8 toggle labels and the cog button's tooltip have Simplified Chinese strings (Chinese support itself shipped in 1.0.2.1; this just covers the new content).

### Fixed

- **P2P bindings lost after reloading a world**: opening the P2P management GUI treated network-rebuild artifacts as real state and wrote them to the save, so after a restart P2Ps lost their bindings or had their role flipped to output. Runtime mappings are now rebuilt from each part's own persisted frequency first, and role correction only runs once the external network is ready.
- **P2P channel stuck on "Not Connected"**: binding a second ME P2P to an existing frequency left both ends as inputs, so the channel never connected. Input/output roles are now arbitrated for every P2P type on bind, and channels already broken in old saves are repaired once automatically.
- **P2P renaming did nothing**: the new name was never saved and the GUI gave no feedback. Renaming now only touches the name (no longer dragging upgrades/priority/frequency along), saves immediately, and no longer clears the tree selection.
- **Pasting onto P2P parts did nothing with the card in the off hand**: bulk paste always assumed the main hand and silently skipped parts otherwise.

### Notes (behavior changes)

- **A card that was never configured now defaults to "nothing selected"**, so a paste applies no optional data until you tick the items you want. The choice is remembered per card — watch the panel the first time you copy after updating.
- **Unticking means "leave the target as it is".** For *patterns / P2P type / name*, AE2 has no "keep existing" semantics, so the mod refills each target's current value instead of clearing it; the other options are simply skipped.

### Documentation

- Both READMEs rewritten for the actual 1.21.1 / NeoForge environment (dependencies, key binds, config layout).
- The in-game AE2 Guide now documents the paste options panel and fixes the selection color legend (red start block, yellow end block, green targeted block, green box once ready to paste, red when over the volume limit).

---

## 1.0.2.1-1.21.1 — 2026-07-23

见 GitHub Releases 自动生成的日志。 / See the auto-generated changelog on GitHub Releases.
