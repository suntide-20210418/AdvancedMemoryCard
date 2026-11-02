# Advanced Memory Card 高级内存卡

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.7.10-blue)
![Forge Version](https://img.shields.io/badge/Forge-10.13.4.1614-orange)
![Mod Version](https://img.shields.io/badge/Version-1.2.0--1.7.10--GTNH-green)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

[简体中文](README.md) | **English**（见 [README_en.md](README_en.md)）

一个 Applied Energistics 2（AE2）扩展模组，为 AE2 内存卡提供**批量复制**与**P2P 通道高级配置**两大增强功能。

> **当前版本：`1.2.0-1.7.10-GTNH`** · Minecraft 1.7.10 / Forge 10.13.4.1614
> 更新内容见 [CHANGELOG.md](CHANGELOG.md)。

## 🌟 功能特性

### 🔧 两种工作模式

模组在物品 NBT 中以「模式」形式存储状态，可随时切换：

| 模式 | 说明 |
| --- | --- |
| **复制模式（Copy Mode）** | 框选一个区域，把复制到的 AE2 设备配置批量粘贴到区域内所有 AE2 方块与部件 |
| **配置模式（P2P 通道管理）** | 对着 P2P 隧道部件右键打开图形化界面，对网络内的 P2P 通道集中管理与配置 |

### 📋 复制模式（Copy Mode）

- **区域批量复制**：先把单个 AE2 设备的配置复制进卡片，再框选两个对角方块确定区域，对着空气右键即可把配置批量应用。
- **支持 AE2 方块与部件**：区域内所有 `AEBaseTile`（ME 控制器、接口、存储总线等）以及 `IPartHost` 上的部件都会被处理。
- **体积上限保护**：区域体积超过服务端配置 `maxCopyVolume`（默认 2048 个方块）时会被拒绝并提示，避免误选超大范围造成卡顿。
- **可视化选择框**：框选过程用不同颜色实时提示状态（红色=选第一点 / 白色=选第二点 / 绿色=待粘贴），目标方块、超范围方块也有独立配色。
- **实时状态提示**：聊天栏会显示已标记的坐标、区域大小与实际粘贴数量，物品 tooltip 则显示当前模式。
- **复制后自动弹出选项面板**：每次复制成功后，按卡片上的开关决定是否立即弹出面板，方便顺手调整这次要粘贴什么。

#### 粘贴选项（7 项，按卡保存）

| 选项 | 作用 |
| --- | --- |
| 设置 | 输出面、模糊模式等通用配置 |
| 优先级 | 存储总线 / 接口等的优先级 |
| 配置物品 | 过滤、标记等配置槽内容 |
| 样板 | 接口的样板槽 |
| P2P 类型 | P2P 隧道类型（勾选且与目标不同时会拆掉重建） |
| P2P 频率 | P2P 频段 |
| 名称 | 方块的自定义名 |

- **未勾选的项保持目标原值**，不是「清空」——这是与 AE2 原生内存卡最大的区别，也是本模组走自己的 `SettingsApplier` 逐段写入、不复用 `uploadSettings` 的原因：后者遇到缺键会直接把对应段清空。
- **未配置过的卡片默认全不选**：第一次使用请按 `G` 打开面板勾选；掩码为空时粘贴只提示、不消耗选区。
- **「样板」在 1.7.10 上不生效**：rv3 的 `downloadSettings` 只写 `display` / `configManager` / `priority` / `config` 四段，样板属于存档路径，不会进入内存卡；该选项仍保留在面板里，但勾选后粘贴不会改变目标。
- **「名称」对方块有效**：部件侧拿不到 `display` 段，因此部件名保持原样。

### 🕸 配置模式（P2P 通道管理）

对着任意 **P2P 隧道部件**右键即可打开配置界面（440×312 自绘面板），对当前 AE 网络内的所有 P2P 设备进行可视化管理：

- **四列 P2P 树**：状态点 / 名称 / 频段 / 汇总四列展示网络内的 P2P，按类型、频道分组，支持展开折叠与搜索过滤（搜索匹配完整频段 hex）。
- **配对条**：顶部「源 → 目标 + 频段下拉」，选定源设备、目标设备与频段后一次完成绑定。
- **检查器**：右侧展示选中设备的频段、角色、连接状态与频道别名，并提供改名、切为输入端、传送 / 高亮定位等按钮。
- **批量绑定**：Ctrl 点击非设备行进入批量模式，框选多台设备后一次性绑定到同一频段；状态行显示「批量成功」或「部分成功」。
- **64 位频段**：频段是 64 位整数，界面上以 **16 位十六进制**表示（如 `A3F019B7C2D40815`）。在配对条的频段下拉中选择目标频段；树中无别名时只显示 `…` + 末 6 位（如 `…A3F019`），完整值在检查器与 tooltip 中；`0000…0000`（未连接）不是可绑定目标，会被拒绝。
- **通道分组**：通道键为「频段 + P2P 类型」，同一频段下不同类型的 P2P 各自成组、互不干扰。
- **就地改名**：在检查器中直接编辑名称，`Enter` 提交、`Esc` 取消；也可为整个频道设置别名。
- **切换输入端**：把选中的 P2P 提升为该频段的输入端，原输入端自动降级为输出端。
- **分配新频道 / 自动配置输入输出**：为选中设备分配一个空闲频段并设为输入端；ME P2P 还可根据外部网络是否连接 ME 控制器自动判定角色。
- **高亮定位**：在世界中高亮渲染 P2P 通道（自身 / 输入端 / 输出端不同颜色），可选自动转头面向目标，并在聊天栏发送位置与传送链接；`server.allowTeleport` 关闭时不显示传送按钮，服务端也会拒绝。
- **状态行**：每次操作后在底部状态行给出反馈（绑定成功 / 无目标 / 频段非法 / 无网络 / 已切换为输入端 等）。
- **跨维度定位**：位置编码携带维度信息，可正确跨维度查找并高亮 P2P 设备。

### ⌨️ 按键一览

| 按键 | 作用 |
| --- | --- |
| `V` | 手持卡片时切换「复制模式 / 配置模式」 |
| `G` | 复制模式下打开粘贴选项面板 |
| `Shift + 右键` | 复制模式下：把目标设备配置复制进卡片（AE2 原生行为）；其它情况下：快速切换模式 |

> `V` 与 `G` 都可以在「选项 → 控制 → 按键绑定 → 高级内存卡」中修改。

## 📦 安装说明

### 前置要求

| 依赖 | 版本要求 |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614（GTNH Forge） |
| Applied Energistics 2 | GTNH 版 `Applied-Energistics-2-Unofficial` rv3（开发基于 `rv3-beta-1000-GTNH`） |

> 模组直接引用 `appeng.*` 的 P2P 与部件实现，AE2 是**必需**依赖，缺失时无法加载。NEI（NotEnoughItems）只是开发环境的运行时依赖，正式游玩不需要。

### 安装步骤

1. 从发布页下载 `advanced_memory_card-1.2.0-1.7.10-GTNH.jar`。
2. 放入 Minecraft 的 `mods` 文件夹。
3. 确保已安装上述前置模组，版本与整合包保持一致。
4. 启动游戏，在创造模式物品栏「高级内存卡」标签页中找到该物品。

## 🛠 配置说明

配置文件位于 `config/advanced_memory_card/`：

### 服务端配置（`server.cfg`，`[server]` 段）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `maxCopyVolume` | `2048` | 复制模式允许的最大区域体积（方块数），超出则拒绝粘贴 |
| `sendHighlightToChat` | `true` | 高亮 P2P 设备时是否在聊天栏发送位置信息与传送链接 |
| `allowTeleport` | `true` | 是否允许在配置界面中直接传送到 P2P 设备；关闭时不显示传送按钮，服务端也会拒绝 |

### 客户端配置（`client.cfg`）

用于界面与渲染外观的个性化，分组包括：

| 分组 | 内容 |
| --- | --- |
| `client.highlight` | 高亮时是否自动转头、高亮持续时间（1–60 秒） |
| `client.palette` | 配置模式新界面的语义色板：主 / 次 / 辅助文本、强调色、行选中与悬停、面板底色、分隔线、状态色、chip 底色、浮层颜色 |
| `client.treeColors` / `client.treeLayout` | P2P 树的背景 / 选中 / 悬停颜色，缩进、行高、图标间距、展开图标宽度、频道警告阈值、名称显示上限 |
| `client.panelColors` / `client.panelLayout` | 检查器的背景与文字颜色、状态色与布局尺寸 |
| `client.buttonColors` | 通用按钮的默认 / 悬停 / 按下 / 文字颜色 |
| `client.copyColors` | 复制模式选择框的起止点、目标、超范围与三种状态颜色 |
| `client.highlightColors` | P2P 高亮的自身 / 输入端 / 输出端颜色 |
| `client.itemColors` | 物品主色与着色色 |

> 配置模式的新界面以 `client.palette` 的语义色为准；`client.treeColors` / `client.panelColors` / `client.buttonColors` 等旧分组仍会写出，以兼容旧配置。
> 颜色为十六进制数值（GUI 类 ARGB，渲染类 RGB），可直接编辑配置文件；改完重启游戏生效。

## 📖 使用流程速览

### 复制模式

1. 手持卡片处于复制模式，对着源 AE2 设备 **Shift + 右键** 复制其配置。
2. （可选）按 `G` 或让面板自动弹出，勾选这次要应用的项。
3. 对第一个角落方块 **普通右键** 标记起点（红色）。
4. 对角方块 **普通右键** 标记终点（白色），进入待粘贴状态（绿色）。
5. 对着空气 **普通右键** 批量应用到区域内所有 AE2 设备。

### 配置模式

1. 切换到配置模式，对着任意 P2P 隧道部件 **普通右键** 打开界面。
2. 在左侧四列树中浏览 / 搜索网络内的 P2P，选中节点查看右侧检查器。
3. 在配对条选择源与目标（Ctrl 多选可批量绑定），或在检查器中改名、切输入端、传送 / 高亮定位。

## 🔧 开发构建（面向开发者）

```bash
# 客户端运行（开发环境）
./gradlew runClient

# 专用服务端运行
./gradlew runServer

# 构建发布 jar
./gradlew build

# 代码格式检查 / 自动修复
./gradlew spotlessCheck
./gradlew spotlessApply
```

- 构建基于 **GTNH Gradle 约定插件**：`build.gradle.kts` 只有一句 `plugins { id("com.gtnewhorizons.gtnhconvention") }`，插件版本在 `settings.gradle.kts`（`gtnhsettingsconvention` 2.0.20）中声明；Gradle wrapper 固定为 9.3.1。
- 需要 **JDK 25** 运行 Gradle；`enableModernJavaSyntax = jabel` 允许源码使用现代语法，最终仍编译为 Java 8 字节码。
- AE2 依赖为 GTNH 的 `com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1000-GTNH:dev`；NEI 只以 `runtimeOnlyNonPublishable` 供开发环境运行。
- 通过 `src/main/resources/META-INF/advanced_memory_card_at.cfg` 访问 AE2 的包级私有成员（例如 `PartP2PTunnel#setOutput`），反射入口集中在 `P2PManager`。
- 本版没有 Mixin 与 datagen：没有 `mixins.json`、没有 `runData`、没有 `src/generated/resources`；模型、贴图与语言文件直接维护在 `src/main/resources` 下，元数据用 `mcmod.info`。

## 📝 更新日志

见 [CHANGELOG.md](CHANGELOG.md)。

## 📄 许可

本项目基于 [GNU GPL-3.0](LICENSE.txt) 许可证开源。
