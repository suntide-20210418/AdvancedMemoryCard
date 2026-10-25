# Advanced Memory Card 跨版本同步移植计划

> 基准版本：1.21.1（`suntide/1.21.1`）
> 目标版本：1.20.1（`suntide/1.20.1`）、1.12.2（`1.12.2`）、1.7.10-GTNH（`suntide/1.7.10-GTNH`）
> 仓库：`git@github.com:suntide-20210418/AdvancedMemoryCard.git`（四个版本为同一仓库的不同分支，各自独立工作副本）

---

## 0. 结论摘要

1. 四版本的差距**不在 GUI 布局**，而在两个语义层：
   - **复制模式的选择性粘贴**（`CopyOption` 8 项位掩码 + 「清空型」选项的目标值回填）只有 1.21.1 有；
   - **P2P 频段角色仲裁**（`freq==0` 拒绝绑定、ME 纳入仲裁、服务映射补登、坏通道修复）只有 1.21.1 完整。
2. 同步顺序为 **1.20.1 → 1.12.2 → 1.7.10**。理由：1.20.1 与基线同属 AE2 15 系，`P2PService` / `AEBaseMenu` / `GuiSync` / `StyleManager` 全部同名，是「零语义损耗」的验证面；1.12.2 是数据载体的第一次降级（DataComponent → NBT，`IPart#importSettings` 消失）；1.7.10 再叠加上 proxy、反射、自建 `BlockPos`、`ForgeDirection`，最脏，放最后。
3. 版本能力裁剪（已确认）：**1.12.2 / 1.7.10 的 AE2 没有部件级 settings 导入 API**，P2P 部件粘贴只走 `PartP2PTunnel.applyMemoryCard`（仅 `freq` + 物品栈 NBT），因此 `P2P_TYPE` / `P2P_FREQUENCY` 两项在这两个版本隐藏，其余 6 项保留，掩码位定义跨版本保持不变。
4. 不引入跨版本共享源码。四个分支各自维护，靠本文档的基线约定 + 逐版本 diff 同步。

---

## 1. 版本基线与前置修正

| 版本 | 工作副本 | 分支 | 加载器 / 工具链 | AE2 | 规模 |
| --- | --- | --- | --- | --- | --- |
| 1.21.1（基准） | `AdvancedMemoryCard-1.21.1` | `suntide/1.21.1` | **NeoForge 21.1.219** / Java 21 / moddev 2.0.140 | AE2 1.21.1（curse 7027323） | 47 文件 / 7939 行 |
| 1.20.1 | `AdvancedMemoryCard-1.20.1` | 当前在 `master` | Forge 47.4.10 / Java 17 / ForgeGradle 6 | 15.4.10 | 43 文件 / 7066 行 |
| 1.12.2 | `AdvancedMemoryCard-1.12.2` | `1.12.2` | Forge 14.23.5.2860 / Java 8 / Gradle 4.9 | Unofficial Extended Life（rv6 系） | 44 文件 / 5737 行 |
| 1.7.10 | `AdvancedMemoryCard-1.7.10` | `suntide/1.7.10-GTNH` | GTNHGradle / Forge 10.13.4.1614 / Java 25 + jabel | GTNH rv3 fork | 45 文件 / 6780 行 |

### 1.1 开工前必须处理

`AdvancedMemoryCard-1.20.1` 的工作副本停在 `master`（`fd32508`），比 `origin/suntide/1.20.1` **落后 2 个提交、领先 1 个提交**。差异集中在：

- `p2p/P2PManager.java`（18 行）
- `item/custom/AdvancedMemoryCardItem.java`（2 行）
- `gradle.properties`

`origin/suntide/1.20.1` 上有一条 `fix(p2p): 修复输入端被误重置为输出端的问题`。**任何 1.20.1 的移植工作都必须先切到该分支**，否则会基于旧 bug 版本继续开发并反向覆盖已修复的逻辑。

### 1.2 各版本已有构建隐患（同阶段顺手修）

