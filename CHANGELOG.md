# 更新日志 / Changelog

面向玩家与整合包作者的版本日志。内部重构、CI 调整等不影响游戏行为的改动不在此列出。

---

## 1.1.0-1.12.2 — 2026-10-27

**Minecraft** 1.12.2 · **Forge** 14.23.5.2860 或更高 · **AE2** AE2 Extended Life（`570458:6302098`）

把 1.20.1 / 1.21.1 线的两项核心能力回移到 1.12.2：**复制模式的选择性粘贴**与**完整的 P2P 频段仲裁**，并让两个界面改用与 1.21.1 相同的贴图与布局。

### 新增

- **复制模式：粘贴选项面板（7 项开关）**：复制到卡片之后、批量粘贴之前，可以逐项决定这一次要应用哪些配置。

| 选项 | 作用 |
| --- | --- |
| 设置 | 输出面、模糊模式等通用配置（`IConfigManager`） |
| 优先级 | 存储总线 / 接口等的优先级 |
| 配置物品 | 过滤、标记等配置槽内容 |
| 样板 | 接口的样板槽 |
| P2P 类型 | P2P 隧道类型（换型时会拆掉重建，见下） |
| P2P 频率 | P2P 频段 |
| 名称 | 方块的自定义名 |

- **`G` 键**：手持卡片且处于复制模式时打开面板，键位可在「控制 → 按键绑定 → 高级内存卡」中修改。
- **复制后自动弹出**：默认在每次复制（Shift + 右键把设置复制进卡片）后自动弹出；目标不是 AE 方块或线缆部件时不会弹。
- **选项按卡保存**：勾选结果与「自动弹出」开关都存在卡片根标签的 `copy_options` 下，切换模式、重新复制、退出存档都不会丢。
- **P2P 换型**：卡片上的 P2P 类型与目标不同且勾选了「P2P 类型」时，按 AE2 自己的内存卡流程（`removeTunnel` → `removePart` → `addPart`）重建部件，并保留原来的输入/输出角色；频段按卡片决定。
- **界面与 1.21.1 同版**：配置模式与粘贴选项两个界面都复用 1.21.1 的面板贴图与 `assets/ae2/screens/*.json` 里的坐标，不再手绘深色底。
  - 粘贴选项：`copy_mode_menu.png` 176×181，标题 (10,10)，选项行 left 10 / width 150 / height 14 / 步进 19 / 首行 top 24；复选框用 `checkbox.png` 的四个 22×12 状态格（含悬停）。
  - P2P 配置：`config_mode_menu.png` 352×282，标题 (7,7)、底部提示 (7,269)、搜索框 (7,25,120,12)、树 (15,53,152,200)、滚动条 (169,53)、详情 (184,53,152,210)。
- **控件点击音效**：复选框、P2P 树节点、详情面板按钮统一走同一个音效入口。

### 修复

- **复制后不弹面板**：1.12.2 的复制发生在方块交互里（`AEBaseTileBlock#onBlockActivated`），方块交互吃掉右键后 `Item#onItemUse` 根本不会执行，原来挂在 `onItemUse` 里的 `openGui` 是死代码。现在由模组自己完成复制（方块 `downloadSettings`、部件 `onActivate`），成功后再按开关弹面板。
- **复选框不显示、配置面板与控件错位**：1.12.2 的 `Gui#drawTexturedModalRect` 把 UV 写死成 1/256，只对 256×256 的贴图成立。64×64 的复选框因此采到透明区（整块看不见），512×512 的配置面板则 UV 越过 1.0 环绕（显示成另一块区域）。已改为按真实尺寸计算 UV。
- **复选框整体发灰发暗**：贴图四边形不带顶点色，取的是固定管线的「当前颜色」；标题文字（`0x404040`）与悬停高亮（`0x22000000`）都会改掉它且不还原。现在画贴图前显式复位颜色。
- **点击后界面要重开才刷新**：复制模式的容器是空容器（没有任何槽位），服务端改写卡片 NBT 后不会触发 `SPacketSetSlot`，客户端读到的永远是旧值。现在菜单持有权威选项并主动下发同步包。
- **P2P 改名不落盘**：`setCustomName` 只改部件持有的 ItemStack 显示名，不会自己写存档；现在显式 `markForSave`。
- **把设备绑到「未连接(0000)」等于静默解绑**：`0000` 是未绑定分组，现在直接拒绝，不再把设备写进 `freq=0` 的公共通道。
- **绑定第二个设备时不再产生两个输入端**：角色仲裁现在对所有 P2P 类型统一执行（旧实现跳过了 ME P2P），目标频段已有其他输入端时，当前设备作为输出端。
- **分配新频道的中断风险**：旧实现先把频段写成 `0` 再分配，两次写入之间被打断（区块卸载、退出存档）会留下「已绑定设备变成未绑定」。现在一次性换到新频段。
- **粘贴不再走 `uploadSettings`**：该方法把「缺少键」当成「清空目标」（优先级缺键即 0、配置槽缺键即清空），且 `TileInterface#uploadSettings` 会把接口样板退还成空白样板、只有背包里有空白样板时才恢复样板。选择性粘贴需要「没勾选的项保持原值」，因此改为按项直接写目标字段。

