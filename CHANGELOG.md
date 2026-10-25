# 更新日志 / Changelog

面向玩家与整合包作者的版本日志。内部重构、CI 调整等不影响游戏行为的改动不在此列出。

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