| 版本 | 隐患 | 后果 |
| --- | --- | --- |
| 1.20.1 | `build.gradle:36` 引用 `src/main/resources/META-INF/accesstransformer.cfg`，该文件不存在 | 构建失败，或 AT 静默缺失 |
| 1.20.1 | `advanced_memory_card.mixins.json` 写 `refmap: advancedmemorycard.refmap.json`，实际产物是 `advanced_memory_card.refmap.json` | mixin 不注入 → `(P2PTunnelPartMixin) part` 抛 `ClassCastException` |
| 1.12.2 | `AdvancedMemoryCardMod.VERSION` 硬编码 `1.0.0`（jar 名 1.0.1）；`mcmod.info` 未声明 AE2 依赖 | 版本显示错误、加载顺序无保证 |
| 1.7.10 | `TranslateHelper` 使用双段前缀 `gui.advanced_memory_card.advanced_memory_card.…`；创造标签 lang 键 `itemGroup.advanced_memory_card` 与 `.tab` 不匹配 | 界面显示原始键名 |

---

## 2. 各分支功能与实现差异

### 2.1 功能矩阵

| 能力 | 1.21.1（基准） | 1.20.1 | 1.12.2 | 1.7.10 |
| --- | --- | --- | --- | --- |
| V 键切模式 | 有（判 `GLFW_PRESS`，防 REPEAT 连切） | 有（无 PRESS 判断） | 有 | 有 |
| G 键开复制选项界面 | 有（`OpenGuiPacket` 有发送点） | **无发送点**（界面不可达） | **无**（G 键被注释） | **无** |
| 区域复制粘贴（右键两次定角） | 有 | 有 | 有 | 有 |
| **选择性粘贴（8 项掩码）** | 有（DataComponent） | **无**（全量导入 `Data`） | **无** | **无** |
| 复制完成后自动弹选项 | 有（卡片级开关） | 无 | 无 | 无 |
| 配置界面（树 / 搜索 / 详情面板） | `AEBaseScreen` + JSON 槽位布局 | 同（JSON 布局一致） | `GuiContainer` 自绘 + `GL11.glScissor` | `GuiContainer` 自绘 |
| 频段别名 / P2P 改名 | 字段访问器 `AEBasePartAccessor` + `markForSave`+`markForUpdate` | `CustomNameUtil` + `importSettings`（有升级/优先级副作用） | `p2pPart.setCustomName`（无落盘标记） | 反射 `setCustomNameInternal` |
| P2P 角色仲裁 | 所有类型统一（含 ME） | 跳过 ME | **仅非 ME** | **仅非 ME** |
| `freq==0` 拒绝绑定 | 有 | 有 | **无**（会把「未连接 0000」当目标静默解绑） | **无** |
| 服务映射补登 / 坏通道修复 | `synchronizeServiceMappings` + `repairChannelInputs` | 无 | 无 | 无 |
| 高亮渲染路径 | 客户端本地（`P2PRenderHelper`），服务端只发聊天坐标 | 服务端 `P2PManager.renderP2P`（`DistExecutor` 内包客户端调用） | `HighlightPacket` S→C + `ClientHighlightHandler` | 同 1.12.2 |
| 聊天传送链接 | `/execute in <dim> run tp @p …` | 同 | `/amc_tp` 命令（权限 2） | 同 |
| 游戏内指南 | `assets/advanced_memory_card/ae2guide/*.md` | 有 | 有 | **不适用**（rv3 无 GuideME） |
| 资源生成 | datagen 4 provider | datagen 4 provider | 手写 `.lang` / `.json` | 手写 `.lang` / `.json` |

### 2.2 代码级差异（1.21.1 vs 1.20.1，逐文件 diff 行数）

`CopyMode` 632 · `CopyModeScreen` 343 · `P2PManager` 323 · `CardMode` 133 · `ClientConfig` 96 · `AdvancedMemoryCardItem` 87 · `NetworkHandler` 73 · `OpenGuiPacket` 71 · `KeyEvent` 57 · `CopyModeRenderer` 44 · `ConfigModeSyncPacket` 42 · `AdvancedMemoryCardMod` 38。

基本一致（无需重写）：`P2PTreeWidget`(3)、`DetailPanelWidget`(7)、`FlatButton`(0)、`P2PInfo` / `ChannelInfo` / `P2PTypeInfo` / `P2PPosition`(0)、`AreaHelper`(0)、`ResourceLocationHelper`(0)。

结构性差异：

