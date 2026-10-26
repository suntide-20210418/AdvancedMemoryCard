# 更新日志 / Changelog

面向玩家与整合包作者的版本日志。内部重构、CI 调整等不影响游戏行为的改动不在此列出。

---

## 1.2.0-1.20.1 — 2026-10-26

**Minecraft** 1.20.1 · **Forge** 47.4.10 或更高 · **AE2** 15.4.10 或更高

本版把 1.21.1 分支的两项核心能力回移到 1.20.1：**复制模式的选择性粘贴**与**P2P 频段仲裁的完整实现**。

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
- **左侧工具栏开关**：面板左边缘的按钮切换自动弹出 —— 亮起 = 弹出，灰化 = 不弹，设置同样跟着卡片走。
- **选项按卡保存**：勾选结果与开关状态都存在卡片上，切换模式、重新复制、退出存档都不会丢。
- **新界面同样自带中文**：面板标题、8 个开关名称与开关按钮的悬停提示都有简体中文。

### 修复

- **P2P 重进存档后绑定丢失 / 角色被改**：网络重建期间的运行时映射尚未建全，旧逻辑把这个中间状态当成真实状态并写进存档。现在会先按部件自身持久化的频段补登缺失的 `inputs`/`outputs`，角色修正也只在外部网络就绪时进行。
- **P2P 通道一直显示「未连接」**：把第二个 ME P2P 绑到同一频段时只登记了第二个输入端，`outputs` 永远为空，整条通道永久不通。现在所有 P2P 类型在绑定时统一仲裁输入/输出角色；旧存档里已经写坏的「多输入、零输出」通道会自愈一次（只在该频段确实没有输出端时动手）。
- **绑定到「未连接(0000)」等于静默解绑**：`0000` 是未绑定分组，不是可绑定目标。服务端现在直接拒绝，界面点击时也会给出提示，不再把设备从存档里解绑。
- **P2P 改名不生效**：改名后名称没有落盘，重进存档就没了，界面也无提示。现在改名只改名称本身（不再顺带改动升级卡/优先级/频段），并会立即存盘与刷新；失败会在日志里留下证据。
- **副手持卡时对 P2P 部件粘贴无效**：批量粘贴此前固定按主手处理，副手持卡时部件会被静默跳过。现在按实际手持侧写入。
- **分配新频道存在中断风险**：旧实现先把频段写成 `0` 再分配新频段，两次调用之间被打断（区块卸载、退出存档）会留下「已绑定设备变成未绑定」的存档。现在直接一次性换频段。
- **专用服务器可能因高亮崩溃**：渲染逻辑原先挂在服务端类上（用 `DistExecutor` 包了一层客户端调用）。现已下沉到客户端，服务端只负责发聊天坐标与传送链接。
- **mixin 静默不注入**：`mixins.json` 声明的 refmap 名与实际产物名不一致，Mixin 不会报错、只是不生效，直到运行时强转失败才暴露。已修正为 `advanced_memory_card.refmap.json`。

### 注意（行为变化）

- **未配置过的卡片默认「全不选」**：粘贴不再改动任何可选项。第一次使用请按 `G` 或在自动弹出的面板里勾选需要应用的内容，勾选结果会跟着卡片走。
- **Shift + 右键空气改为切换模式**：粘贴请使用**非潜行**的右键空气。对着 AE 设备 Shift + 右键仍是「把设置复制进卡片」。
- **复制界面不再是坐标输入框**：坐标仍然通过世界内两次右键标记，界面只负责选项。

### 文档

- 游戏内指南与 1.21.1 保持同一份内容：补齐粘贴选项面板、按键一览与选择框配色说明（红=起点、黄=终点、绿=准星、超范围变红）。
- 两份 README 按 1.20.1 / Forge 环境重写（依赖表、键位、配置段、构建方式），并对齐 1.21.1 的章节结构。
- 新增本更新日志；新增构建校验与发布工作流。

---

## 1.2.0 (English)

**Minecraft** 1.20.1 · **Forge** 47.4.10 or higher · **AE2** 15.4.10 or higher