### 注意（行为变化）

- **未配置过的卡片默认「全不选」**：粘贴不再改动任何可选项。第一次使用请按 `G` 勾选需要应用的内容；掩码为 0 时粘贴只给提示，不消耗选区。
- **「升级」不在选项里**：AE2 1.12.2 的 `downloadSettings` 从不导出 `upgrades`（全仓没有对应的 `writeToNBT`），卡片里没有这段数据。与其放一个永远空转的开关，不如不提供。
- **「名称」对方块有效，对部件无效**：部件侧的 `downloadSettings` 不写 `display` 段，卡片里没有这个数据；此时保持目标原名，不会清空。
- **P2P 换型会拆掉原部件**：这是 AE2 自己的内存卡行为（`PartP2PTunnel#onPartActivate`），升级卡不会跟着走。

### 文档

- README 按 1.21.1 的结构重写，并修正了从 1.20.1 分支带过来的错误徽章与前置要求（此前写着 Minecraft 1.20.1 / Forge 47.4.10 / AE2 15.4.10）。
- 新增本更新日志。

---

## 1.1.0-1.12.2 (English)

**Minecraft** 1.12.2 · **Forge** 14.23.5.2860+ · **AE2** AE2 Extended Life (`570458:6302098`)

Backports the two core features of the 1.20.1 / 1.21.1 line to 1.12.2: **selective paste** in copy mode and **complete P2P frequency arbitration**. Both GUIs now use the same textures and layout as 1.21.1.

### Added

- **Copy mode: paste options panel (7 toggles)** — decide per paste which parts of the stored configuration are applied: settings, priority, config slots, patterns, P2P type, P2P frequency, custom name.
- **`G` key** — opens the panel while holding the card in copy mode (rebindable).
- **Auto-open after copy** — the panel opens after a Shift + right-click copy unless disabled in the panel.
- **Options are stored on the card** (`copy_options`), so they survive mode switches, re-copies and world reloads.
- **P2P type change** — when the card's tunnel type differs and the toggle is enabled, the part is rebuilt using AE2's own memory-card flow, keeping the previous input/output role.
- **GUI parity with 1.21.1** — both screens reuse the 1.21.1 panel textures and the coordinates from `assets/ae2/screens/*.json`.
- **Click sounds** for checkboxes, tree rows and detail-panel buttons.

### Fixed

- Auto-open never fired: in 1.12.2 the copy happens in `AEBaseTileBlock#onBlockActivated`, so `Item#onItemUse` is never reached.
- Checkbox invisible / config panel misaligned: `Gui#drawTexturedModalRect` hardcodes 1/256 UVs, which is only correct for 256×256 textures.
- Checkbox rendered dimmed: texture draws use the fixed-function current color, which text and rectangle drawing leave modified.
- UI refreshed only after reopening: the copy-mode container has no slots, so `SPacketSetSlot` is never sent for the card; options are now pushed by an explicit sync packet.
- P2P rename was not saved; binding to `0000` silently unbound the device; binding a second device produced two inputs; assigning a new frequency could be interrupted halfway.
- Paste no longer uses `uploadSettings` (it treats missing keys as "clear", and refunds interface patterns).

### Notes (behavior changes)

- Cards that were never configured paste **nothing** — press `G` to select options first.
- **Upgrades are not an option**: AE2 1.12.2 never exports `upgrades` into the memory card.
- **Name applies to blocks only**; AE2 does not export `display` for parts, so part names are left untouched.
- **Type change replaces the part** (AE2's own behavior); upgrade cards are not carried over.

---

## 1.0.1-1.12.2 及更早 / 1.0.1-1.12.2 and earlier

见 GitHub Releases 自动生成的日志。 / See the auto-generated notes on GitHub Releases.
