# Advanced Memory Card 高级内存卡

**简体中文** | [English](README_en.md)

一个 Applied Energistics 2（AE2）扩展模组，为 AE2 内存卡提供**区域批量粘贴**与**P2P 通道集中配置**两大能力。

## 🌟 功能特性

### 🔧 两种工作模式

模式与卡片级偏好都存在物品的 `CUSTOM_DATA` 组件里（`mode_data` 加上若干偏好键），可随时切换：

| 模式 | 说明 |
| --- | --- |
| **复制模式（Copy Mode）** | 先把一个 AE2 设备的配置复制进卡片，再框选区域，把配置批量粘贴到区域内所有 AE2 方块与部件 |
| **配置模式（P2P 通道管理）** | 对着 P2P 隧道部件右键打开图形界面，集中管理与配置当前 AE 网络内的 P2P 通道 |

### 📋 复制模式（Copy Mode）

- **区域批量粘贴**：Shift + 右键复制单个 AE2 设备的配置 → 右键标记两个对角方块 → 右键空气批量应用。
- **方块与部件都覆盖**：区域内所有 `AEBaseBlockEntity`（ME 控制器、接口、存储总线等）与 `IPartHost` 上的所有部件都会被处理。
- **体积上限保护**：区域体积超过服务端配置 `maxCopyVolume`（默认 2048 个方块）时会被拒绝并重置选区，避免误选超大范围卡顿。
- **可视化选区**：起始点、结束点、准星指向的方块、超范围的方块各自使用独立配色。
- **实时状态提示**：物品 tooltip 与聊天栏显示已标记坐标、区域大小、粘贴完成的方块数量。

#### 粘贴选项（8 项，按卡保存）

粘贴时具体应用哪些内容由 8 个开关控制：

| 选项 | 对应 AE2 组件 | 说明 |
| --- | --- | --- |
| 升级 | `EXPORTED_UPGRADES` | 升级卡 |
| 设置 | `EXPORTED_SETTINGS` | `IConfigurableObject` 的通用配置 |
| 优先级 | `EXPORTED_PRIORITY` | 优先级 |
| 配置物品 | `EXPORTED_CONFIG_INV` | 配置槽内容（过滤/标记物品等） |
| 样板 | `EXPORTED_PATTERNS` | 样板供应器的样板 |
| P2P 类型 | `EXPORTED_P2P_TYPE` | P2P 隧道类型（决定部件是否换型） |
| P2P 频率 | `EXPORTED_P2P_FREQUENCY` | P2P 频段 |
| 名称 | `EXPORTED_CUSTOM_NAME` | 自定义名称 |

- **打开方式**：手持卡且处于复制模式时按 `G`（默认键位，可在控制设置中修改）；默认在**每次复制时**（Shift + 右键把设备配置复制进卡片）自动弹出。
- **自动弹出开关**：面板左侧（挂在面板外）的齿轮按钮 —— `COG` = 每次复制后弹出，`COG_DISABLED` = 不弹出，状态同样按卡保存。
- **首次默认全不选**：从未配置过的卡片默认不应用任何项，需要显式勾选；勾选结果写在卡片上，之后每次弹出都沿用。
- **「不应用」= 目标保持原值**：勾掉某项后，该项不会被写入目标。其中样板 / P2P 类型 / 名称三项在 AE2 的实现里「数据缺失 = 清空目标」，所以粘贴时会把目标自身的当前值回填，效果等同于保持原样。
- **与模式切换无关**：选项存在卡片根标签（`mode_data` 之外），切换模式、重新复制都不会被重置。

### 🕸 配置模式（P2P 通道管理）

对着任意 **P2P 隧道部件**右键即可打开配置界面，对当前 AE 网络内的所有 P2P 设备进行可视化管理：

- **P2P 树形列表**：自动扫描所在网络，按类型 / 频道分组展示所有 P2P 设备，支持展开折叠与搜索过滤。
- **详情面板**：选中节点后展示该 P2P 的类型、频段、输入/输出状态、连接状态等信息。
- **重命名 / 别名**：为单个 P2P 设备设置自定义名称，也可为整个频道（频段输入端）设置别名，便于识别。
- **绑定频段**：将选中的 P2P 绑定到指定 4 位十六进制频段（如 `0A1B`）。所有类型的 P2P 都会自动判断输入/输出端角色：目标频段已有其他输入端时当前设备作为输出端，否则作为输入端。
- **分配新频道**：为当前选中的非 ME P2P 一键分配一个全新的空闲频段并设为输入端。
- **自动配置输入/输出（仅 ME P2P）**：根据外部网络是否连接「ME 控制器」自动判定该 ME P2P 应作为输入端还是输出端，并自动分配频段。
- **高亮定位**：在世界上高亮渲染 P2P 通道（自身 / 输入端 / 输出端使用不同颜色），可选自动转头面向目标，并在聊天栏发送位置与传送链接。
- **跨维度连接**：编码格式携带维度信息，可正确跨维度查找并高亮 P2P 设备。

### ⌨️ 按键一览

| 按键 | 生效场景 | 作用 |
| --- | --- | --- |
| `V` | 手持卡 | 在复制模式 / 配置模式之间切换 |
| `G` | 手持卡且处于复制模式 | 打开粘贴选项面板 |
| Shift + 右键（空气） | 手持卡 | 快速切换模式 |
| Shift + 右键（AE 方块 / 部件） | 复制模式 | 把该设备的配置复制进卡片（原生 AE2 行为），并按设置自动弹出选项面板 |
| 右键（空气） | 复制模式 | 批量粘贴；尚未完成选区时给出提示 |
| 右键（P2P 部件） | 配置模式 | 打开 P2P 配置界面 |