- **1.20.1 缺失的类**：`CopyOption`、新版 `CopyModeMenu`（掩码语义）、`P2PRenderHelper`、`AEBasePartAccessor`、`AdvancedMemoryCardClient`（其客户端入口叫 `AdvancedMemoryCardModClient`，用 `@EventBusSubscriber(Dist.CLIENT, Bus.MOD)`）。
- **包结构不同**：1.20.1 的 `ModMenu`、`ConfigModeMenu`、`CopyModeMenu` 放在 `client/gui/` 下；1.21.1 统一到 `menu/`。`ConfigModeMenu` 的 10 个 action 已同名同签名，配置侧几乎零移植。
- **复制界面用途完全不同**：1.20.1 是六个坐标输入框（正则解析 `BlockPos{x=..}`）+ 清空按钮，用于手调选区；1.21.1 是 8 个 `AECheckbox`，用于控制粘贴应用哪些内容。
- **1.12.2 / 1.7.10 已有的能力**（不要重复移植）：`assignNewFrequency`、`getCurrentGrid`、`fixZeroFrequencyAnomalies`、非 ME 类型仲裁。
- **1.12.2 / 1.7.10 的载体差异**：`node.getMachine()` 而非 `getOwner()`；频率为 `short`（`%04X`）/ `long`（`%016X`）；`P2PCache` 以部件实例为键并按类严格比较；`usedChannels()` 走反射、`maxChannel` 硬编码 32。

---

## 3. 移植分层：语义基线 vs 版本外壳

**语义基线（四个分支必须逐字一致）**

1. `CopyOption` 8 项位序（bit0..bit7）与 `preserveTargetValue` 语义。
2. 卡片级键名 `apply_options_mask` / `auto_open_options`，存放在卡片**根标签**而非 `mode_data` 内 —— 否则每次切模式写 `mode_data` 都会把偏好重置。
3. P2P 仲裁规则：目标频段已有**其他设备的**输入端 → 本机为输出端；否则本机为输入端。
4. `freq==0` 一律拒绝绑定（`0000` 表示未连接，不是可绑定目标）。
5. 频段格式：`%04X`（1.21.1 / 1.20.1 / 1.12.2），`%016X`（1.7.10）。
6. UI 中各项的排列顺序与翻译键命名。

**版本外壳（各版本独立实现，不强行统一）**

网络栈（`CustomPacketPayload` / `SimpleChannel` / `SimpleNetworkWrapper`）、GUI 基类（`AEBaseScreen` / `GuiContainer`）、数据载体（DataComponent / NBT）、渲染事件（`RenderLevelStageEvent` / `RenderWorldLastEvent`）、注册入口（`@Mod` 构造注入 / `FMLPreInitializationEvent` / `@SidedProxy`）。

---

## 4. 阶段计划

### P0 基线固化（1.21.1，只产出文档）

- **目标**：把上述 6 条基线写成版本内文档，作为下游对照依据，并打 tag。
- **动作**：核对 8 项选项与 AE2 实际导入行为（`CopyOption.java` 的注释已含证据链）；记录三种仲裁场景的可复现步骤。
- **退出条件**：文档落地、`git tag baseline-1.21.1`、`runClient` 两个界面可正常打开。
- **约束**：不修改 1.21.1 业务代码。

### P1 1.20.1 全量对齐（主战场）

**进入条件**：切到 `suntide/1.20.1`；修掉 §1.2 的 AT 与 refmap 两处；`gradlew build` 通过。

**新增**

| 文件 | 要点 |
| --- | --- |
| `item/custom/CopyOption.java` | 8 项枚举；字段由 `DataComponentType<?>` 换成 `String nbtKey`；位序与 `preserveTargetValue` 照搬 |
| `client/P2PRenderHelper.java` | 客户端据 `P2PInfo` → `IPartHost.getPart(Direction)` → `triggerRender` |
| `mixin/AEBasePartAccessor.java` | 先确认 15.4.10 的 `AEBasePart.customName` 字段仍存在；否则退回 `CustomNameUtil`，不新增 mixin |

**改写**