This release backports two core capabilities from the 1.21.1 branch: **selective pasting in Copy Mode** and **fully implemented P2P frequency arbitration**.

### Added

- **Copy Mode: paste-options panel (8 toggles)**: after copying a device's settings onto the card and before pasting, choose item by item what this paste should apply.

| Option | Effect |
| --- | --- |
| Upgrades | Upgrade cards |
| Settings | Generic settings such as output side and fuzzy mode |
| Priority | Priority of storage buses / interfaces |
| Config inventory | Filter and marker slots |
| Patterns | Patterns of a pattern provider |
| P2P type | P2P tunnel type |
| P2P frequency | P2P frequency |
| Name | Custom name of the block or part |

- **`G` key**: opens the panel while holding the card in Copy Mode (rebindable in Options → Controls → Key Binds → Advanced Memory Card).
- **Auto-open after copying**: by default the panel opens on every copy (Shift + Right-click to copy a device's settings onto the card); it does not open when the target is not an AE block or cable part.
- **Left-toolbar toggle**: the button on the panel's left edge switches auto-open — highlighted = open, greyed out = do not open. The state is stored on the card.
- **Options are saved per card**: both the selection and the toggle survive mode switching, re-copying and relogging.
- **Localized**: panel title, all 8 toggles and the toggle's tooltip have Simplified Chinese.

### Fixed

- **P2P bindings lost / roles flipped after relogging**: the runtime mapping is incomplete while the grid is being rebuilt, and the old logic persisted that intermediate state. Missing `inputs`/`outputs` entries are now re-registered from each part's own persisted frequency, and role correction only happens once the external grid is ready.
- **Channels permanently showing "not connected"**: binding a second ME P2P to an occupied frequency only registered another input, leaving `outputs` empty. All P2P types now go through the same input/output arbitration, and channels that are already broken in existing saves ("several inputs, no output") heal once — only when the frequency really has no output.
- **Binding to "Unconnected (0000)" silently unbound the device**: `0000` is the unbound group, not a bindable target. The server now refuses it and the UI gives immediate feedback.
- **P2P renames were not applied**: the name was never saved and the UI stayed silent. Renaming now touches only the name (no upgrade/priority/frequency side effects), saves immediately, refreshes the client, and logs a warning when it fails.
- **Part pasting did nothing when the card was in the off hand**: bulk paste was hardcoded to the main hand. It now uses the actual hand.
- **Assigning a new frequency could corrupt the save**: the old code wrote `0` first and then the new frequency; an interruption in between left a bound device unbound. It now switches in one step.
- **Dedicated servers could crash on highlight**: rendering lived in a server-side class behind `DistExecutor`. It moved to the client; the server only sends chat coordinates and teleport links.
- **Mixin silently not injected**: the refmap declared in `mixins.json` did not match the produced file name, so Mixin failed silently until a runtime cast blew up. Fixed to `advanced_memory_card.refmap.json`.

### Notes (behavior changes)

- **Cards that were never configured apply nothing**: the paste no longer touches any option by default. Press `G` (or use the auto-opened panel) to opt in; the choice is stored on the card.
- **Shift + Right-click on air now switches modes**: paste with a **non-sneaking** right-click on air. Shift + Right-click on an AE device still copies its settings onto the card.
- **The copy screen is no longer a coordinate editor**: coordinates are still marked by two right-clicks in the world; the screen only edits options.

### Documentation

- The in-game guide now matches 1.21.1 exactly: paste-options panel, key bind table, and the corrected selection color legend (red start block, yellow end block, green targeted block, green box once ready to paste, red when over the volume limit).
- Both READMEs rewritten for the 1.20.1 / Forge environment (dependencies, key binds, config sections, build commands) and aligned with the 1.21.1 structure.
- Added this changelog plus build-verification and release workflows.

---

## 1.1.3 及更早 / 1.1.3 and earlier

本分支此前没有维护更新日志，见 GitHub Releases 自动生成的日志。 / This branch had no maintained changelog; see the auto-generated notes on GitHub Releases.