> `V` 与 `G` 均可在「选项 → 控制 → 按键绑定 → 高级内存卡」中修改。

## 📦 安装说明

### 前置要求

| 依赖 | 版本要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.219` 或更高（`[21.1.219,)`） |
| Applied Energistics 2 (AE2) | 与 MC 1.21.1 匹配的版本（开发基于 `19.2.17`；`neoforge.mods.toml` 中声明为 `*`） |

> 除以上三项外没有声明其他运行时依赖。Jade、GuideME、JEI 只在开发环境引入以便调试，发行包不依赖它们。

### 安装步骤

1. 从发布页下载最新版本的 `advanced_memory_card-<版本>.jar`。
2. 将 `.jar` 文件放入 Minecraft 的 `mods` 文件夹。
3. 确保已安装上述前置模组（NeoForge 与 AE2）。
4. 启动游戏，在创造模式物品栏「高级内存卡」分组（`advanced_memory_card:advanced_memory_card_tab`）中找到物品 `advanced_memory_card:advanced_memory_card`。

## 🛠 配置说明

模组提供客户端与服务端两套配置文件（位于 `config/` 目录）：

### 服务端配置（`advanced_memory_card-server.toml`，`[server]` 段）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | 复制模式允许的最大区域体积（方块数，范围 1 – `Integer.MAX_VALUE`），超出则拒绝粘贴 |
| `sendHighlightToChat` | `true` | 高亮 P2P 设备时是否在聊天栏发送位置信息与传送链接 |

### 客户端配置（`advanced_memory_card-client.toml`）

主要用于界面与渲染外观的个性化，按片段组织：

- `[client.highlight]`：`rotateHeadOnHighlight`（高亮时自动转头）、`p2pHighlightDurationSec`（高亮持续时间，1–60 秒）。
- `[client.treeColors]`：`bg` / `selected` / `hover`。
- `[client.treeLayout]`：`indentWidth`、`rowHeight`、`iconTextSpacing`、`expandIconWidth`、`channelWarningThreshold`、`p2pNameMaxLength`。
- `[client.panelColors]`：`bg` / `title` / `label` / `value` / `placeholder` / `separator` / `buttonAreaBg`，以及 `[client.panelColors.status]` 下的 `notActive` / `notConnected` / `connected`。
- `[client.panelLayout]`：`buttonWidth`、`buttonHeight`、`buttonSpacing`、`lineHeight`、`paddingLeft`、`paddingTop`、`buttonCountMax` 等布局参数。
- `[client.buttonColors]`：`bg` / `hover` / `click` / `text`（通用 FlatButton 按钮）。
- `[client.copyColors]`：`startPos` / `endPos` / `targetedPos` / `overLimit`，以及 `[client.copyColors.selection]` 下的 `ready` / `first` / `second`。
- `[client.highlightColors]`：`self` / `input` / `output`（P2P 高亮渲染）。
- `[client.itemColors]`：`color`（物品主色）与 `tintColor`（着色色）。

> 颜色为十六进制数值（GUI 类为 ARGB，世界渲染类为 RGB），可直接在配置文件中修改。

## 📖 使用流程速览

### 复制模式

1. 手持高级内存卡处于复制模式，对着源 AE2 设备 **Shift + 右键** 复制其配置（此时会按设置弹出粘贴选项面板，按需勾选）。
2. 对着第一个角落方块 **普通右键** 标记起点（红色）。
3. 对着对角方块 **普通右键** 标记终点（白色），此时进入待粘贴状态（绿色）。
4. 对着空气 **普通右键**，把配置批量应用到区域内所有 AE2 设备。

> 面板可以随时关闭，选项会保留在卡片上；不想每次都弹出就点左侧的齿轮按钮切换成 `COG_DISABLED`。

### 配置模式

1. 切换至配置模式，对着任意 P2P 隧道部件 **普通右键** 打开配置界面。
2. 在左侧树中浏览 / 搜索网络内的 P2P，选中节点查看右侧详情。
3. 通过详情面板按钮进行重命名、绑定频段、分配新频道、自动配置输入/输出、高亮定位等操作。

## 🔧 开发构建（面向开发者）

```bash
# 客户端运行（开发环境）
./gradlew runClient

# 专用服务端运行
./gradlew runServer

# 游戏测试服务端
./gradlew gameTestServer

# 构建发布 jar
./gradlew build

# 生成配方 / 物品模型 / 语言文件
./gradlew runData
```

- 基于 **NeoForge 21.1.219** 与 **ModDevGradle 2.0.140**，Java 21 工具链，Parchment 映射 `2024.11.17`。
- 使用 Mixin（`advanced_memory_card.mixins.json`，`compatibilityLevel: JAVA_21`）做两处扩展：
  - `P2PTunnelPartMixin`：公开 AE2 的 `P2PTunnelPart#setOutput`；
  - `AEBasePartAccessor`：用 `@Accessor` 直写 `AEBasePart#customName` 实现 P2P 改名（绕开 `importSettings` 的副作用）。
- 资源由 `src/main/java/.../datagen` 下的生成器产出到 `src/generated/resources/`：`ModRecipesProvider`、`ModItemModelsProvider`、`ModEnusLangProvider`、`ModZhcnLangProvider`。

## 📄 许可

本项目基于 [GNU GPL-3.0](LICENSE.txt) 许可证开源。