| 文件 | 改动要点 |
| --- | --- |
| `item/custom/CopyMode.java` | 加 `APPLY_OPTIONS_MASK` / `AUTO_OPEN_OPTIONS` 静态读写；`createFilteredCard`（`tag.copy()` 后按位 `remove(key)`）；`withTargetValues`（清空型键用目标自身导出值回填）；`onItemUseFirst` 的 shift 分支改为「先弹界面再 `PASS`」 |
| `client/gui/menu/CopyModeMenu.java` | `@GuiSync` 由 `startPos`/`endPos`/`mode` 改为 `applyMask`/`handOrdinal`/`autoOpen`；action 改 `SET_APPLY_MASK`/`SET_AUTO_OPEN`；建议顺带迁到 `menu/` 包以对齐 1.21.1（需同步 `ModMenu`、`AdvancedMemoryCardMod` 引用） |
| `client/gui/screen/CopyModeScreen.java` | 删六个坐标框、正则解析、清空按钮；改 8 个 `AECheckbox` + 标题 + COG 自动弹出开关 |
| `p2p/P2PManager.java` | 加 `synchronizeServiceMappings()`、`repairChannelInputs()`；`bind()` 去 ME 例外、加 `freq==0` 早退、末尾 `getHost().markForSave()`；删 `renderP2P(...)` 两个重载 |
| `client/key/KeyEvent.java` + `ModKey.java` | 加 G 键（`COPY_OPTIONS_KEY`）+ `GLFW_PRESS` 早退；`OpenGuiPacket` 接上发送点 |
| `client/gui/screen/ConfigModeScreen.java` + `client/renderer/P2PRenderer.java` | 高亮改本地渲染；服务端 `highlightP2P` / `highlightP2PTunnel` 只保留聊天坐标与传送链接 |
| `AdvancedMemoryCardModClient.java` | `RegisterMenuScreensEvent` 注册第二个 Screen |
| `assets/ae2/screens/copy_mode_menu.json` + `assets/ae2/textures/guis/copy_mode_menu.png` | `srcRect` 由坐标版换成 176×181。**1.20.1 的 json 没有 `textureWidth/Height`，不要照抄 1.21.1 的 256 声明** |
| datagen 的 en_us / zh_cn provider | 8 项选项、标题、G 键 |

**删除**：坐标解析 helper、`P2PManager.renderP2P`、未被引用的 `advanced_memory_card_base_gui_spritesheet.png`。

**退出条件**：8 项逐项粘贴验证通过；三角色场景通过；无 highlight 网络包残留；专服可启动。

### P2 1.12.2 API 内对齐

**新增**：`item/custom/CopyOption.java`（6 项，隐藏 `P2P_TYPE` / `P2P_FREQUENCY`）；`client/renderer/P2PRenderHelper.java`。

**改写**

- `item/custom/CopyMode.java`：掩码读写在卡片根标签；粘贴前对 `NBTTagCompound` 副本做裁剪，再走 `AEBaseTile.uploadSettings(...)`；清空型键先回填。
- `client/gui/menu/CopyModeMenu.java`：`Container` 版改为掩码同步（`@GuiSync int applyMask`）。
- `client/gui/screen/CopyModeScreen.java`：删坐标框，用 `Gui.drawRect` 复刻 1.21.1 的 176×181 分区与配色，画 6 项。
- `p2p/P2PManager.java`：补 `freq==0` 早退、ME 纳入仲裁、`synchronizeServiceMappings`、`repairChannelInputs`、`markDirty` + 方块更新。
- `client/gui/menu/ConfigModeMenu.java`：高亮改本地渲染（`HighlightPacket` 保留但不再作主路径）。
- `network/OpenCopyGuiPacket.java`、`client/key/*`：接 G 键。
- `command/TeleportCommand.java`：保留 `/amc_tp`（老版本 `ClickEvent.RunCommand` 体验不可靠时作为兜底）。
- `utils/TranslateHelper.java`：补新键。

**删除**：坐标输入相关字段与方法；`HighlightPacket` 的发送点（接收端保留一个版本周期）。

**退出条件**：6 项逐项验证通过；P2P 部件粘贴行为与 guide 描述一致；存档重进后角色与频段不漂移。

### P3 1.7.10 GTNH 收尾

**新增**：`item/custom/CopyOption.java`（6 项）；`client/renderer/P2PRenderHelper.java`。

**改写**

- `item/custom/CopyMode.java`：同 P2，但频率为 `long`；部件路径只能控制「贴不贴」，不能选择类型/频率。
- `menu/CopyModeMenu.java` / `menu/ConfigModeMenu.java`：`Container` 掩码同步；构造期即发包的时序问题按 §7 处理。
- `p2p/P2PManager.java`：仲裁 + `%016X` + `markDirty`；`setOutput` / `setCustomNameInternal` 改为「AT 优先 → 反射兜底 → 双失败降级并打日志」。
- `client/gui/screen/CopyModeScreen.java`：`GL11` 自绘 6 项。
- `client/key/*`、`GuiHandler.java`、`CommonProxy` / `ClientProxy`：第二 GUI ID、G 键、渲染器注册。
- `utils/TranslateHelper.java`（双段前缀统一）、`item/ModCreativeModeTabs.java`（创造标签键修正）。

**删除**：坐标展示路径（`utils/BlockPos.toShortString` 仅保留内部用途）。

**退出条件**：专服 + 客户端均可启动；AT/反射任一失效时功能降级但**不崩**；所有 lang 键命中。

---

## 5. API 与兼容性差异对照

| 1.21.1 | 1.20.1 | 1.12.2 | 1.7.10 |
| --- | --- | --- | --- |
| `DataComponents.CUSTOM_DATA` + `mode_data` | NBT 根 `mode_data`（同） | `stack.getTagCompound().getCompoundTag("mode_data")` | 同 1.12.2 |
| `AEComponents.EXPORTED_UPGRADES` | 键 `upgrades` | `upgrades` | `upgrades` |
| `EXPORTED_SETTINGS` | config-manager 段（`getConfigManager().writeToNBT`） | 键 `config` | `config` |
| `EXPORTED_PRIORITY` / `EXPORTED_CONFIG_INV` | `priority` / `config` | `priority` / `config` | 同 |
| `EXPORTED_PATTERNS` | `PatternProviderLogic.NBT_MEMORY_CARD_PATTERNS`（`patterns`） | `DualityInterface` 的 `patterns` 子标签 | 同 |
| `EXPORTED_P2P_TYPE` / `EXPORTED_P2P_FREQUENCY` | `p2pType` / `p2pFreq` | **降级**：只走 `applyMemoryCard`，不可选 | 同 1.12.2 |
| `EXPORTED_CUSTOM_NAME` | `display.Name`（`CustomNameUtil`，缺省即清空） | `display.Name` | 同 |
| `AEBaseBlockEntity.importSettings` / `IPart#importSettings` | 两者都有 | 仅 tile：`AEBaseTile.uploadSettings`；部件无 | 同 1.12.2 |
| `IPartItem.getId(...)` | 同 | 部件类判等（`instanceof`） | 类名字符串判等 |
| `P2PService.get/getInput/updateFreq/newFrequency` | 同 | `getProxy().getP2P()` + `P2PCache` | 同 |
| `getHost().markForSave()` / `markForUpdate()` | `markForSave` | `markDirty` + `world.markBlockForUpdate` | 同 |
| `P2PTunnelPartMixin.@Invoker setOutput` | 同（`remap=false`） | 反射（AT 也暴露 `setOutput`） | AT + 反射双路径 |
| `AEBasePartAccessor.@Accessor customName` | 新增，或退回 `CustomNameUtil` | `setCustomName` | 反射 `setCustomNameInternal` |
| `RegisterPayloadHandlersEvent` + `StreamCodec` | `SimpleChannel` + `IMessage` | `SimpleNetworkWrapper`（6 包位次固定） | 同 1.12.2 |
| `AEBaseMenu` + `registerClientAction` | 同 | `Container` + `GuiHandler` | `Container` + `GuiHandler` |
| `RenderLevelStageEvent(AFTER_TRIPWIRE_BLOCKS)` | 同 | `RenderWorldLastEvent` | 同 |
| `writeResourceKey(DIMENSION)` | 同 | `dimension` 字符串（`DimensionType.getName`） | `dimensionId` int，`DimensionHelper` 仅 0/-1/1 |

---

## 6. CopyOption 在三套 NBT 体系上的实现

### 6.1 表达「未勾选」的两条路径

- **跳过型**（`UPGRADES` / `SETTINGS` / `PRIORITY` / `CONFIG_INV` / `P2P_FREQUENCY`）：从交给导入方的 NBT 副本里 `remove(key)` 即可，AE2 取不到就跳过。
- **清空型**（`PATTERNS` / `P2P_TYPE` / `CUSTOM_NAME`）：缺失会被写成空值（样板被清、名称置 null、P2P 类型分支整段不执行），必须先用**目标自身导出值**回填该键，语义才等于「不复制」。

### 6.2 默认值与界面时序

1.12.2 / 1.7.10 的 `Container` 在 Screen 之前构造，首帧复选框全空。掩码缺省必须写 `0xFF`（全选），并且 `mask == 0` 时按全选解释 —— 否则会出现「打开界面 → 确认 → 什么都没粘贴」。

### 6.3 低版本降级

1.12.2 / 1.7.10 隐藏 `P2P_TYPE` / `P2P_FREQUENCY`（位 5/6 的定义保留、读取时忽略），`ae2guide` 与 README 明写「P2P 部件粘贴始终带类型与频率」；方块侧保留 6 项。

---

## 7. 风险清单

| 级别 | 风险 | 触发条件 | 止损手段 |
| --- | --- | --- | --- |
| 坏存档 | 角色误判被 `markDirty` 落盘 | 1.12.2 / 1.7.10 的 `P2PCache` 以实例为键；ME 由「跳过」改为「仲裁」后，老存档里已存在的双输入端通道 | 先只读预演再写；`freq==0` 早退；提供「修复通道」手动入口 |
| 坏存档 | 静默解绑 | 漏移植 `freq==0` 拒绝 → GUI 的「未连接(0000)」被当成可绑定目标 | 用例断言：`bind("0000")` 不改变 `getFrequency()` |
| 崩服务端 | 反射失败 | 1.7.10 GTNH 混淆名变化，静态初始化抛 `NoSuchMethodException` | `try/catch` 降级为「只改频段不改角色」；AT/反射双路径；启动日志说明走了哪条 |
| 崩服务端 | 客户端类混入服务端 | 1.20.1 的 `AdvancedMemoryCardModClient`；1.12.2 的 `ClientHighlightHandler` | 专服 `runServer` 必测；渲染调用一律下沉到客户端 Screen |
| 静默失效 | mixin 不注入 | 1.20.1 refmap 名不一致 + AT 文件缺失 | 两处同修；`instanceof` 守卫 + warn；CI grep 产物 refmap 名 |
| 静默失效 | 掩码写成 0 | `Container` 同步早于 Screen（见 §6.2） | 缺省 `0xFF`；UI 禁止全不选 |
| 静默失效 | 部件「勾了没反应」 | 低版本 `IPart` 无 settings 导入入口 | 隐藏两项；右键给气泡提示 |
| 视觉回归 | GUI 图集错位 | 1.20.1 换贴图时 `srcRect` / `textureWidth` 与 AE2 15.4 不符 | 先只改 `widgets` 不动 `background`；保留旧 json/png；一阶段一提交 |
| 视觉回归 | 全界面显示原始键名 | 1.7.10 双段前缀、创造标签键不匹配 | 启动遍历 `LanguageMap` 断言键存在 |

---

## 8. 验证与回归测试要点

每个阶段都必须跑完这一组：

1. **编译**：`gradlew build` 无新增 warning；1.20.1 额外确认 `build/tmp/compileJava/*.refmap.json` 名称与 `mixins.json` 一致。
2. **单机 `runClient`**：两个界面均可打开；G 键开复制选项；V 键长按不重复触发。
3. **专服 `runServer`**：无客户端类加载；1.7.10 检查 proxy 分流是否正确。
4. **跨维度**：主世界 ↔ 下界的 P2P 绑定；1.12.2 / 1.7.10 仅支持 0/-1/1，末地需给降级提示。
5. **仲裁三场景**：
   - 空频段绑定 → 本机成为输入端；
   - 已有输入端再绑 → 本机成为输出端；
   - 同频双输入端（异常存档）→ `repairChannelInputs` 降级多余输入端，界面恢复「已连接」。
6. **掩码逐项**：8 项（低版本 6 项）各做一次「只勾它」的粘贴，前后取目标 NBT 快照比对；重点验证 `PATTERNS` / `CUSTOM_NAME` 未勾时目标值不变、`P2P_TYPE` 未勾时部件仍能粘贴其余项。
7. **存档重进**：绑定后重启，角色与频段不漂移；掩码持久化；复制选项开关持久化。
8. **文档一致性**：`ae2guide` / README / CHANGELOG 与当版实际能力逐条对齐；1.7.10 无 GuideME，改 README + CHANGELOG 说明。

---

## 9. 提交与交付约定

- 每个阶段在各自分支独立提交并推送；只提交代码、资源、构建与文档文件，不提交 `.workbuddy/`、`.idea/`、`build/`、`run/`。
- 提交粒度按主题拆分，例如 `feat(copy): 引入 CopyOption 掩码粘贴`、`fix(p2p): 补全角色仲裁与坏通道修复`；构建修复单独一条。
- 每个版本的 `CHANGELOG.md` 记录「与 1.21.1 对齐到什么程度、哪些项降级」。
- 阶段顺序不可并行：P1 产出的 NBT 键表是 P2 / P3 的输入，P2 的降级决策是 P3 的输入。
